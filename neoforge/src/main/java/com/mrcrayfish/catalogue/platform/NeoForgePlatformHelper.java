package com.mrcrayfish.catalogue.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.client.NeoForgeModData;
import com.mrcrayfish.catalogue.exception.ModResourceNotFoundException;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;
import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarResource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModFileInfo;

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
        IModFileInfo info = ModList.get().getModFileById(modId);
        JarContents contents = info.getFile().getContents();
        JarResource file = contents.get(resource);
        if(file != null)
        {
            try(InputStream stream = file.open())
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
        return ModList.get().isLoaded(modId);
    }

    @Override
    public GuiRenderState getGuiRenderState(GuiGraphics graphics)
    {
        return graphics.guiRenderState;
    }
}
