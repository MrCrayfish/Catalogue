package com.mrcrayfish.catalogue.client.screen.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.catalogue.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class CatalogueCheckBoxButton extends Checkbox
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Constants.MOD_ID, "textures/gui/checkbox.png");

    private final OnPress onPress;

    public CatalogueCheckBoxButton(int x, int y, OnPress onPress)
    {
        super(x, y, 14, 14, CommonComponents.EMPTY, false);
        this.onPress = onPress;
    }

    @Override
    public void onPress()
    {
        super.onPress();
        this.onPress.onPress(this);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        graphics.blit(TEXTURE, this.getX(), this.getY(), this.isHoveredOrFocused() ? 14 : 0, this.selected() ? 14 : 0, 14, 14, 64, 64);
    }

    public interface OnPress
    {
        void onPress(Checkbox button);
    }
}
