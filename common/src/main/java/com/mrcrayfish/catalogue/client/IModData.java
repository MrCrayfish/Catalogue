package com.mrcrayfish.catalogue.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

/**
 * Author: MrCrayfish
 */
public interface IModData
{
    Type getType();

    String getModId();

    String getDisplayName();

    String getVersion();

    String getDescription();

    @Nullable
    String getItemIcon();

    @Nullable
    String getImageIcon();

    String getLicense();

    @Nullable
    String getCredits();

    @Nullable
    String getAuthors();

    @Nullable
    String getHomepage();

    @Nullable
    String getIssueTracker();

    @Nullable
    String getBanner();

    @Nullable
    String getBackground();

    Update getUpdate();

    boolean hasConfig();

    boolean isLogoSmooth();

    boolean isInternal();

    void openConfigScreen(Screen parent);

    record Update(boolean animated, String url, int texOffset) {}

    enum Type
    {
        DEFAULT(ChatFormatting.RESET),
        LIBRARY(ChatFormatting.DARK_GRAY),
        GENERATED(ChatFormatting.AQUA);

        private final ChatFormatting style;

        Type(ChatFormatting style)
        {
            this.style = style;
        }

        public ChatFormatting getStyle()
        {
            return this.style;
        }
    }
}
