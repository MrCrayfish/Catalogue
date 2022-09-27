package com.mrcrayfish.catalogue.client;

import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Author: MrCrayfish
 */
public class ClientHandler implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
        {
            if(screen instanceof TitleScreen)
            {
                int x = screen.width / 2;
                int y = screen.height / 4 + 48;
                Screens.getButtons(screen).add(new CatalogueIconButton(x - 124, y + 48, 30, 0, button -> {
                    client.setScreen(new CatalogueModListScreen(screen));
                }, (button, poseStack, mouseX, mouseY) -> {
                    screen.renderTooltip(poseStack, Component.translatable("catalogue.gui.mod_list"), mouseX, mouseY);
                }));
            }
            else if(screen instanceof PauseScreen)
            {
                int x = screen.width / 2;
                int y = screen.height / 4 + 32;
                Screens.getButtons(screen).add(new CatalogueIconButton(x - 124, y + 48, 30, 0, button -> {
                    client.setScreen(new CatalogueModListScreen(screen));
                }, (button, poseStack, mouseX, mouseY) -> {
                    screen.renderTooltip(poseStack, Component.translatable("catalogue.gui.mod_list"), mouseX, mouseY);
                }));
            }
        });
    }
}
