package com.mrcrayfish.catalogue.platform.services;

import com.mojang.blaze3d.platform.NativeImage;
import com.mrcrayfish.catalogue.client.IModData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IPlatformHelper
{
    List<IModData> getAllModData();

    File getModDirectory();

    Path getConfigDirectory();

    NativeImage loadImageFromModResource(String modId, String resource) throws IOException;

    boolean isModLoaded(String modId);

    GuiRenderState getGuiRenderState(GuiGraphicsExtractor extractor);
}