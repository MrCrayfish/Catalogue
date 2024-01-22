package com.mrcrayfish.catalogue.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import javax.annotation.Nullable;

/**
 * Author: MrCrayfish
 */
public class ForgeModData implements IModData
{
    private final IModInfo info;
    private final Type type;

    public ForgeModData(IModInfo info)
    {
        this.info = info;
        this.type = analyzeType(info);
    }

    @Override
    public Type getType()
    {
        return this.type;
    }

    @Override
    public String getModId()
    {
        return this.info.getModId();
    }

    @Override
    public String getDisplayName()
    {
        return this.info.getDisplayName();
    }

    @Override
    public String getVersion()
    {
        return this.info.getVersion().toString();
    }

    @Override
    public String getDescription()
    {
        return this.info.getDescription();
    }

    @Override
    @Nullable
    public String getItemIcon()
    {
        String itemIcon = (String) this.info.getModProperties().get("catalogueItemIcon");
        if(itemIcon == null)
        {
            // Fallback to old method for backwards compatibility on Forge
            itemIcon = (String) ((ModInfo) this.info).getConfigElement("itemIcon").orElse(null);
        }
        return itemIcon;
    }

    @Nullable
    @Override
    public String getImageIcon()
    {
        return this.info.getModProperties().get("catalogueImageIcon") instanceof String s ? s : null;
    }

    @Override
    public String getLicense()
    {
        return this.info.getOwningFile().getLicense();
    }

    @Override
    public String getCredits()
    {
        return this.getConfigString("credits");
    }

    @Nullable
    @Override
    public String getAuthors()
    {
        return this.getConfigString("authors");
    }

    @Nullable
    @Override
    public String getHomepage()
    {
        return this.getConfigString("displayURL");
    }

    @Nullable
    @Override
    public String getIssueTracker()
    {
        return this.getConfigString("issueTrackerURL");
    }

    @Nullable
    @Override
    public String getBanner()
    {
        return this.info.getLogoFile().orElse(null);
    }

    @Nullable
    @Override
    public String getBackground()
    {
        return this.info.getModProperties().get("catalogueBackground") instanceof String s ? s : null;
    }

    @Override
    public Update getUpdate()
    {
        VersionChecker.CheckResult result = VersionChecker.getResult(this.info);
        if(result.status().shouldDraw())
        {
            return new Update(result.status().isAnimated(), result.url(), result.status().getSheetOffset());
        }
        return null;
    }

    @Override
    public boolean hasConfig()
    {
        return ConfigScreenHandler.getScreenFactoryFor(this.info).isPresent();
    }

    @Override
    public boolean isLogoSmooth()
    {
        return this.info.getLogoBlur();
    }

    @Override
    public void openConfigScreen(Screen parent)
    {
        ConfigScreenHandler.getScreenFactoryFor(this.info).map(f -> f.apply(Minecraft.getInstance(), parent)).ifPresent(newScreen -> Minecraft.getInstance().setScreen(newScreen));
    }

    @Nullable
    private String getConfigString(String key)
    {
        return ((ModInfo) this.info).getConfigElement(key).map(Object::toString).orElse(null);
    }

    private Type analyzeType(IModInfo info)
    {
        return switch(info.getOwningFile().getFile().getType())
        {
            case MOD -> Type.DEFAULT;
            case LIBRARY, LANGPROVIDER, GAMELIBRARY -> Type.LIBRARY;
        };
    }
}
