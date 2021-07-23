package com.mrcrayfish.catalogue.client.screen.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: MrCrayfish
 */
public class CatalogueCheckBoxButton extends Checkbox
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("catalogue", "textures/gui/checkbox.png");

    private final OnPress onPress;

    public CatalogueCheckBoxButton(int x, int y, OnPress onPress)
    {
        super(x, y, 14, 14, TextComponent.EMPTY, false);
        this.onPress = onPress;
    }

    @Override
    public void onPress()
    {
        super.onPress();
        this.onPress.onPress(this);
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        blit(poseStack, this.x, this.y, this.isHovered() ? 14 : 0, this.selected() ? 14 : 0, 14, 14, 64, 64);
        this.renderBg(poseStack, minecraft, mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress
    {
        void onPress(Checkbox button);
    }
}
