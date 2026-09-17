package com.mrcrayfish.catalogue.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.client.NeoForgeModData;
import com.mrcrayfish.catalogue.exception.ModResourceNotFoundException;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.modlist.DefaultModDisplayInfo;
import net.neoforged.neoforge.client.gui.modlist.ImageResource;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class NeoForgePlatformHelper implements IPlatformHelper
{
    @Override
    public List<IModData> getAllModData()
    {
        return ModList.get().getMods().stream().map(NeoForgeModData::new).collect(Collectors.toList());
    }

    @Override
    public File getModDirectory()
    {
        return FMLPaths.MODSDIR.get().toFile();
    }

    @Override
    public Path getConfigDirectory()
    {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public NativeImage loadImageFromModResource(String modId, String resource) throws IOException
    {
        ImageResource imageResource = ModList.get().getModContainerById(modId).map(modContainer -> {
            var displayInfo = new DefaultModDisplayInfo(modContainer);
            return displayInfo.convertPath(resource);
        }).orElse(null);
        if(imageResource != null)
        {
            var ioSupplier = imageResource.get(Minecraft.getInstance().getResourceManager());
            if(ioSupplier != null)
            {
                try(InputStream stream = ioSupplier.get())
                {
                    return NativeImage.read(stream);
                }
            }
        }
        throw new ModResourceNotFoundException();
    }

    @Override
    public boolean isModLoaded(String modId)
    {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public GuiRenderState getGuiRenderState(GuiGraphicsExtractor extractor)
    {
        return extractor.guiRenderState;
    }
}
