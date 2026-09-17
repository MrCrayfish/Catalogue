package com.mrcrayfish.catalogue.platform;

import com.mrcrayfish.catalogue.platform.services.IComponentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Author: MrCrayfish
 */
public class NeoForgeComponentHelper implements IComponentHelper
{
    @Override
    public MutableComponent createTitle()
    {
        return Component.translatable("fml.menu.mods");
    }

    @Override
    public MutableComponent createVersion(String version)
    {
        return Component.translatable("catalogue.gui.version", version);
    }

    @Override
    public MutableComponent createFormatted(String formatKey, String value)
    {
        return Component.translatable(formatKey, value);
    }

    @Override
    public String getCreditsKey()
    {
        return "catalogue.gui.credits";
    }
}
