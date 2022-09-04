package com.mrcrayfish.catalogue.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrcrayfish.catalogue.client.ScreenUtil;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueCheckBoxButton;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class CatalogueModListScreen extends Screen
{
    private static final ResourceLocation CATALOGUE_ICON = new ResourceLocation("catalogue", "icon.png");
    private static final Comparator<ModEntry> SORT = Comparator.comparing(o -> o.getInfo().getName());
    private static final ResourceLocation MISSING_BANNER = new ResourceLocation("catalogue", "textures/gui/missing_banner.png");
    private static final Map<String, Pair<ResourceLocation, Size2i>> ICON_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> ITEM_CACHE = new HashMap<>();
    private static List<ModInfo> cachedInfo;

    private EditBox searchTextField;
    private ModList modList;
    private ModInfo selectedModInfo;
    private Button modFolderButton;
    private Button configButton;
    private Button websiteButton;
    private Button issueButton;
    private Checkbox libraryButton;
    private StringList descriptionList;
    private int tooltipYOffset;
    private List<? extends FormattedCharSequence> activeTooltip;

    public CatalogueModListScreen()
    {
        super(CommonComponents.EMPTY);
        if(cachedInfo == null) {
            cachedInfo = FabricLoaderImpl.INSTANCE.getAllMods().stream().map(ModInfo::new).toList();
        }
        ICON_CACHE.clear();
    }

    @Override
    protected void init()
    {
        super.init();
        this.searchTextField = new EditBox(this.font, 11, 25, 148, 20, CommonComponents.EMPTY);
        this.searchTextField.setResponder(s -> {
            this.updateSearchField(s);
            this.modList.filterAndUpdateList(s);
            this.updateSelectedModList();
        });
        this.addWidget(this.searchTextField);
        this.modList = new ModList();
        this.modList.setLeftPos(10);
        this.modList.setRenderTopAndBottom(false);
        this.addWidget(this.modList);
        this.addRenderableWidget(new Button(10, this.modList.getBottom() + 8, 127, 20, CommonComponents.GUI_BACK, onPress -> {
            this.minecraft.setScreen(null);
        }));
        this.modFolderButton = this.addRenderableWidget(new CatalogueIconButton(140, this.modList.getBottom() + 8, 0, 0, onPress -> {
            Util.getPlatform().openFile(FabricLoader.getInstance().getGameDir().resolve("mods").toFile());
        }));
        int padding = 10;
        int contentLeft = this.modList.getRight() + 12 + padding;
        int contentWidth = this.width - contentLeft - padding;
        int buttonWidth = (contentWidth - padding) / 3;
        this.configButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft, 105, 10, 0, buttonWidth, Component.translatable("catalogue.gui.config"), onPress ->
        {
            if(this.selectedModInfo != null)
            {
                openConfigScreen(this.selectedModInfo, this);
            }
        }));
        this.configButton.visible = false;
        this.websiteButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft + buttonWidth + 5, 105, 20, 0, buttonWidth, Component.literal("Website"), onPress -> {
            this.selectedModInfo.getHomepageLink().ifPresent(this::openLink);
        }));
        this.websiteButton.visible = false;
        this.issueButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft + buttonWidth + buttonWidth + 10, 105, 30, 0, buttonWidth, Component.literal("Submit Bug"), onPress -> {
            this.selectedModInfo.getIssueLink().ifPresent(this::openLink);
        }));
        this.issueButton.visible = false;
        this.descriptionList = new StringList(contentWidth, this.height - 135 - 55, contentLeft, 130);
        this.descriptionList.setRenderTopAndBottom(false);
        this.descriptionList.setRenderBackground(false);
        this.addWidget(this.descriptionList);

        this.libraryButton = this.addRenderableWidget(new CatalogueCheckBoxButton(this.modList.getRight() - 14, 7, button -> {
            this.modList.filterAndUpdateList(this.searchTextField.getValue());
            this.updateSelectedModList();
        }));

        this.modList.filterAndUpdateList(this.searchTextField.getValue());

        // Resizing window causes all widgets to be recreated, therefore need to update selected info
        if(this.selectedModInfo != null)
        {
            this.setSelectedModInfo(this.selectedModInfo);
            this.updateSelectedModList();
            ModEntry entry = this.modList.getEntryFromInfo(this.selectedModInfo);
            if(entry != null)
            {
                this.modList.centerScrollOn(entry);
            }
        }
        this.updateSearchField(this.searchTextField.getValue());
    }

    private void openLink(String url)
    {
        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        this.handleComponentClicked(style);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.activeTooltip = null;
        this.renderBackground(poseStack);
        this.drawModList(poseStack, mouseX, mouseY, partialTicks);
        this.drawModInfo(poseStack, mouseX, mouseY, partialTicks);
        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Draw Catalogue Icon
        RenderSystem.setShaderTexture(0, CATALOGUE_ICON);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Screen.blit(poseStack, 10, 9, 10, 10, 0.0F, 0.0F, 160, 160, 160, 160);

        if(ScreenUtil.isMouseWithin(10, 9, 10, 10, mouseX, mouseY))
        {
            this.setActiveTooltip(Component.translatable("catalogue.gui.info"));
            this.tooltipYOffset = 10;
        }

        if(this.modFolderButton.isMouseOver(mouseX, mouseY))
        {
            this.setActiveTooltip(Component.translatable("catalogue.gui.open_mods_folder"));
        }

        if(this.activeTooltip != null)
        {
            this.renderTooltip(poseStack, this.activeTooltip, mouseX, mouseY + this.tooltipYOffset);
            this.tooltipYOffset = 0;
        }
    }

    private void updateSelectedModList()
    {
        ModEntry selectedEntry = this.modList.getEntryFromInfo(this.selectedModInfo);
        if(selectedEntry != null)
        {
            this.modList.setSelected(selectedEntry);
        }
    }

    private void updateSearchField(String value)
    {
        if(value.isEmpty())
        {
            this.searchTextField.setSuggestion(Component.translatable("catalogue.gui.search").append(Component.literal("...")).getString());
        }
        else
        {
            Optional<ModInfo> optional = cachedInfo.stream().filter(info -> {
                return info.getName().toLowerCase(Locale.ENGLISH).startsWith(value.toLowerCase(Locale.ENGLISH));
            }).min(Comparator.comparing(ModInfo::getName));
            if(optional.isPresent())
            {
                int length = value.length();
                String displayName = optional.get().getName();
                this.searchTextField.setSuggestion(displayName.substring(length));
            }
            else
            {
                this.searchTextField.setSuggestion("");
            }
        }
    }

    /**
     * Draws everything considered left of the screen; title, search bar and mod list.
     *
     * @param poseStack  the current matrix stack
     * @param mouseX       the current mouse x position
     * @param mouseY       the current mouse y position
     * @param partialTicks the partial ticks
     */
    private void drawModList(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.modList.render(poseStack, mouseX, mouseY, partialTicks);
        drawCenteredString(poseStack, this.font, Component.translatable("catalogue.gui.mod_list").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.WHITE), 85, 10, 0xFFFFFF);
        this.searchTextField.render(poseStack, mouseX, mouseY, partialTicks);

        if(ScreenUtil.isMouseWithin(this.modList.getRight() - 14, 7, 14, 14, mouseX, mouseY))
        {
            this.setActiveTooltip(Component.translatable("catalogue.gui.internal_libraries"));
            this.tooltipYOffset = 10;
        }
    }

    /**
     * Draws everything considered right of the screen; logo, mod title, description and more.
     *
     * @param poseStack  the current matrix stack
     * @param mouseX       the current mouse x position
     * @param mouseY       the current mouse y position
     * @param partialTicks the partial ticks
     */
    private void drawModInfo(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.vLine(poseStack, this.modList.getRight() + 11, -1, this.height, 0xFF707070);
        fill(poseStack, this.modList.getRight() + 12, 0, this.width, this.height, 0x66000000);
        this.descriptionList.render(poseStack, mouseX, mouseY, partialTicks);

        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;

        if(this.selectedModInfo != null)
        {
            // Draw mod logo
            this.drawLogo(poseStack, contentWidth, contentLeft, 10, this.width - (this.modList.getRight() + 12 + 10) - 10, 50);

            // Draw mod name
            poseStack.pushPose();
            poseStack.translate(contentLeft, 70, 0);
            poseStack.scale(2.0F, 2.0F, 2.0F);
            drawString(poseStack, this.font, this.selectedModInfo.getName(), 0, 0, 0xFFFFFF);
            poseStack.popPose();

            // Draw version
            Component modId = Component.literal("Mod ID: " + this.selectedModInfo.getId()).withStyle(ChatFormatting.DARK_GRAY);
            int modIdWidth = this.font.width(modId);
            drawString(poseStack, this.font, modId, contentLeft + contentWidth - modIdWidth, 92, 0xFFFFFF);

            // Draw version
            this.drawStringWithLabel(poseStack, "catalogue.gui.version", this.selectedModInfo.getVersion(), contentLeft, 92, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);

            int labelOffset = this.height - 20;

            // Draw license
            String license = this.selectedModInfo.getLicense();
            this.drawStringWithLabel(poseStack, "catalogue.gui.licenses", license, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
            labelOffset -= 15;

            // Draw credits
            String credits = this.selectedModInfo.getContributors();
            if(!credits.isEmpty())
            {
                this.drawStringWithLabel(poseStack, "catalogue.gui.contributors", credits, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
                labelOffset -= 15;
            }

            // Draw authors
            String authors = this.selectedModInfo.getAuthors();
            if(!authors.isEmpty())
            {
                this.drawStringWithLabel(poseStack, "catalogue.gui.authors", authors, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
            }
        }
        else
        {
            Component message = Component.translatable("catalogue.gui.no_selection").withStyle(ChatFormatting.GRAY);
            drawCenteredString(poseStack, this.font, message, contentLeft + contentWidth / 2, this.height / 2 - 5, 0xFFFFFF);
        }
    }

    /**
     * Draws a string and prepends a label. If the formed string and label is longer than the
     * specified max width, it will automatically be trimmed and allows the user to hover the
     * string with their mouse to read the full contents.
     *
     * @param poseStack the current matrix stack
     * @param format      a string to prepend to the content
     * @param text        the string to render
     * @param x           the x position
     * @param y           the y position
     * @param maxWidth    the maximum width the string can render
     * @param mouseX      the current mouse x position
     * @param mouseY      the current mouse u position
     */
    private void drawStringWithLabel(PoseStack poseStack, String format, String text, int x, int y, int maxWidth, int mouseX, int mouseY, ChatFormatting labelColor, ChatFormatting contentColor)
    {
        String formatted = Component.translatable(format, text).getString();
        String label = formatted.substring(0, formatted.indexOf(":") + 1);
        String content = formatted.substring(formatted.indexOf(":") + 1);
        if(this.font.width(formatted) > maxWidth)
        {
            content = this.font.plainSubstrByWidth(content, maxWidth - this.font.width(label) - 7) + "...";
            MutableComponent credits = Component.literal(label).withStyle(labelColor);
            credits.append(Component.literal(content).withStyle(contentColor));
            drawString(poseStack, this.font, credits, x, y, 0xFFFFFF);
            if(ScreenUtil.isMouseWithin(x, y, maxWidth, 9, mouseX, mouseY)) // Sets the active tool tip if string is too long so users can still read it
            {
                this.setActiveTooltip(Component.literal(text));
            }
        }
        else
        {
            drawString(poseStack, this.font, Component.literal(label).withStyle(labelColor).append(Component.literal(content).withStyle(contentColor)), x, y, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(ScreenUtil.isMouseWithin(10, 9, 10, 10, (int) mouseX, (int) mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            this.openLink("https://www.curseforge.com/minecraft/mc-mods/catalogue");
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setActiveTooltip(Component content)
    {
        this.activeTooltip = this.font.split(content, Math.min(200, this.width));
        this.tooltipYOffset = 0;
    }

    private void setSelectedModInfo(ModInfo info)
    {
        this.selectedModInfo = info;
        this.configButton.visible = true;
        this.websiteButton.visible = true;
        this.issueButton.visible = true;
        this.configButton.active = info.getConfigFactory().isPresent();
        this.websiteButton.active = info.getHomepageLink().isPresent();
        this.issueButton.active = info.getIssueLink().isPresent();
        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;
        int labelCount = this.getLabelCount(info);
        this.descriptionList.updateSize(contentWidth, this.height - 135 - 10 - labelCount * 15, 130, this.height - 10 - labelCount * 15);
        this.descriptionList.setLeftPos(contentLeft);
        this.descriptionList.setTextFromInfo(info);
        this.descriptionList.setScrollAmount(0);
    }

    private int getLabelCount(ModInfo info)
    {
        int count = 1; //1 by default since license property will always exist
        if(!info.getContributors().isEmpty()) count++;
        if(!info.getAuthors().isEmpty()) count++;
        return count;
    }

    private void drawLogo(PoseStack poseStack, int contentWidth, int x, int y, int maxWidth, int maxHeight)
    {
        if(this.selectedModInfo != null)
        {
            ResourceLocation logoResource = MISSING_BANNER;
            Size2i size = new Size2i(600, 120);

            if(ICON_CACHE.containsKey(this.selectedModInfo.getId()))
            {
                Pair<ResourceLocation, Size2i> logoInfo = ICON_CACHE.get(this.selectedModInfo.getId());
                if(logoInfo.getLeft() != null)
                {
                    logoResource = logoInfo.getLeft();
                    size = logoInfo.getRight();
                }
            }

            RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            RenderSystem.setShaderTexture(0, logoResource);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();

            int width = size.width;
            int height = size.height;
            if(size.width > maxWidth)
            {
                width = maxWidth;
                height = (width * size.height) / size.width;
            }
            if(height > maxHeight)
            {
                height = maxHeight;
                width = (height * size.width) / size.height;
            }

            x += (contentWidth - width) / 2;
            y += (maxHeight - height) / 2;

            Screen.blit(poseStack, x, y, width, height, 0.0F, 0.0F, size.width, size.height, size.width, size.height);

            RenderSystem.disableBlend();
        }
    }

    private void loadAndCacheIcon(ModInfo info)
    {
        if(ICON_CACHE.containsKey(info.getId()))
            return;

        // Fills an empty icon as icon may not be present
        ICON_CACHE.put(info.getId(), Pair.of(null, new Size2i(0, 0)));

        info.getImageIcon().ifPresent(path ->
        {
            try(InputStream is = Files.newInputStream(path); NativeImage icon = NativeImage.read(is))
            {
                if(icon.getWidth() == icon.getHeight())
                {
                    TextureManager textureManager = this.minecraft.getTextureManager();
                    ResourceLocation location = textureManager.register("catalogueicon", this.createLogoTexture(icon, false));
                    ICON_CACHE.put(info.getId(), Pair.of(location, new Size2i(icon.getWidth(), icon.getHeight())));
                }
            }
            catch(IOException ignored) {}
        });
    }

    private DynamicTexture createLogoTexture(NativeImage image, boolean smooth)
    {
        return new DynamicTexture(image)
        {
            @Override
            public void upload()
            {
                this.bind();
                image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), smooth, false, false, false);
            }
        };
    }

    private class ModList extends AbstractSelectionList<ModEntry>
    {
        public ModList()
        {
            super(CatalogueModListScreen.this.minecraft, 150, CatalogueModListScreen.this.height, 46, CatalogueModListScreen.this.height - 35, 26);
        }

        public int getLeft()
        {
            return this.x0;
        }

        public int getRight()
        {
            return this.x1;
        }

        public int getTop()
        {
            return this.y0;
        }

        public int getBottom()
        {
            return this.y1;
        }

        public int getWidth()
        {
            return this.width;
        }

        @Override
        protected int getScrollbarPosition()
        {
            return this.getLeft() + this.width - 6;
        }

        @Override
        public int getRowLeft()
        {
            return this.getLeft();
        }

        @Override
        public int getRowWidth()
        {
            return this.width;
        }

        public void filterAndUpdateList(String text)
        {
            List<ModEntry> entries = cachedInfo.stream()
                .filter(info -> info.getName().toLowerCase(Locale.ENGLISH).contains(text.toLowerCase(Locale.ENGLISH)))
                .filter(info -> !info.getId().startsWith("fabric") && !info.getId().startsWith("java") || libraryButton.selected())
                .map(info -> new ModEntry(info, this))
                .sorted(SORT)
                .collect(Collectors.toList());
            this.replaceEntries(entries);
            this.setScrollAmount(0);
        }

        @Nullable
        public ModEntry getEntryFromInfo(ModInfo info)
        {
            return this.children().stream().filter(entry -> entry.info == info).findFirst().orElse(null);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
        {
            ScreenUtil.scissor(this.getRowLeft(), this.getTop(), this.getWidth(), this.getBottom() - this.getTop());
            super.render(poseStack, mouseX, mouseY, partialTicks);
            RenderSystem.disableScissor();
        }

        @Override
        public boolean keyPressed(int key, int scanCode, int modifiers)
        {
            if(key == GLFW.GLFW_KEY_ENTER && this.getSelected() != null)
            {
                CatalogueModListScreen.this.setSelectedModInfo(this.getSelected().info);
                SoundManager handler = Minecraft.getInstance().getSoundManager();
                handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            return super.keyPressed(key, scanCode, modifiers);
        }

        @Override
        public void updateNarration(NarrationElementOutput p_169152_)
        {

        }

        @Override
        public void centerScrollOn(ModEntry entry)
        {
            super.centerScrollOn(entry);
        }
    }

    private class ModEntry extends AbstractSelectionList.Entry<ModEntry>
    {
        private final ModInfo info;
        private final ModList list;

        public ModEntry(ModInfo info, ModList list)
        {
            this.info = info;
            this.list = list;
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            // Draws mod name and version
            drawString(poseStack, CatalogueModListScreen.this.font, this.getFormattedModName(), left + 24, top + 2, 0xFFFFFF);
            drawString(poseStack, CatalogueModListScreen.this.font, Component.literal(this.info.getVersion().toString()).withStyle(ChatFormatting.GRAY), left + 24, top + 12, 0xFFFFFF);

            CatalogueModListScreen.this.loadAndCacheIcon(this.info);

            // Draw icon
            if(ICON_CACHE.containsKey(this.info.getId()) && ICON_CACHE.get(this.info.getId()).getLeft() != null)
            {
                ResourceLocation logoResource = TextureManager.INTENTIONAL_MISSING_TEXTURE;
                Size2i size = new Size2i(16, 16);

                Pair<ResourceLocation, Size2i> logoInfo = ICON_CACHE.get(this.info.getId());
                if(logoInfo != null && logoInfo.getLeft() != null)
                {
                    logoResource = logoInfo.getLeft();
                    size = logoInfo.getRight();
                }

                RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
                RenderSystem.setShaderTexture(0, logoResource);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                Screen.blit(poseStack, left + 4, top + 2, 16, 16, 0.0F, 0.0F, size.width, size.height, size.width, size.height);
                RenderSystem.disableBlend();
            }
            else
            {
                // Some items from mods utilise the player or world instance to render. This means
                // rendering the item from the main menu may result in a crash since mods don't check
                // for null pointers. Switches the icon to a grass block if an exception occurs.
                try
                {
                    CatalogueModListScreen.this.minecraft.getItemRenderer().renderGuiItem(this.getItemIcon(), left + 4, top + 2);
                }
                catch(Exception e)
                {
                    ITEM_CACHE.put(this.info.getId(), new ItemStack(Items.GRASS_BLOCK));
                }
            }
        }

        private ItemStack getItemIcon()
        {
            if(ITEM_CACHE.containsKey(this.info.getId()))
            {
                return ITEM_CACHE.get(this.info.getId());
            }

            // Put grass as default item icon
            ITEM_CACHE.put(this.info.getId(), new ItemStack(Items.GRASS_BLOCK));

            // Special case for Forge to set item icon to anvil
            if(this.info.getId().equals("forge"))
            {
                ItemStack item = new ItemStack(Items.ANVIL);
                ITEM_CACHE.put("forge", item);
                return item;
            }

            // Try and get the item icon specified from the mod's metadata
            Optional<String> itemIcon = this.info.getItemIcon();
            if(itemIcon.isPresent())
            {
                try
                {
                    ItemParser.ItemResult result = ItemParser.parseForItem(HolderLookup.forRegistry(Registry.ITEM), new StringReader(itemIcon.get()));
                    ItemStack item = new ItemStack(result.item().value(), 1);
                    item.setTag(result.nbt());
                    ITEM_CACHE.put(this.info.getId(), item);
                    return item;
                }
                catch (CommandSyntaxException ignored) {}
            }

            // If the mod doesn't specify an item to use, Catalogue will attempt to get an item from the mod
            Optional<ItemStack> optional = Registry.ITEM.stream().filter(item -> item.builtInRegistryHolder().key().location().getNamespace().equals(this.info.getId())).map(ItemStack::new).findFirst();
            if(optional.isPresent())
            {
                ItemStack item = optional.get();
                if(item.getItem() != Items.AIR)
                {
                    ITEM_CACHE.put(this.info.getId(), item);
                    return item;
                }
            }

            return new ItemStack(Items.GRASS_BLOCK);
        }

        private Component getFormattedModName()
        {
            String name = this.info.getName();
            int width = this.list.getRowWidth() - (this.list.getMaxScroll() > 0 ? 30 : 24);
            if(CatalogueModListScreen.this.font.width(name) > width)
            {
                name = CatalogueModListScreen.this.font.plainSubstrByWidth(name, width - 10) + "...";
            }
            MutableComponent title = Component.literal(name);
            if(this.info.getId().startsWith("fabric-") || this.info.getId().equals("minecraft") || this.info.getId().equals("java"))
            {
                title.withStyle(ChatFormatting.DARK_GRAY);
            }
            return title;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            CatalogueModListScreen.this.setSelectedModInfo(this.info);
            this.list.setSelected(this);
            return false;
        }

        public ModInfo getInfo()
        {
            return this.info;
        }
    }

    private class StringList extends AbstractSelectionList<StringEntry>
    {
        public StringList(int width, int height, int left, int top)
        {
            super(CatalogueModListScreen.this.minecraft, width, CatalogueModListScreen.this.height, top, top + height, 10);
            this.setLeftPos(left);
        }

        public void setTextFromInfo(ModInfo info)
        {
            this.clearEntries();
            CatalogueModListScreen.this.font.getSplitter().splitLines(info.getDescription().trim(), this.getRowWidth(), Style.EMPTY).forEach(text -> {
                this.addEntry(new StringEntry(text.getString().replace("\n", "").replace("\r", "").trim()));
            });
        }

        @Override
        public void setSelected(@Nullable StringEntry entry) {}

        @Override
        protected int getScrollbarPosition()
        {
            return this.getLeft() + this.width - 7;
        }

        @Override
        public int getRowLeft()
        {
            return this.getLeft();
        }

        @Override
        public int getRowWidth()
        {
            return this.width - 10;
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
        {
            ScreenUtil.scissor(this.getRowLeft(), this.getTop(), this.getWidth(), this.getBottom() - this.getTop());
            super.render(poseStack, mouseX, mouseY, partialTicks);
            RenderSystem.disableScissor();
        }

        @Override
        public void updateNarration(NarrationElementOutput p_169152_)
        {

        }

        public int getLeft()
        {
            return this.x0;
        }

        public int getRight()
        {
            return this.x1;
        }

        public int getTop()
        {
            return this.y0;
        }

        public int getBottom()
        {
            return this.y1;
        }

        public int getWidth()
        {
            return this.width;
        }
    }

    private class StringEntry extends AbstractSelectionList.Entry<StringEntry>
    {
        private final String line;

        public StringEntry(String line)
        {
            this.line = line;
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            drawString(poseStack, CatalogueModListScreen.this.font, this.line, left, top, 0xFFFFFF);
        }
    }

    public static class ModInfo
    {
        private final ModContainer container;
        private final String id;
        private final String name;
        private final String description;
        private final String version;
        private final String imageIcon;
        private final String itemIcon;
        private final String license;
        private final String authors;
        private final String contributors;
        private final String issueLink;
        private final String homepageLink;
        private final Method configFactory;

        public ModInfo(ModContainer container)
        {
            ModMetadata metadata = container.getMetadata();
            this.container = container;
            this.id = metadata.getId();
            this.name = metadata.getName();
            this.description = metadata.getDescription();
            this.version = metadata.getVersion().getFriendlyString();
            this.license = StringUtils.join(metadata.getLicense(), ", ");
            this.authors = StringUtils.join(metadata.getAuthors().stream().map(Person::getName).collect(Collectors.toList()), ", ");
            this.contributors = StringUtils.join(metadata.getContributors().stream().map(Person::getName).collect(Collectors.toList()), ", ");
            this.issueLink = metadata.getContact().get("issues").orElse(null);
            this.homepageLink = metadata.getContact().get("homepage").orElse(null);

            String imageIcon = metadata.getIconPath(64).orElse(null);
            String itemIcon = null;
            Method configFactory = null;
            CustomValue value = metadata.getCustomValue("catalogue");
            if(value != null && value.getType() == CustomValue.CvType.OBJECT)
            {
                CustomValue.CvObject catalogueObj = value.getAsObject();
                CustomValue imageIconValue = catalogueObj.get("imageIcon");
                if(imageIconValue != null && imageIconValue.getType() == CustomValue.CvType.STRING)
                {
                    imageIcon = imageIconValue.getAsString();
                }
                CustomValue itemIconValue = catalogueObj.get("itemIcon");
                if(itemIconValue != null && itemIconValue.getType() == CustomValue.CvType.STRING)
                {
                    itemIcon = itemIconValue.getAsString();
                }
                CustomValue configFactoryValue = catalogueObj.get("configFactory");
                if(configFactoryValue != null && configFactoryValue.getType() == CustomValue.CvType.STRING)
                {
                    configFactory = findConfigFactoryMethod(configFactoryValue.getAsString());
                }
            }
            this.imageIcon = imageIcon;
            this.itemIcon = itemIcon;
            this.configFactory = configFactory;
        }

        public String getId()
        {
            return this.id;
        }

        public String getName()
        {
            return this.name;
        }

        public String getDescription()
        {
            return description;
        }

        public String getVersion()
        {
            return this.version;
        }

        public Optional<Path> getImageIcon()
        {
            return Optional.ofNullable(this.imageIcon).flatMap(this.container::findPath);
        }

        public Optional<String> getItemIcon()
        {
            return Optional.ofNullable(this.itemIcon);
        }

        public String getLicense()
        {
            return this.license;
        }

        public String getAuthors()
        {
            return this.authors;
        }

        public String getContributors()
        {
            return this.contributors;
        }

        public Optional<String> getIssueLink()
        {
            return Optional.ofNullable(this.issueLink);
        }

        public Optional<String> getHomepageLink()
        {
            return Optional.ofNullable(this.homepageLink);
        }

        public Optional<Method> getConfigFactory()
        {
            return Optional.ofNullable(this.configFactory);
        }
    }

    public record Size2i(int width, int height) {}

    private static Method findConfigFactoryMethod(String className)
    {
        try
        {
            Class<?> configFactoryClass = Class.forName(className);
            Method createConfigScreenMethod = configFactoryClass.getDeclaredMethod("createConfigScreen", Screen.class, ModContainer.class);
            int mods = createConfigScreenMethod.getModifiers();
            if(!Modifier.isPublic(mods))
            {
                throw new RuntimeException("createConfigScreen is not accessible for class: " + className);
            }
            if(!Modifier.isStatic(mods))
            {
                throw new RuntimeException("createConfigScreen is not static for class: " + className);
            }
            return createConfigScreenMethod;
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to locate config factory class: " + className);
        }
        catch(NoSuchMethodException e)
        {
            throw new RuntimeException("Missing \"public static createConfigScreen(Screen,ModContainer)\" method for config factory class: " + className);
        }
    }

    private static void openConfigScreen(ModInfo info, Screen currentScreen)
    {
        info.getConfigFactory().ifPresent(method ->
        {
            try
            {
                Object object = method.invoke(null, currentScreen, info.container);
                if(object instanceof Screen configScreen)
                {
                    Minecraft.getInstance().setScreen(configScreen);
                }
            }
            catch(InvocationTargetException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        });
    }
}
