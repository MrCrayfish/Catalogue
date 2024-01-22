package com.mrcrayfish.catalogue.platform.services;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Author: MrCrayfish
 */
public interface IComponentHelper
{
    MutableComponent createTitle();

    MutableComponent createVersion(String version);

    MutableComponent createFormatted(String formatKey, String value);

    Component createFilterUpdates();

    String getCreditsKey();
}
