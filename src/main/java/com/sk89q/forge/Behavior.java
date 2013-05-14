package com.sk89q.forge;

import net.minecraft.nbt.NBTTagCompound;

/**
 * An object that is automatically managed in regards to saving its persistent data and
 * some of its network synchronization concerns.
 */
public interface Behavior {

    /**
     * Write NBT data that needs to be saved to the world.
     * 
     * @param tag the tag
     */
    void writeSaveNBT(NBTTagCompound tag);
    
    /**
     * Read NBT data that has been retrieved from a saved world.
     * 
     * @param tag the tag
     */
    void readSaveNBT(NBTTagCompound tag);
    
    /**
     * Write NBT data that is sent to the client/server. The given NBT data should
     * contain the "full snapshot" to fully describe the object.
     * 
     * @param tag the tag
     */
    void writeNetworkedNBT(NBTTagCompound tag);
    
    /**
     * Read NBT data that is sent to the client/server. The given NBT data should
     * contain the "full snapshot" to fully describe the object.
     * 
     * @param tag the tag
     */
    void readNetworkedNBT(NBTTagCompound tag);
    
    /**
     * Read NBT data that has been sent from 
     * {@link BehaviorListener#nbtEvent(NBTTagCompound)}.
     * 
     * @param tag the tag
     */
    void handleNBTEvent(NBTTagCompound tag);
    
    /**
     * Add a listener.
     * 
     * @param listener the listener
     */
    void addBehaviorListener(BehaviorListener listener);

    /**
     * Remove a listener.
     * 
     * @param listener the listener
     */
    void removeBehaviorListener(BehaviorListener listener);
    
}

