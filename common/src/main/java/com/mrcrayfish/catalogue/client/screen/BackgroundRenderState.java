package com.mrcrayfish.catalogue.client.screen;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;

public record BackgroundRenderState(BlitRenderState state) implements GuiElementRenderState
{
    @Override
    public void buildVertices(VertexConsumer consumer)
    {
        consumer.addVertexWith2DPose(this.state.pose(), (float) this.state.x0(), (float) this.state.y0()).setUv(this.state.u0(), this.state.v0()).setColor(0xFFFFFFFF);
        consumer.addVertexWith2DPose(this.state.pose(), (float) this.state.x0(), (float) this.state.y1()).setUv(this.state.u0(), this.state.v1()).setColor(0x00000000);
        consumer.addVertexWith2DPose(this.state.pose(), (float) this.state.x1(), (float) this.state.y1()).setUv(this.state.u1(), this.state.v1()).setColor(0x00000000);
        consumer.addVertexWith2DPose(this.state.pose(), (float) this.state.x1(), (float) this.state.y0()).setUv(this.state.u1(), this.state.v0()).setColor(0xFFFFFFFF);
    }

    @Override
    public RenderPipeline pipeline()
    {
        return this.state.pipeline();
    }

    @Override
    public TextureSetup textureSetup()
    {
        return this.state.textureSetup();
    }

    @Override
    public @Nullable ScreenRectangle scissorArea()
    {
        return this.state.scissorArea();
    }

    @Override
    public @Nullable ScreenRectangle bounds()
    {
        return this.state.bounds();
    }
}
