package com.mrcrayfish.catalogue.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.catalogue.Constants;
import com.mrcrayfish.catalogue.client.ForgeModData;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ForgePlatformHelper implements IPlatformHelper
{
    @Override
    public boolean isForge()
    {
        return true;
    }

    @Override
    public List<IModData> getAllModData()
    {
        return ModList.get().getMods().stream().map(ForgeModData::new).collect(Collectors.toList());
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
    public void loadNativeImage(String modId, String resource, Consumer<NativeImage> consumer)
    {
        try
        {
            NativeImage image = null;
            IModFileInfo info = ModList.get().getModFileById(modId);
            Path path = info.getFile().findResource(resource);
            if(Files.exists(path))
            {
                image = NativeImage.read(Files.newInputStream(path));
            }
            Optional.ofNullable(image).ifPresent(consumer);
        }
        catch(IOException e)
        {
            Constants.LOG.error("Failed to load image resource {}:file/{}", modId, resource, e);
        }
    }

    @Override
    public boolean isCustomItemRendering(Item item)
    {
        return IClientItemExtensions.of(item).getCustomRenderer() != Minecraft.getInstance().getItemRenderer().getBlockEntityRenderer();
    }

    @Override
    public void drawUpdateIcon(GuiGraphics graphics, int x, int y)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(ForgeModData.VERSION_CHECK_ICONS, x, y, 24, 0, 8, 8, 64, 16);
    }

    @Override
    public boolean isModLoaded(String modId)
    {
        return ModList.get().isLoaded(modId);
    }
}
