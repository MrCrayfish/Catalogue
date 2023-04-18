package com.mrcrayfish.catalogue.platform;

import com.mojang.blaze3d.platform.NativeImage;
import com.mrcrayfish.catalogue.client.ForgeModData;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.resource.ResourcePackLoader;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
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
    public void loadNativeImage(String modId, String resource, Consumer<NativeImage> consumer)
    {
        PathPackResources resourcePack = ResourcePackLoader.getPackFor(modId).orElse(ResourcePackLoader.getPackFor("forge").orElseThrow(() -> new RuntimeException("Can't find forge, WHAT!")));
        IoSupplier<InputStream> supplier = resourcePack.getRootResource(resource);
        if(supplier != null)
        {
            try(InputStream is = supplier.get(); NativeImage image = NativeImage.read(is))
            {
                consumer.accept(image);
            }
            catch(IOException ignored) {}
        }
    }
}
