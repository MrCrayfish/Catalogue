package com.mrcrayfish.catalogue.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class NeoForgeModData implements IModData
{
    public static final ResourceLocation VERSION_CHECK_ICONS = ResourceLocation.fromNamespaceAndPath("neoforge", "textures/gui/version_check_icons.png");

    private final IModInfo info;
    private final Type type;
    private final Set<String> dependencies;

    public NeoForgeModData(IModInfo info)
    {
        this.info = info;
        this.type = analyzeType(info);
        this.dependencies = analyzeDependencies(info);
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
        return translateOrFallback("fml.menu.mods.info.description." + this.info.getModId(), this.info::getDescription);
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
        if(this.info.getModId().equals("minecraft"))
        {
            return "All Rights Reserved";
        }
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
        if(this.info.getModId().equals("minecraft"))
        {
            return "Mojang AB";
        }
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
            return new Update(result.status().isAnimated(), result.url(), result.status().getSheetOffset(), VERSION_CHECK_ICONS);
        }
        return null;
    }

    @Override
    public Set<String> getDependencies()
    {
        return this.dependencies;
    }

    @Override
    public boolean hasConfig()
    {
        return IConfigScreenFactory.getForMod(this.info).isPresent();
    }

    @Override
    public boolean isLogoSmooth()
    {
        return this.info.getLogoBlur();
    }

    @Override
    public boolean isLibrary()
    {
        return this.info.getModId().equals("neoforge") || this.type != Type.DEFAULT;
    }

    @Override
    public void openConfigScreen(Screen parent)
    {
        ModList.get()
            .getModContainerById(this.info.getModId())
            .flatMap(container -> IConfigScreenFactory.getForMod(this.info)
                .map(f -> f.createScreen(container, parent)))
            .ifPresent(newScreen -> Minecraft.getInstance().setScreen(newScreen));
    }

    @Override
    public void drawUpdateIcon(GuiGraphics graphics, Update update, int x, int y)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int vOffset = update.animated() && (System.currentTimeMillis() / 800 & 1) == 1 ? 8 : 0;
        graphics.blit(update.textures(), x, y, update.texOffset() * 8, vOffset, 8, 8, 64, 16);
    }

    @Nullable
    private String getConfigString(String key)
    {
        return ((ModInfo) this.info).getConfigElement(key).map(Object::toString).orElse(null);
    }

    private static Type analyzeType(IModInfo info)
    {
        // For Fabric libraries loaded by Sinytra Connector
        String modId = info.getModId();
        if(modId.startsWith("fabric-") || modId.equals("fabricloader") || modId.equals("mixinextras"))
        {
            return Type.LIBRARY;
        }

        return switch(info.getOwningFile().getFile().getType())
        {
            case MOD -> Type.DEFAULT;
            case LIBRARY, GAMELIBRARY -> Type.LIBRARY;
        };
    }

    private static Set<String> analyzeDependencies(IModInfo source)
    {
        List<? extends IModInfo.ModVersion> versions = source.getDependencies();
        return versions.stream().filter(version -> {
            return !version.getModId().equals("minecraft") && !version.getModId().equals("neoforge");
        }).map(IModInfo.ModVersion::getModId).collect(Collectors.toUnmodifiableSet());
    }

    private static String translateOrFallback(String key, Supplier<String> fallback)
    {
        return I18n.exists(key) ? I18n.get(key) : fallback.get();
    }
}
