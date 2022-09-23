package com.mrcrayfish.catalogue.client;

import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class CatalogueScreenEvents {
    public static void initScreenEvents() {
        ScreenEvents.AFTER_INIT.register(((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen titleScreen) {
                addModsButton(titleScreen);
            }
        }));
    }
    private static void addModsIcon(TitleScreen titleScreen) {
        int x = titleScreen.width / 2;
        int y = titleScreen.height / 4 + 48;
        Screens.getButtons(titleScreen).add(new CatalogueIconButton(x - 124, y + 48, 30, 0, button -> {
            Screens.getClient(titleScreen).setScreen(new CatalogueModListScreen(titleScreen));
        }, (button, poseStack, mouseX, mouseY) -> {
            titleScreen.renderTooltip(poseStack, Component.translatable("catalogue.gui.mod_list"), mouseX, mouseY);
        }));
    }

    private static void addModsButton(TitleScreen titleScreen) {
      //  this.width / 2 - 100, i + j * 2, 200, 20
        var realmsButton = Screens.getButtons(titleScreen).stream().filter(abstractWidget -> abstractWidget.getMessage().equals(Component.translatable("menu.online"))).findFirst().get(); // if it's not there then panik
        realmsButton.setWidth(98);
        var button = new Button(titleScreen.width / 2 + 2, realmsButton.y, 98, 20, Component.translatable("catalogue.gui.mod_list"), (btn) -> {
            Screens.getClient(titleScreen).setScreen(new CatalogueModListScreen(titleScreen));
        });
        Screens.getButtons(titleScreen).add(button);
    }
}
