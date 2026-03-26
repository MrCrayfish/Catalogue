package com.mrcrayfish.catalogue.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mrcrayfish.catalogue.client.ForgeModData;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.exception.ModResourceNotFoundException;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ForgePlatformHelper implements IPlatformHelper
{
    @Override
    public List<IModData> getAllModData()
    {
        return ModList.getMods().stream().map(ForgeModData::new).collect(Collectors.toList());
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
        IModFileInfo info = ModList.getModFileById(modId);
        Path path = info.getFile().findResource(resource);
        if(Files.exists(path))
        {
            try(InputStream stream = Files.newInputStream(path))
            {
                return NativeImage.read(stream);
            }
        }
        else
        {
            throw new ModResourceNotFoundException();
        }
    }

    @Override
    public boolean isModLoaded(String modId)
    {
        return ModList.isLoaded(modId);
    }

    @Override
    public GuiRenderState getGuiRenderState(GuiGraphicsExtractor extractor)
    {
        return extractor.getRenderState();
    }
}
