package com.mrcrayfish.catalogue.client;

import com.mrcrayfish.catalogue.Constants;
import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ModListScreen;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ClientHandler
{
    @SubscribeEvent
    public static void onOpenScreen(ScreenEvent.Opening event)
    {
        if(event.getScreen() instanceof ModListScreen)
        {
            event.setNewScreen(new CatalogueModListScreen(event.getCurrentScreen()));
        }
    }
}
