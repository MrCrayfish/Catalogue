package com.mrcrayfish.catalogue;

import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.catalogue.client.Config;
import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

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
        Config.load(FabricLoaderImpl.INSTANCE.getConfigDir());

        Catalogue.providers = this.findConfigFactoryProviders();

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
        {
            if(Config.isTitleMenuVisible() && screen instanceof TitleScreen)
            {
                AbstractWidget widget = this.findTitleTarget(screen);
                int x = widget != null ? widget.getX() : screen.width / 2 - 124;
                int y = widget != null ? widget.getY() : screen.height / 4 + 48 + 48;
                if(widget != null) x += Config.getTitleMenuAlign() == Config.Align.LEFT ? -24 : widget.getWidth() + 24;
                Button modButton = new CatalogueIconButton(x, y, 30, 0, button -> client.gui.setScreen(new CatalogueModListScreen(screen)));
                modButton.setTooltip(Tooltip.create(Component.translatable("catalogue.gui.mod_list")));
                Screens.getWidgets(screen).add(modButton);
            }
            else if(Config.isPauseMenuVisible() && screen instanceof PauseScreen)
            {
                AbstractWidget widget = this.findPauseTarget(screen);
                int x = widget != null ? widget.getX() : screen.width / 2 - 124;
                int y = widget != null ? widget.getY() : screen.height / 4 + 32 + 48;
                if(widget != null) x += Config.getPauseMenuAlign() == Config.Align.LEFT ? -24 : widget.getWidth() + 24;
                Button modButton = new CatalogueIconButton(x, y, 30, 0, button -> client.gui.setScreen(new CatalogueModListScreen(screen)));
                modButton.setTooltip(Tooltip.create(Component.translatable("catalogue.gui.mod_list")));
                Screens.getWidgets(screen).add(modButton);
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
                Constants.LOG.error("createConfigProvider does not have public visibility in config provider: {}", className);
                return null;
            }
            if(!Modifier.isStatic(mods))
            {
                Constants.LOG.error("createConfigProvider does not have static modifier in config provider: {}", className);
                return null;
            }
            if(createConfigProviderMethod.getReturnType() != Map.class)
            {
                Constants.LOG.error("createConfigProvider must return a Map<String, BiFunction<Screen, ModContainer, Screen>> in config provider: {}", className);
                return null;
            }
            return (Map<String, BiFunction<Screen, ModContainer, Screen>>) createConfigProviderMethod.invoke(null);
        }
        catch(ClassNotFoundException e)
        {
            Constants.LOG.error("Unable to locate config provider: " + className, e);
        }
        catch(InvocationTargetException | IllegalAccessException e)
        {
            Constants.LOG.error("Failed to load config provider: " + className, e);
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
                Constants.LOG.error("createConfigScreen does not have public visibility in config provider: {}", className);
                return Optional.empty();
            }
            if(!Modifier.isStatic(mods))
            {
                Constants.LOG.error("createConfigScreen does not have static modifier in config provider: {}", className);
                return Optional.empty();
            }
            return Optional.of((currentScreen, container) ->
            {
                try
                {
                    return (Screen) createConfigScreenMethod.invoke(null, currentScreen, container);
                }
                catch(InvocationTargetException | IllegalAccessException e)
                {
                    throw new RuntimeException("Failed to create config screen from provider: " + className, e);
                }
            });
        }
        catch(ClassNotFoundException e)
        {
            Constants.LOG.error("Unable to locate config provider: " + className, e);
        }
        catch(NoSuchMethodException e)
        {
            // Method is optional
        }
        return Optional.empty();
    }

    private AbstractWidget findTitleTarget(Screen screen)
    {
        String targetLang = this.getTitleTargetLang(Config.getTitleMenuTarget());
        return this.findTarget(screen, targetLang);
    }

    private AbstractWidget findPauseTarget(Screen screen)
    {
        String targetLang = this.getPauseTargetLang(Config.getPauseMenuTarget());
        return this.findTarget(screen, targetLang);
    }

    private AbstractWidget findTarget(Screen screen, String langTarget)
    {
        return screen.children().stream()
            .filter(listener -> listener instanceof AbstractWidget)
            .map(listener -> (AbstractWidget) listener)
            .filter(listener -> {
                if(listener instanceof Button btn) {
                    if(btn.getMessage() instanceof MutableComponent component) {
                        if(component.getContents() instanceof TranslatableContents contents) {
                            return contents.getKey().equals(langTarget);
                        }
                    }
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }

    private String getTitleTargetLang(Config.TitleMenuTargets target)
    {
        return switch(target) {
            case SINGLE_PLAYER -> "menu.singleplayer";
            case MULTIPLAYER -> "menu.multiplayer";
            case REALMS -> "menu.online";
            case LANGUAGE -> "options.language";
            case OPTIONS -> "menu.options";
            case QUIT_GAME -> "menu.quit";
            case ACCESSIBILITY -> "options.accessibility";
        };
    }

    private String getPauseTargetLang(Config.PauseMenuTargets target)
    {
        return switch(target) {
            case RETURN_TO_GAME -> "menu.returnToGame";
            case ADVANCEMENTS -> "gui.advancements";
            case FEEDBACK -> "menu.sendFeedback";
            case OPTIONS -> "menu.options";
            case STATISTICS -> "gui.stats";
            case REPORT_BUGS -> "menu.reportBugs";
            case OPEN_TO_LAN -> "menu.shareToLan";
            case SAVE_AND_QUIT -> "menu.returnToMenu";
        };
    }
}
