package com.mrcrayfish.catalogue.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.client.NeoForgeModData;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class NeoForgePlatformHelper implements IPlatformHelper
{
    @Override
    public boolean isForge()
    {
        return true;
    }

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
            throw new RuntimeException(e);
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
        graphics.blit(NeoForgeModData.VERSION_CHECK_ICONS, x, y, 24, 0, 8, 8, 64, 16);
    }
}
