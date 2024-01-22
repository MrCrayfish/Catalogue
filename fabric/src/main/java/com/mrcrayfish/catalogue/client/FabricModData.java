package com.mrcrayfish.catalogue.client;

import com.mrcrayfish.catalogue.Catalogue;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.mrcrayfish.catalogue.client.IModData.Type.*;

/**
 * Author: MrCrayfish
 */
public class FabricModData implements IModData
{
    private final ModMetadata metadata;
    private final Type type;
    private final String imageIcon;
    private final String imageBanner;
    private final String imageBackground;
    private final String itemIcon;

    public FabricModData(ModMetadata metadata)
    {
        this.metadata = metadata;
        this.type = analyzeType(metadata);
        String imageIcon = metadata.getIconPath(64).orElse(null);
        String imageBanner = null;
        String imageBackground = null;
        String itemIcon = null;
        CustomValue value = metadata.getCustomValue("catalogue");
        if(value != null && value.getType() == CustomValue.CvType.OBJECT)
        {
            CustomValue.CvObject catalogueObj = value.getAsObject();
            CustomValue iconValue = catalogueObj.get("icon");
            if(iconValue != null && iconValue.getType() == CustomValue.CvType.OBJECT)
            {
                CustomValue.CvObject iconObj = iconValue.getAsObject();
                CustomValue imageValue = iconObj.get("image");
                if(imageValue != null && imageValue.getType() == CustomValue.CvType.STRING)
                {
                    imageIcon = imageValue.getAsString();
                }
                CustomValue itemValue = iconObj.get("item");
                if(itemValue != null && itemValue.getType() == CustomValue.CvType.STRING)
                {
                    itemIcon = itemValue.getAsString();
                }
            }
            CustomValue bannerValue = catalogueObj.get("banner");
            if(bannerValue != null && bannerValue.getType() == CustomValue.CvType.STRING)
            {
                imageBanner = bannerValue.getAsString();
            }
            CustomValue backgroundValue = catalogueObj.get("background");
            if(backgroundValue != null && backgroundValue.getType() == CustomValue.CvType.STRING)
            {
                imageBackground = backgroundValue.getAsString();
            }
        }
        this.imageIcon = imageIcon;
        this.itemIcon = itemIcon;
        this.imageBanner = imageBanner;
        this.imageBackground = imageBackground;
    }

    @Override
    public Type getType()
    {
        return this.type;
    }

    @Override
    public String getModId()
    {
        return this.metadata.getId();
    }

    @Override
    public String getDisplayName()
    {
        return this.metadata.getName();
    }

    @Override
    public String getVersion()
    {
        return this.metadata.getVersion().getFriendlyString();
    }

    @Override
    public String getDescription()
    {
        return this.metadata.getDescription();
    }

    @Nullable
    @Override
    public String getItemIcon()
    {
        return this.itemIcon;
    }

    @Nullable
    @Override
    public String getImageIcon()
    {
        return this.imageIcon;
    }

    @Override
    public String getLicense()
    {
        return StringUtils.join(this.metadata.getLicense(), ", ");
    }

    @Nullable
    @Override
    public String getCredits()
    {
        return StringUtils.join(this.metadata.getContributors().stream().map(Person::getName).collect(Collectors.toList()), ", ");
    }

    @Nullable
    @Override
    public String getAuthors()
    {
        return StringUtils.join(this.metadata.getAuthors().stream().map(Person::getName).collect(Collectors.toList()), ", ");
    }

    @Nullable
    @Override
    public String getHomepage()
    {
        return this.metadata.getContact().get("homepage").orElse(null);
    }

    @Nullable
    @Override
    public String getIssueTracker()
    {
        return this.metadata.getContact().get("issues").orElse(null);
    }

    @Nullable
    @Override
    public String getBanner()
    {
        return this.imageBanner;
    }

    @Nullable
    @Override
    public String getBackground()
    {
        return this.imageBackground;
    }

    @Override
    public Update getUpdate()
    {
        return null;
    }

    @Override
    public boolean hasConfig()
    {
        return Catalogue.getConfigProviders().containsKey(this.metadata.getId());
    }

    @Override
    public boolean isLogoSmooth()
    {
        return false;
    }

    @Override
    public boolean isInternal()
    {
        return this.type == LIBRARY || this.type == GENERATED;
    }

    @Override
    public void openConfigScreen(Screen parent)
    {
        BiFunction<Screen, ModContainer, Screen> configFactory = Catalogue.getConfigProviders().get(this.metadata.getId());
        if(configFactory != null)
        {
            FabricLoader.getInstance().getModContainer(this.metadata.getId()).ifPresent(container ->
            {
                Screen configScreen = configFactory.apply(parent, container);
                if(configScreen != null)
                {
                    Minecraft.getInstance().setScreen(configScreen);
                }
            });
        }
    }

    private static Type analyzeType(ModMetadata metadata)
    {
        CustomValue lifecycle = metadata.getCustomValue("fabric-api:module-lifecycle");
        if(lifecycle != null)
        {
            return LIBRARY;
        }

        String modId = metadata.getId();
        if(modId.startsWith("fabric-") || modId.equals("minecraft") || modId.equals("java") || modId.equals("fabricloader"))
        {
            return LIBRARY;
        }

        CustomValue generated = metadata.getCustomValue("fabric-loom:generated");
        if(generated != null && generated.getType() == CustomValue.CvType.BOOLEAN && generated.getAsBoolean())
        {
            return GENERATED;
        }

        return DEFAULT;
    }
}
