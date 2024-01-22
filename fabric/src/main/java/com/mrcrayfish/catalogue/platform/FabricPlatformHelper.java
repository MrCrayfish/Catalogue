package com.mrcrayfish.catalogue.platform;

import com.mrcrayfish.catalogue.client.FabricModData;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FabricPlatformHelper implements IPlatformHelper
{
    @Override
    public List<IModData> getAllModData()
    {
        return FabricLoader.getInstance().getAllMods().stream().map(ModContainer::getMetadata).map(FabricModData::new).collect(Collectors.toList());
    }

    @Override
    public File getModDirectory()
    {
        return FabricLoaderImpl.INSTANCE.getModsDirectory();
    }

    @Override
    public void loadNativeImage(String modId, String resource, Consumer<NativeImage> consumer)
    {
        FabricLoader.getInstance().getModContainer(modId).flatMap(container -> container.findPath(resource)).ifPresent(path ->
        {
            try(InputStream is = Files.newInputStream(path); NativeImage icon = NativeImage.read(is))
            {
                consumer.accept(icon);
            }
            catch(IOException ignored) {}
        });
    }

    @Override
    public boolean isCustomItemRendering(Item item)
    {
        return false;
    }

    @Override
    public void drawUpdateIcon(GuiGraphics graphics, int x, int y) {}
}
