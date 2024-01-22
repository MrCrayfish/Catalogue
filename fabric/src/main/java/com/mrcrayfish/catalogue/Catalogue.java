package com.mrcrayfish.catalogue;

import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class Catalogue implements ClientModInitializer
{
    private static Map<String, BiFunction<Screen, ModContainer, Screen>> providers;

    @Override
    public void onInitializeClient()
    {
        Catalogue.providers = this.findConfigFactoryProviders();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
        {
            if(screen instanceof TitleScreen)
            {
                int x = screen.width / 2;
                int y = screen.height / 4 + 48;
                Button modButton = new CatalogueIconButton(x - 124, y + 48, 30, 0, button -> client.setScreen(new CatalogueModListScreen(screen)));
                modButton.setTooltip(Tooltip.create(Component.translatable("catalogue.gui.mod_list")));
                Screens.getButtons(screen).add(modButton);
            }
            else if(screen instanceof PauseScreen)
            {
                int x = screen.width / 2;
                int y = screen.height / 4 + 32;
                Button modButton = new CatalogueIconButton(x - 124, y + 48, 30, 0, button -> client.setScreen(new CatalogueModListScreen(screen)));
                modButton.setTooltip(Tooltip.create(Component.translatable("catalogue.gui.mod_list")));
                Screens.getButtons(screen).add(modButton);
            }
        });
    }

    public static Map<String, BiFunction<Screen, ModContainer, Screen>> getConfigProviders()
    {
        return providers;
    }

    private Map<String, BiFunction<Screen, ModContainer, Screen>> findConfigFactoryProviders()
    {
        Map<String, BiFunction<Screen, ModContainer, Screen>> factories = new HashMap<>();
        Map<String, BiFunction<Screen, ModContainer, Screen>> providers = new HashMap<>();
        FabricLoader.getInstance().getAllMods().forEach(container -> {
            this.getConfigFactoryClass(container).ifPresent(className -> {
                Optional.ofNullable(createConfigFactoryProvider(className)).ifPresent(map -> {
                    map.forEach(providers::putIfAbsent); // Only adds provider if not provided already
                });
                this.createConfigFactory(className).ifPresent(function -> {
                    factories.put(container.getMetadata().getId(), function);
                });
            });
        });
        providers.putAll(factories);
        return ImmutableMap.copyOf(providers);
    }

    private Optional<String> getConfigFactoryClass(ModContainer container)
    {
        ModMetadata metadata = container.getMetadata();
        CustomValue value = metadata.getCustomValue("catalogue");
        if(value == null || value.getType() != CustomValue.CvType.OBJECT)
            return Optional.empty();

        CustomValue.CvObject catalogueObj = value.getAsObject();
        CustomValue configFactoryValue = catalogueObj.get("configFactory");
        if(configFactoryValue == null || configFactoryValue.getType() != CustomValue.CvType.STRING)
            return Optional.empty();

        return Optional.of(configFactoryValue.getAsString());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, BiFunction<Screen, ModContainer, Screen>> createConfigFactoryProvider(String className)
    {
        try
        {
            Class<?> configFactoryClass = Class.forName(className);
            Method createConfigProviderMethod = configFactoryClass.getDeclaredMethod("createConfigProvider");
            int mods = createConfigProviderMethod.getModifiers();
            if(!Modifier.isPublic(mods))
            {
                throw new RuntimeException("createConfigProvider is not accessible for class: " + className);
            }
            if(!Modifier.isStatic(mods))
            {
                throw new RuntimeException("createConfigProvider is not static for class: " + className);
            }
            if(createConfigProviderMethod.getReturnType() != Map.class)
            {
                throw new RuntimeException("createConfigProvider must return a Map<String, BiFunction<Screen, ModContainer, Screen>>");
            }
            return (Map<String, BiFunction<Screen, ModContainer, Screen>>) createConfigProviderMethod.invoke(null);
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to locate config factory class: " + className);
        }
        catch(InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch(NoSuchMethodException e)
        {
            // Method is optional
        }
        return null;
    }

    private Optional<BiFunction<Screen, ModContainer, Screen>> createConfigFactory(String className)
    {
        try
        {
            Class<?> configFactoryClass = Class.forName(className);
            Method createConfigScreenMethod = configFactoryClass.getDeclaredMethod("createConfigScreen", Screen.class, ModContainer.class);
            int mods = createConfigScreenMethod.getModifiers();
            if(!Modifier.isPublic(mods))
            {
                throw new RuntimeException("createConfigScreen is not accessible for class: " + className);
            }
            if(!Modifier.isStatic(mods))
            {
                throw new RuntimeException("createConfigScreen is not static for class: " + className);
            }
            return Optional.of((currentScreen, container) ->
            {
                try
                {
                    return (Screen) createConfigScreenMethod.invoke(null, currentScreen, container);
                }
                catch(InvocationTargetException | IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
            });
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to locate config factory class: " + className);
        }
        catch(NoSuchMethodException e)
        {
            // Method is optional
        }
        return Optional.empty();
    }
}
