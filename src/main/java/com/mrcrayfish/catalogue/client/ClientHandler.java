package com.mrcrayfish.catalogue.client;

import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = "catalogue", value = Dist.CLIENT)
public class ClientHandler
{
    @SubscribeEvent
    public static void onOpenScreen(ScreenEvent.Opening event)
    {
        if(event.getScreen() instanceof ModListScreen)
        {
            event.setNewScreen(new CatalogueModListScreen());
        }
    }
}
