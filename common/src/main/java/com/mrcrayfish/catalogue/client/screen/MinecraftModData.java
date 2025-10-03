package com.mrcrayfish.catalogue.client.screen;

import com.mrcrayfish.catalogue.client.IModData;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class MinecraftModData implements IModData
{
    @Override
    public Type getType()
    {
        return Type.LIBRARY;
    }

    @Override
    public String getModId()
    {
        return "minecraft";
    }

    @Override
    public String getDisplayName()
    {
        return "Minecraft";
    }

    @Override
    public String getVersion()
    {
        return SharedConstants.getCurrentVersion().name();
    }

    @Override
    public String getDescription()
    {
        // Description provided by minecraft.wiki (CC BY-NC-SA 3.0)
        return "Minecraft is a 3D sandbox adventure game developed by Mojang Studios where players can interact with a fully customizable three-dimensional world made of blocks and entities. Its diverse gameplay options allow players to choose the way they play, creating countless possibilities.";
    }

    @Nullable
    @Override
    public String getItemIcon()
    {
        return null;
    }

    @Nullable
    @Override
    public String getImageIcon()
    {
        return null;
    }

    @Override
    public String getLicense()
    {
        return "All Rights Reserved";
    }

    @Nullable
    @Override
    public String getCredits()
    {
        return null;
    }

    @Nullable
    @Override
    public String getAuthors()
    {
        return "Mojang AB";
    }

    @Nullable
    @Override
    public String getHomepage()
    {
        return "https://www.minecraft.net";
    }

    @Nullable
    @Override
    public String getIssueTracker()
    {
        return "https://bugs.mojang.com/projects/MC/issues";
    }

    @Nullable
    @Override
    public String getBanner()
    {
        return null;
    }

    @Nullable
    @Override
    public String getBackground()
    {
        return null;
    }

    @Override
    public Update getUpdate()
    {
        return null;
    }

    @Override
    public Set<String> getDependencies()
    {
        return Collections.emptySet();
    }

    @Override
    public boolean hasConfig()
    {
        return true;
    }

    @Override
    public boolean isLogoSmooth()
    {
        return true;
    }

    @Override
    public boolean isLibrary()
    {
        return true;
    }

    @Override
    public void openConfigScreen(Screen parent)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new OptionsScreen(parent, minecraft.options));
    }

    @Override
    public void drawUpdateIcon(GuiGraphics graphics, Update update, int x, int y)
    {

    }
}
