package com.mrcrayfish.catalogue.platform;

import com.mrcrayfish.catalogue.platform.services.IComponentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Author: MrCrayfish
 */
public class ForgeComponentHelper implements IComponentHelper
{
    @Override
    public MutableComponent createTitle()
    {
        return Component.translatable("fml.menu.mods.title");
    }

    @Override
    public MutableComponent createVersion(String version)
    {
        return Component.translatable("fml.menu.mods.info.version", version);
    }

    @Override
    public MutableComponent createFormatted(String formatKey, String value)
    {
        return Component.translatable(formatKey, value);
    }

    @Override
    public Component createFilterUpdates()
    {
        return Component.translatable("fml.menu.mods.filter_updates");
    }

    @Override
    public String getCreditsKey()
    {
        return "catalogue.gui.credits";
    }
}
