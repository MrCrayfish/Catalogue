package com.mrcrayfish.catalogue.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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

    @Nullable
    Update getUpdate();

    Set<String> getDependencies(); //TODO lazily

    boolean hasConfig();

    boolean isLogoSmooth();

    default boolean isLibrary()
    {
        Type type = this.getType();
        return type == Type.LIBRARY || type == Type.GENERATED;
    }

    void openConfigScreen(Screen parent);

    void drawUpdateIcon(GuiGraphics graphics, Update update, int x, int y);

    record Update(boolean animated, String url, int texOffset, Identifier textures) {}

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
