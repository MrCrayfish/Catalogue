package com.mrcrayfish.catalogue.client.screen.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: MrCrayfish
 */
public class CatalogueCheckBoxButton extends CheckboxButton
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("catalogue", "textures/gui/checkbox.png");

    private final IPressable pressable;

    public CatalogueCheckBoxButton(int x, int y, IPressable pressable)
    {
        super(x, y, 14, 14, StringTextComponent.EMPTY, false);
        this.pressable = pressable;
    }

    @Override
    public void onPress()
    {
        super.onPress();
        this.pressable.onPress(this);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bind(TEXTURE);
        RenderSystem.enableDepthTest();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        blit(matrixStack, this.x, this.y, this.isHovered() ? 14 : 0, this.selected() ? 14 : 0, 14, 14, 64, 64);
        this.renderBg(matrixStack, minecraft, mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    public interface IPressable
    {
        void onPress(CheckboxButton button);
    }
}
