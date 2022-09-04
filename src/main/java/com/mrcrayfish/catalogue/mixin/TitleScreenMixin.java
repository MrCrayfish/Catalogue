package com.mrcrayfish.catalogue.mixin;

import com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Author: MrCrayfish
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen
{
    protected TitleScreenMixin(Component component)
    {
        super(component);
    }

    @Inject(method = "init", at = @At(value = "TAIL"))
    private void initTail(CallbackInfo ci)
    {
        int x = this.width / 2;
        int y = this.height / 4 + 48;
        this.addRenderableWidget(new CatalogueIconButton(x - 124, y + 48, 30, 0, button -> {
            this.minecraft.setScreen(new CatalogueModListScreen());
        }, (button, poseStack, mouseX, mouseY) -> {
            this.renderTooltip(poseStack, Component.translatable("catalogue.gui.mod_list"), mouseX, mouseY);
        }));
    }
}
