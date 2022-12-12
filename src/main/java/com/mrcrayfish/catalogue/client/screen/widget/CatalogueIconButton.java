package com.mrcrayfish.catalogue.client.screen.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Author: MrCrayfish
 */
public class CatalogueIconButton extends Button
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("catalogue", "textures/gui/icons.png");

    private final Component label;
    private final int u, v;

    public CatalogueIconButton(int x, int y, int u, int v, int width, Component label, OnPress onPress)
    {
        super(x, y, width, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.label = label;
        this.u = u;
        this.v = v;
    }

    public CatalogueIconButton(int x, int y, int u, int v, OnPress onPress)
    {
        super(x, y, 20, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.label = CommonComponents.EMPTY;
        this.u = u;
        this.v = v;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        super.renderButton(poseStack, mouseX, mouseY, partialTicks);
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int contentWidth = 10 + minecraft.font.width(this.label) + (!this.label.getString().isEmpty() ? 4 : 0);
        int iconX = this.getX() + (this.width - contentWidth) / 2;
        int iconY = this.getY() + 5;
        float brightness = this.active ? 1.0F : 0.5F;
        RenderSystem.setShaderColor(brightness, brightness, brightness, this.alpha);
        blit(poseStack, iconX, iconY, this.u, this.v, 10, 10, 64, 64);
        RenderSystem.setShaderColor(brightness, brightness, brightness, this.alpha);
        int textColor = (this.active ? 16777215 : 10526880) | Mth.ceil(this.alpha * 255.0F) << 24;
        drawString(poseStack, minecraft.font, this.label, iconX + 14, iconY + 1, textColor);
    }
}
