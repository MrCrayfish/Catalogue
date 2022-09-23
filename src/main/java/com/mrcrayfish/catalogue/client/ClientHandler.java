package com.mrcrayfish.catalogue.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Author: MrCrayfish
 */
//@Mod.EventBusSubscriber(modid = "catalogue", value = Dist.CLIENT)
public class ClientHandler implements ClientModInitializer
{
    @Override
    public void onInitializeClient() {
        CatalogueScreenEvents.initScreenEvents();
    }
    /*@SubscribeEvent
    public static void onOpenScreen(ScreenEvent.Opening event)
    {
        if(event.getScreen() instanceof ModListScreen)
        {
            event.setNewScreen(new CatalogueModListScreen());
        }
    }*/
}
