package com.mrcrayfish.catalogue.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.catalogue.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class CatalogueCheckBoxButton extends AbstractButton
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/checkbox.png");

    private final OnPress onPress;
    private boolean selected;

    public CatalogueCheckBoxButton(int x, int y, OnPress onPress)
    {
        super(x, y, 14, 14, CommonComponents.EMPTY);
        this.onPress = onPress;
    }

    public boolean isSelected()
    {
        return this.selected;
    }

    @Override
    public void onPress(InputWithModifiers modifiers)
    {
        this.selected = !this.selected;
        this.onPress.onPress(this);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), this.isHoveredOrFocused() ? 14 : 0, this.isSelected() ? 14 : 0, 14, 14, 64, 64);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {

    }

    public interface OnPress
    {
        void onPress(CatalogueCheckBoxButton button);
    }
}
