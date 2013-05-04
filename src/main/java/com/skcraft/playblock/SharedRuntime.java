package com.skcraft.playblock;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;

import com.skcraft.playblock.player.ProjectorBlock;
import com.skcraft.playblock.player.ProjectorTileEntity;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

/**
 * Initializes everything.
 */
public class SharedRuntime {

    private Block projectorBlock;

    /**
     * Called on FML pre-initialization.
     * 
     * @param event the event
     */
    public void preInit(FMLPreInitializationEvent event) {
    }

    /**
     * Called on FML initialization.
     * 
     * @param event the event
     */
    public void load(FMLInitializationEvent event) {
        projectorBlock = new ProjectorBlock(3400, 0, Material.iron)
                .setHardness(0.5F).setStepSound(Block.soundGlassFootstep)
                .setLightValue(1.0F)
                .setBlockName(ProjectorBlock.INTERNAL_NAME)
                .setCreativeTab(CreativeTabs.tabMisc);

        GameRegistry.registerBlock(projectorBlock, ProjectorBlock.INTERNAL_NAME);
        GameRegistry.registerTileEntity(ProjectorTileEntity.class,
                ProjectorTileEntity.INTERNAL_NAME);
        LanguageRegistry.addName(projectorBlock, "PlayBlock Projector");
    }

    /**
     * Called on server start.
     * 
     * @param event the event
     */
    public void serverStarted(FMLServerStartedEvent event) {
    }


    /**
     * Called on server stop.
     * 
     * @param event the event
     */
    public void serverStopping(FMLServerStoppingEvent event) {
    }

    public void showProjectorGui(EntityPlayer player, ProjectorTileEntity tileEntity) {
        // Overriden on the client
    }

}