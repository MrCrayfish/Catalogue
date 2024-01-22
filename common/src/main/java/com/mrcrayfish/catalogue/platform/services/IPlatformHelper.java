package com.mrcrayfish.catalogue.platform.services;

import com.mojang.blaze3d.platform.NativeImage;
import com.mrcrayfish.catalogue.client.IModData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface IPlatformHelper
{
    List<IModData> getAllModData();

    File getModDirectory();

    default boolean isForge()
    {
        return false;
    }

    void loadNativeImage(String modId, String resource, Consumer<NativeImage> consumer);

    boolean isCustomItemRendering(Item item);

    void drawUpdateIcon(GuiGraphics graphics, int x, int y);
}