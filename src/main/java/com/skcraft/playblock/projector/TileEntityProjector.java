package com.skcraft.playblock.projector;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.List;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.SimpleComponent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import org.apache.logging.log4j.Level;

import com.sk89q.forge.BehaviorList;
import com.sk89q.forge.BehaviorListener;
import com.sk89q.forge.BehaviorPayload;
import com.sk89q.forge.PayloadReceiver;
import com.sk89q.forge.TileEntityPayload;
import com.skcraft.playblock.PacketHandler;
import com.skcraft.playblock.PlayBlock;
import com.skcraft.playblock.SharedRuntime;
import com.skcraft.playblock.network.PlayBlockPayload;
import com.skcraft.playblock.player.MediaPlayer;
import com.skcraft.playblock.player.MediaPlayerClient;
import com.skcraft.playblock.player.MediaPlayerHost;
import com.skcraft.playblock.queue.ExposedQueue;
import com.skcraft.playblock.queue.QueueBehavior;
import com.skcraft.playblock.util.AccessList;
import com.skcraft.playblock.util.DoubleThresholdRange;
import com.skcraft.playblock.util.DoubleThresholdRange.RangeTest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.common.Optional;

/**
 * The tile entity for the projector block.
 */
@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")
public class TileEntityProjector extends TileEntity implements BehaviorListener, PayloadReceiver, ExposedQueue, SimpleComponent {

    public static final String INTERNAL_NAME = "PlayBlockProjector";

    private final BehaviorList behaviors = new BehaviorList();

    private final MediaPlayer mediaPlayer;
    private final DoubleThresholdRange range;
    private final ProjectorOptions options;
    private final QueueBehavior queueBehavior;

    private final RangeTest rangeTest;
    private boolean withinRange = false;

    /**
     * Construct a new instance of the projector tile entity.
     */
    public TileEntityProjector() {
        behaviors.addBehaviorListener(this);
        behaviors.add(range = new DoubleThresholdRange());

        Side side = FMLCommonHandler.instance().getEffectiveSide();

        if (side == Side.CLIENT) {
            behaviors.add(mediaPlayer = new MediaPlayerClient());
            behaviors.add(queueBehavior = new QueueBehavior(null));
            rangeTest = range.createRangeTest();
        } else {
            behaviors.add(mediaPlayer = new MediaPlayerHost());
            behaviors.add(queueBehavior = new QueueBehavior((MediaPlayerHost) mediaPlayer));
            rangeTest = null;
        }

        behaviors.add(options = new ProjectorOptions(mediaPlayer, range));
        if (side != Side.CLIENT) {
            options.useAccessList(true);
        }
    }

    /**
     * Get the behaviors of this projector.
     * 
     * @return the list of behaviors
     */
    public BehaviorList getBehaviors() {
        return behaviors;
    }

    /**
     * Get the media player.
     * 
     * @return the media player
     */
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * Get the range manager.
     * 
     * @return the range manager, null if on the server
     */
    public DoubleThresholdRange getRange() {
        return range;
    }

    /**
     * Get the options controller.
     * 
     * @return options controller
     */
    public ProjectorOptions getOptions() {
        return options;
    }

    /**
     * Get the access list.
     * 
     * @return the access list, null if on the client
     */
    public AccessList getAccessList() {
        return getOptions().getAccessList();
    }

    /**
     * Get the queue behavior.
     * 
     * @return the queue behavior
     */
    @Override
    public QueueBehavior getQueueBehavior() {
        return queueBehavior;
    }

    /**
     * Get the local player is in range.
     * 
     * @return true if in range
     */
    public boolean inRange() {
        if (rangeTest == null) {
            throw new RuntimeException("Can't do range test on server");
        }

        return rangeTest.getCachedInRange();
    }

    @Override
    public void readPayload(EntityPlayer player, ByteBufInputStream in) throws IOException {
        BehaviorPayload payload = new BehaviorPayload();
        payload.read(in);
        behaviors.readPayload(player, payload, in);
    }

    @Override
    public Packet getDescriptionPacket() {
        // Client -> Server
        NBTTagCompound tag = new NBTTagCompound();
        behaviors.writeNetworkedNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        // Only called on the client
        NBTTagCompound tag = packet.func_148857_g();
        behaviors.readNetworkedNBT(tag);
    }

    @Override
    public void networkedNbt(NBTTagCompound tag) {
        if (!this.worldObj.isRemote) {
            try {
                super.writeToNBT(tag); // Coordinates
                ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer());
                out.writeByte(PlayBlockPayload.Type.TILE_ENTITY_NBT.ordinal());
                ByteBufUtils.writeTag(out.buffer(), tag);
                FMLProxyPacket packet = new FMLProxyPacket(out.buffer(), PlayBlock.CHANNEL_ID);
                SharedRuntime.networkWrapper.sendToAllAround(packet, new NetworkRegistry.TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 250));
                out.close();
            } catch (IOException e) {
                PlayBlock.log(Level.WARN, "Failed to send tile info to players!");
            }
        }
    }

    @Override
    public void payloadSend(BehaviorPayload behaviorPayload, List<EntityPlayer> players) {
        PlayBlockPayload payload = new PlayBlockPayload(new TileEntityPayload(this, behaviorPayload));

        if (worldObj.isRemote) {
            PacketHandler.sendToServer(payload);
        } else {
            PacketHandler.sendToClient(payload, players);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        // Saving to disk
        super.writeToNBT(tag);
        behaviors.writeSaveNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        // Saving to disk
        super.readFromNBT(tag);
        behaviors.readSaveNBT(tag);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (this.worldObj.isRemote) {
            ((MediaPlayerClient) mediaPlayer).release();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (this.worldObj.isRemote) {
            ((MediaPlayerClient) mediaPlayer).release();
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (this.worldObj.isRemote) {
            if (rangeTest.inRange(xCoord, yCoord, zCoord)) {
                ((MediaPlayerClient) mediaPlayer).enable();
            } else {
                ((MediaPlayerClient) mediaPlayer).disable();
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        // XXX: May want to use a less expansive render AABB
        return INFINITE_EXTENT_AABB;
    }
    
    //Computer compat

	@Override
	public String getComponentName() {
		return "pbProjector";
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] setURL(Context context, Arguments args) {
		String uri = args.checkString(0);
		if (uri != null) {
			float width = mediaPlayer.getWidth();
			float height = mediaPlayer.getHeight();
			float triggerRange = range.getTriggerRange();
			float fadeRange = range.getFadeRange();
			this.getOptions().sendUpdate(uri, width, height, triggerRange, fadeRange);
		}
		return null;
	}
	
	@Callback
	@Optional.Method(modid="OpenComputers")
	public Object[] setResolution(Context context, Arguments args) {
		String uri = mediaPlayer.getUri();
		float width = args.checkInteger(0);
		float height = args.checkInteger(1);
		float triggerRange = range.getTriggerRange();
		float fadeRange = range.getFadeRange();
		this.getOptions().sendUpdate(uri, width, height, triggerRange, fadeRange);
		return null;
	}
	
	@Callback
	@Optional.Method(modid="OpenComputers")
	public Object[] setRanges(Context context, Arguments args) {
		String uri = mediaPlayer.getUri();
		float width = mediaPlayer.getWidth();
		float height = mediaPlayer.getHeight();
		float triggerRange = args.checkInteger(0);
		float fadeRange = args.checkInteger(1);
		this.getOptions().sendUpdate(uri, width, height, triggerRange, fadeRange);
		return null;
	}
}
