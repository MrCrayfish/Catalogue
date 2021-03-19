package com.mrcrayfish.catalogue.client;

import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.screen.ModListScreen;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = "catalogue")
public class ClientHandler
{
    @SubscribeEvent
    public static void onOpenScreen(GuiOpenEvent event)
    {
        if(event.getGui() instanceof ModListScreen)
        {
            event.setGui(new CatalogueModListScreen());
        }
    }
}
