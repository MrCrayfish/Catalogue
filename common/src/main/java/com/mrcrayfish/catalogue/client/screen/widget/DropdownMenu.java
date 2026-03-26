package com.mrcrayfish.catalogue.client.screen.widget;

import com.mojang.blaze3d.platform.Window;
import com.mrcrayfish.catalogue.Constants;
import com.mrcrayfish.catalogue.client.screen.DropdownMenuHandler;
import com.mrcrayfish.catalogue.client.screen.layout.BorderedLinearLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class DropdownMenu extends AbstractWidget
{
    private final DropdownMenuHandler handler;
    private final BorderedLinearLayout layout = (BorderedLinearLayout)
        BorderedLinearLayout.vertical().border(1).spacing(1);
    private final List<AbstractWidget> items = new ArrayList<>();
    private Alignment alignment = Alignment.BELOW_LEFT;
    private @Nullable DropdownMenu parent;
    private @Nullable DropdownMenu subMenu;

    private DropdownMenu(DropdownMenuHandler handler)
    {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
        this.handler = handler;
        this.visible = false;
    }

    private void setAlignment(Alignment alignment)
    {
        this.alignment = alignment;
    }

    public void toggle(int mouseX, int mouseY)
    {
        this.toggle(new ScreenRectangle(mouseX, mouseY, 0, 0));
    }

    public void toggle(AbstractWidget widget)
    {
        this.toggle(widget.getRectangle());
    }

    public void toggle(ScreenRectangle rect)
    {
        if(!this.visible)
        {
            this.show(rect);
        }
        else
        {
            this.hide();
        }
    }

    private void show(ScreenRectangle rect)
    {
        this.updatePosition(rect);
        this.items.forEach(child -> {
            child.visible = true;
        });
        this.visible = true;
        if(this.parent == null)
        {
            this.handler.setMenu(this);
        }
    }

    public void hide()
    {
        this.items.forEach(child -> {
            child.visible = false;
            if(child instanceof DropdownItem menu) {
                menu.subMenu.hide();
            }
        });
        this.subMenu = null;
        this.visible = false;
    }

    private void updatePosition(ScreenRectangle rect)
    {
        this.layout.arrangeElements();
        this.width = this.layout.getWidth();
        this.height = this.layout.getHeight();
        this.alignment.aligner.accept(this, rect);
        this.layout.setX(this.getX());
        this.layout.setY(this.getY());
    }

    public void addItem(MenuItem item)
    {
        this.layout.addChild(item);
        this.items.add(item);
        item.visible = false;
    }

    private void deepClose()
    {
        this.handler.setMenu(null);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick)
    {
        graphics.pose().pushMatrix();
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        graphics.fill(0, 0, window.getWidth(), window.getHeight(), 0x50000000);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xAA000000);
        this.items.forEach(widget -> {
            widget.render(graphics, mouseX, mouseY, deltaTick);
        });
        if(this.subMenu != null)
        {
            this.subMenu.render(graphics, mouseX, mouseY, deltaTick);
        }
        graphics.pose().pushMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if(!this.active || !this.visible)
            return false;

        AtomicBoolean clicked = new AtomicBoolean();
        this.layout.visitWidgets(widget -> {
            if(widget.mouseClicked(event, doubleClick)) {
                clicked.set(true);
            }
        });
        return clicked.get();
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> consumer)
    {
        this.layout.visitWidgets(consumer);
    }

    public static class MenuItem extends AbstractWidget
    {
        protected static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "dropdown/item"),
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "dropdown/item_highlighted")
        );
        protected final DropdownMenu parent;
        private final Runnable onClick;

        public MenuItem(DropdownMenu menu, Component label, Runnable onClick)
        {
            super(0, 0, 100, 20, label);
            this.parent = menu;
            this.onClick = onClick;
        }

        protected boolean selected()
        {
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick)
        {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHovered() || this.selected()), this.getX(), this.getY(), this.getWidth(), this.getHeight());

            Font font = Minecraft.getInstance().font;
            int offset = (this.getHeight() - font.lineHeight) / 2 + 1;
            graphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX() + offset, this.getY() + offset, 0xFFFFFFFF);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick)
        {
            this.onClick.run();
            this.parent.deepClose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output)
        {
            output.add(NarratedElementType.TITLE, this.getMessage());
        }

        protected int calculateWidth()
        {
            Font font = Minecraft.getInstance().font;
            int labelOffset = (this.getHeight() - font.lineHeight) / 2 + 1;
            int labelWidth = font.width(this.getMessage());
            return labelOffset + labelWidth + labelOffset;
        }
    }

    private static class CheckboxMenuItem extends MenuItem
    {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/checkbox.png");

        private final MutableBoolean holder;
        private final Function<Boolean, Boolean> callback;

        public CheckboxMenuItem(DropdownMenu menu, Component label, MutableBoolean holder, Function<Boolean, Boolean> callback)
        {
            super(menu, label, () -> {});
            this.holder = holder;
            this.callback = callback;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick)
        {
            super.renderWidget(graphics, mouseX, mouseY, deltaTick);
            int offset = (this.getHeight() - 14) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX() + this.getWidth() - 14 - offset, this.getY() + offset, this.isHoveredOrFocused() ? 14 : 0, this.holder.get() ? 14 : 0, 14, 14, 64, 64);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick)
        {
            boolean newValue = !this.holder.get();
            this.holder.setValue(newValue);
            if(this.callback.apply(newValue))
            {
                this.parent.deepClose();
            }
        }

        @Override
        protected int calculateWidth()
        {
            Font font = Minecraft.getInstance().font;
            int labelOffset = (this.getHeight() - font.lineHeight) / 2 + 1;
            int labelWidth = font.width(this.getMessage());
            int checkboxOffset = (this.getHeight() - 14) / 2;
            return labelOffset + labelWidth + labelOffset + 14 + checkboxOffset;
        }
    }

    private static class DropdownItem extends MenuItem
    {
        private final DropdownMenu subMenu;

        public DropdownItem(DropdownMenu menu, DropdownMenu subMenu, Component label)
        {
            super(menu, label, () -> {});
            this.subMenu = subMenu;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick)
        {
            super.renderWidget(graphics, mouseX, mouseY, deltaTick);
            Font font = Minecraft.getInstance().font;
            int top = this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1;
            graphics.drawString(Minecraft.getInstance().font, ">", this.getX() + this.getWidth() - 10, top, 0xFFFFFFFF);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick)
        {
            if(this.parent.subMenu != null)
            {
                this.parent.subMenu.hide();
                if(this.parent.subMenu == this.subMenu)
                {
                    this.parent.subMenu = null;
                    return;
                }
            }
            this.parent.subMenu = this.subMenu;
            this.subMenu.show(this.getRectangle());
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer)
        {
            consumer.accept(this);
            this.subMenu.visitWidgets(consumer);
        }

        @Override
        protected boolean selected()
        {
            return this.parent.subMenu == this.subMenu;
        }

        @Override
        protected int calculateWidth()
        {
            Font font = Minecraft.getInstance().font;
            int labelOffset = (this.getHeight() - font.lineHeight) / 2 + 1;
            int labelWidth = font.width(this.getMessage());
            int arrowWidth = font.width(">");
            return labelOffset + labelWidth + labelOffset + arrowWidth + labelOffset;
        }
    }

    private interface MenuAligner
    {
        void accept(DropdownMenu menu, ScreenRectangle rectangle);
    }

    public enum Alignment
    {
        ABOVE_LEFT((menu, rectangle) -> {
            menu.setX(rectangle.left());
            menu.setY(rectangle.top() - menu.getHeight());
        }),
        ABOVE_RIGHT((menu, rectangle) -> {
            menu.setX(rectangle.right() - menu.getWidth());
            menu.setY(rectangle.top() - menu.getHeight());
        }),
        BELOW_LEFT((menu, rectangle) -> {
            menu.setX(rectangle.left() - 1);
            menu.setY(rectangle.bottom());
        }),
        BELOW_RIGHT((menu, rectangle) -> {
            menu.setX(rectangle.right() - menu.getWidth() + 1);
            menu.setY(rectangle.bottom());
        }),
        END_TOP((menu, rectangle) -> {
            menu.setX(rectangle.right());
            menu.setY(rectangle.top() - 1);
        }),
        END_BOTTOM((menu, rectangle) -> {
            menu.setX(rectangle.right());
            menu.setY(rectangle.bottom() - menu.getHeight() + 1);
        });

        private final MenuAligner aligner;

        Alignment(MenuAligner positioner)
        {
            this.aligner = positioner;
        }

    }

    public static Builder builder(DropdownMenuHandler handler)
    {
        return new Builder(handler);
    }

    public static class Builder
    {
        private final DropdownMenuHandler handler;
        private final DropdownMenu base;
        private final List<MenuItem> items = new ArrayList<>();
        private int minItemWidth = 0;
        private int minItemHeight = 20;

        private Builder(DropdownMenuHandler handler)
        {
            this.handler = handler;
            this.base = new DropdownMenu(handler);
        }

        public Builder setMinItemSize(int width, int height)
        {
            this.minItemWidth = width;
            this.minItemHeight = height;
            return this;
        }

        public Builder setAlignment(Alignment alignment)
        {
            this.base.setAlignment(alignment);
            return this;
        }

        public Builder addItem(Component label, Runnable onClick)
        {
            this.items.add(new MenuItem(this.base, label, onClick));
            return this;
        }

        public Builder addCheckbox(Component label, MutableBoolean holder, Function<Boolean, Boolean> callback)
        {
            this.items.add(new CheckboxMenuItem(this.base, label, holder, callback));
            return this;
        }

        public Builder addMenu(Component label, Builder builder)
        {
            DropdownMenu menu = builder.build();
            menu.parent = this.base;
            this.items.add(new DropdownItem(this.base, menu, label));
            return this;
        }

        public DropdownMenu build()
        {
            int maxWidth = this.items.stream().mapToInt(MenuItem::calculateWidth).max().orElse(100);
            this.items.forEach(widget -> {
                widget.setSize(Math.max(maxWidth, this.minItemWidth), this.minItemHeight);
                this.base.addItem(widget);
            });
            return this.base;
        }
    }
}
