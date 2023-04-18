package com.mrcrayfish.catalogue.platform;

import com.mrcrayfish.catalogue.Constants;
import com.mrcrayfish.catalogue.platform.services.IComponentHelper;
import com.mrcrayfish.catalogue.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class ClientServices
{
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IComponentHelper COMPONENT = load(IComponentHelper.class);

    public static <T> T load(Class<T> clazz)
    {
        final T loadedService = ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}