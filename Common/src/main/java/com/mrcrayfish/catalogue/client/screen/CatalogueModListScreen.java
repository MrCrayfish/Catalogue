package com.mrcrayfish.catalogue.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrcrayfish.catalogue.Constants;
import com.mrcrayfish.catalogue.client.ClientHelper;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueCheckBoxButton;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import com.mrcrayfish.catalogue.platform.ClientServices;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.registries.BuiltInRegistries;
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
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class CatalogueModListScreen extends Screen
{
    private static final Comparator<ModListEntry> SORT = Comparator.comparing(o -> o.getData().getDisplayName());
    private static final ResourceLocation MISSING_BANNER = new ResourceLocation(Constants.MOD_ID, "textures/gui/missing_banner.png");
    private static final ResourceLocation VERSION_CHECK_ICONS = new ResourceLocation("forge", "textures/gui/version_check_icons.png");
    private static final Map<String, Pair<ResourceLocation, Dimension>> BANNER_CACHE = new HashMap<>();
    private static final Map<String, Pair<ResourceLocation, Dimension>> IMAGE_ICON_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> ITEM_ICON_CACHE = new HashMap<>();
    private static final Map<String, IModData> CACHED_MODS = new HashMap<>();
    private static ResourceLocation cachedBackground;
    private static boolean loaded = false;

    private final Screen parentScreen;
    private EditBox searchTextField;
    private ModList modList;
    private IModData selectedModData;
    private Button modFolderButton;
    private Button configButton;
    private Button websiteButton;
    private Button issueButton;
    private Checkbox updatesButton;
    private StringList descriptionList;
    private int tooltipYOffset;
    private List<? extends FormattedCharSequence> activeTooltip;

    public CatalogueModListScreen(Screen parent)
    {
        super(CommonComponents.EMPTY);
        this.parentScreen = parent;
        if(!loaded)
        {
            ClientServices.PLATFORM.getAllModData().forEach(data -> CACHED_MODS.put(data.getModId(), data));
            loaded = true;
        }
    }

    @Override
    public void onClose()
    {
        this.minecraft.setScreen(this.parentScreen);
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
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, btn -> {
            this.minecraft.setScreen(null);
        }).pos(10, this.modList.getBottom() + 8).size(127, 20).build());
        this.modFolderButton = this.addRenderableWidget(new CatalogueIconButton(140, this.modList.getBottom() + 8, 0, 0, onPress -> {
            Util.getPlatform().openFile(ClientServices.PLATFORM.getModDirectory());
        }));
        int padding = 10;
        int contentLeft = this.modList.getRight() + 12 + padding;
        int contentWidth = this.width - contentLeft - padding;
        int buttonWidth = (contentWidth - padding) / 3;
        this.configButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft, 105, 10, 0, buttonWidth, Component.translatable("catalogue.gui.config"), onPress ->
        {
            if(this.selectedModData != null)
            {
                this.selectedModData.openConfigScreen(this);
            }
        }));
        this.configButton.visible = false;
        this.websiteButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft + buttonWidth + 5, 105, 20, 0, buttonWidth, Component.literal("Website"), onPress -> {
            this.openLink(this.selectedModData.getHomepage());
        }));
        this.websiteButton.visible = false;
        this.issueButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft + buttonWidth + buttonWidth + 10, 105, 30, 0, buttonWidth, Component.literal("Submit Bug"), onPress -> {
            this.openLink(this.selectedModData.getIssueTracker());
        }));
        this.issueButton.visible = false;
        this.descriptionList = new StringList(contentWidth, this.height - 135 - 55, contentLeft, 130);
        this.descriptionList.setRenderTopAndBottom(false);
        this.descriptionList.setRenderBackground(false);
        this.addWidget(this.descriptionList);

        this.updatesButton = this.addRenderableWidget(new CatalogueCheckBoxButton(this.modList.getRight() - 14, 7, button -> {
            this.modList.filterAndUpdateList(this.searchTextField.getValue());
            this.updateSelectedModList();
        }));

        this.modList.filterAndUpdateList(this.searchTextField.getValue());

        // Resizing window causes all widgets to be recreated, therefore need to update selected info
        if(this.selectedModData != null)
        {
            this.setSelectedModData(this.selectedModData);
            this.updateSelectedModList();
            ModListEntry entry = this.modList.getEntryFromInfo(this.selectedModData);
            if(entry != null)
            {
                this.modList.centerScrollOn(entry);
            }
        }
        this.updateSearchField(this.searchTextField.getValue());
    }

    private void openLink(@Nullable String url)
    {
        if(url != null)
        {
            Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            this.handleComponentClicked(style);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.activeTooltip = null;
        this.renderBackground(poseStack);
        this.drawModList(poseStack, mouseX, mouseY, partialTicks);
        this.drawModInfo(poseStack, mouseX, mouseY, partialTicks);
        super.render(poseStack, mouseX, mouseY, partialTicks);

        Optional<IModData> optional = Optional.ofNullable(CACHED_MODS.get(Constants.MOD_ID));
        optional.ifPresent(this::loadAndCacheLogo);
        Pair<ResourceLocation, Dimension> pair = BANNER_CACHE.get(Constants.MOD_ID);
        if(pair != null && pair.getLeft() != null)
        {
            ResourceLocation textureId = pair.getLeft();
            Dimension size = pair.getRight();
            RenderSystem.setShaderTexture(0, textureId);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Screen.blit(poseStack, 10, 9, 10, 10, 0.0F, 0.0F, size.width, size.height, size.width, size.height);
        }

        if(ClientHelper.isMouseWithin(10, 9, 10, 10, mouseX, mouseY))
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
        ModListEntry selectedEntry = this.modList.getEntryFromInfo(this.selectedModData);
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
            Optional<IModData> optional = CACHED_MODS.values().stream().filter(data -> {
                return data.getDisplayName().toLowerCase(Locale.ENGLISH).startsWith(value.toLowerCase(Locale.ENGLISH));
            }).min(Comparator.comparing(IModData::getDisplayName));
            if(optional.isPresent())
            {
                int length = value.length();
                String displayName = optional.get().getDisplayName();
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
        if(ClientServices.PLATFORM.isForge())
        {
            RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            RenderSystem.setShaderTexture(0, VERSION_CHECK_ICONS);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            blit(poseStack, this.modList.getRight() - 24, 10, 24, 0, 8, 8, 64, 16);
        }

        this.modList.render(poseStack, mouseX, mouseY, partialTicks);
        drawString(poseStack, this.font, ClientServices.COMPONENT.createTitle().withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.WHITE), 70, 10, 0xFFFFFF);
        this.searchTextField.render(poseStack, mouseX, mouseY, partialTicks);

        if(ClientHelper.isMouseWithin(this.modList.getRight() - 14, 7, 14, 14, mouseX, mouseY))
        {
            this.setActiveTooltip(ClientServices.COMPONENT.createFilterUpdates());
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
        int listRight = this.modList.getRight();
        this.vLine(poseStack, listRight + 11, -1, this.height, 0xFF707070);
        fill(poseStack, listRight + 12, 0, this.width, this.height, 0x66000000);
        this.descriptionList.render(poseStack, mouseX, mouseY, partialTicks);

        int contentLeft = listRight + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;

        if(this.selectedModData != null)
        {
            this.drawBackground(poseStack, this.width - contentLeft + 10, listRight + 12, 0);

            // Draw mod logo
            this.drawBanner(poseStack, contentWidth, contentLeft, 10, this.width - (listRight + 12 + 10) - 10, 50);

            // Draw mod name
            poseStack.pushPose();
            poseStack.translate(contentLeft, 70, 0);
            poseStack.scale(2.0F, 2.0F, 2.0F);
            drawString(poseStack, this.font, this.selectedModData.getDisplayName(), 0, 0, 0xFFFFFF);
            poseStack.popPose();

            // Draw version
            Component modId = Component.literal("Mod ID: " + this.selectedModData.getModId()).withStyle(ChatFormatting.DARK_GRAY);
            int modIdWidth = this.font.width(modId);
            drawString(poseStack, this.font, modId, contentLeft + contentWidth - modIdWidth, 92, 0xFFFFFF);

            // Draw version
            this.drawStringWithLabel(poseStack, "catalogue.gui.version", this.selectedModData.getVersion().toString(), contentLeft, 92, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);

            // Draws an icon if there is an update for the mod
            IModData.Update update = this.selectedModData.getUpdate();
            if(ClientServices.PLATFORM.isForge() && update != null && update.url() != null && !update.url().isBlank())
            {
                Component version = ClientServices.COMPONENT.createVersion(this.selectedModData.getVersion());
                int versionWidth = this.font.width(version);
                RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
                RenderSystem.setShaderTexture(0, VERSION_CHECK_ICONS);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                int vOffset = update.animated() && (System.currentTimeMillis() / 800 & 1) == 1 ? 8 : 0;
                Screen.blit(poseStack, contentLeft + versionWidth + 5, 92, update.texOffset() * 8, vOffset, 8, 8, 64, 16);
                if(ClientHelper.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, mouseX, mouseY))
                {
                    Component message = ClientServices.COMPONENT.createFormatted("catalogue.gui.update_available", update.url());
                    this.setActiveTooltip(message);
                }
            }

            int labelOffset = this.height - 20;

            // Draw license
            String license = this.selectedModData.getLicense();
            this.drawStringWithLabel(poseStack, "catalogue.gui.licenses", license, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
            labelOffset -= 15;

            // Draw credits
            String credits = this.selectedModData.getCredits();
            if(credits != null)
            {
                this.drawStringWithLabel(poseStack, ClientServices.COMPONENT.getCreditsKey(), credits, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
                labelOffset -= 15;
            }

            // Draw authors
            String authors = this.selectedModData.getAuthors();
            if(authors != null)
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
        Component formatted = ClientServices.COMPONENT.createFormatted(format, text);
        String rawString = formatted.getString();
        String label = rawString.substring(0, rawString.indexOf(":") + 1);
        String content = rawString.substring(rawString.indexOf(":") + 1);
        if(this.font.width(formatted) > maxWidth)
        {
            content = this.font.plainSubstrByWidth(content, maxWidth - this.font.width(label) - 7) + "...";
            MutableComponent credits = Component.literal(label).withStyle(labelColor);
            credits.append(Component.literal(content).withStyle(contentColor));
            drawString(poseStack, this.font, credits, x, y, 0xFFFFFF);
            if(ClientHelper.isMouseWithin(x, y, maxWidth, 9, mouseX, mouseY)) // Sets the active tool tip if string is too long so users can still read it
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
        if(ClientHelper.isMouseWithin(10, 9, 10, 10, (int) mouseX, (int) mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            this.openLink("https://www.curseforge.com/minecraft/mc-mods/catalogue");
            return true;
        }
        if(ClientServices.PLATFORM.isForge() && this.selectedModData != null)
        {
            int contentLeft = this.modList.getRight() + 12 + 10;
            Component version = ClientServices.COMPONENT.createVersion(this.selectedModData.getVersion());
            int versionWidth = this.font.width(version);
            if(ClientHelper.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, (int) mouseX, (int) mouseY))
            {
                IModData.Update update = this.selectedModData.getUpdate();
                if(update != null && update.url() != null && !update.url().isBlank())
                {
                    Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, update.url()));
                    this.handleComponentClicked(style);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setActiveTooltip(Component content)
    {
        this.activeTooltip = this.font.split(content, Math.min(200, this.width));
        this.tooltipYOffset = 0;
    }

    private void setSelectedModData(IModData data)
    {
        this.selectedModData = data;
        this.loadAndCacheLogo(data);
        this.loadAndCacheBackground(data);
        this.configButton.visible = true;
        this.websiteButton.visible = true;
        this.issueButton.visible = true;
        this.configButton.active = data.hasConfig();
        this.websiteButton.active = data.getHomepage() != null;
        this.issueButton.active = data.getIssueTracker() != null;
        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;
        int labelCount = this.getLabelCount(data);
        this.descriptionList.updateSize(contentWidth, this.height - 135 - 10 - labelCount * 15, 130, this.height - 10 - labelCount * 15);
        this.descriptionList.setLeftPos(contentLeft);
        this.descriptionList.setTextFromInfo(data);
        this.descriptionList.setScrollAmount(0);
    }

    private int getLabelCount(IModData selectedModData)
    {
        int count = 1; //1 by default since license property will always exist
        if(selectedModData.getCredits() != null && !selectedModData.getCredits().isBlank()) count++;
        if(selectedModData.getAuthors() != null && !selectedModData.getAuthors().isBlank()) count++;
        return count;
    }

    private void drawBackground(PoseStack poseStack, int contentWidth, int x, int y)
    {
        if(this.selectedModData == null)
            return;

        if(cachedBackground == null)
            return;

        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, cachedBackground);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        builder.vertex(matrix, x, y, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 0).endVertex();
        builder.vertex(matrix, x, y + 128, 0).color(0.0F, 0.0F, 0.0F, 0.0F).uv(0, 1).endVertex();
        builder.vertex(matrix, x + contentWidth, y + 128, 0).color(0.0F, 0.0F, 0.0F, 0.0F).uv(1, 1).endVertex();
        builder.vertex(matrix, x + contentWidth, y, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(1, 0).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableBlend();
    }

    private void drawBanner(PoseStack poseStack, int contentWidth, int x, int y, int maxWidth, int maxHeight)
    {
        if(this.selectedModData != null)
        {
            ResourceLocation logoResource = MISSING_BANNER;
            Dimension size = new Dimension(600, 120);

            if(BANNER_CACHE.containsKey(this.selectedModData.getModId()))
            {
                Pair<ResourceLocation, Dimension> logoInfo = BANNER_CACHE.get(this.selectedModData.getModId());
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

    private void loadAndCacheLogo(IModData data)
    {
        if(BANNER_CACHE.containsKey(data.getModId()))
            return;

        // Fills an empty logo as logo may not be present
        BANNER_CACHE.put(data.getModId(), Pair.of(null, new Dimension(0, 0)));

        // Attempts to load the real logo
        String banner = data.getBanner();
        if(banner != null && !banner.isEmpty())
        {
            if(ClientServices.PLATFORM.isForge() && (banner.contains("/") || banner.contains("\\")))
            {
                Constants.LOG.warn("Skipped loading logo file from {}. The file name '{}' contained illegal characters '/' or '\\'", data.getDisplayName(), banner);
                return;
            }

            ClientServices.PLATFORM.loadNativeImage(data.getModId(), banner, image ->
            {
                if(image.getWidth() == image.getHeight())
                {
                    TextureManager textureManager = this.minecraft.getTextureManager();
                    BANNER_CACHE.put(data.getModId(), Pair.of(textureManager.register("modlogo", this.createLogoTexture(image, data.isLogoSmooth())), new Dimension(image.getWidth(), image.getHeight())));
                }
            });
        }
    }

    private void loadAndCacheIcon(IModData data)
    {
        if(IMAGE_ICON_CACHE.containsKey(data.getModId()))
            return;

        // Fills an empty icon as icon may not be present
        IMAGE_ICON_CACHE.put(data.getModId(), Pair.of(null, new Dimension(0, 0)));

        // Attempts to load the real icon
        String imageIcon = data.getImageIcon();
        if(imageIcon != null && !imageIcon.isEmpty())
        {
            if(ClientServices.PLATFORM.isForge() && (imageIcon.contains("/") || imageIcon.contains("\\")))
            {
                Constants.LOG.warn("Skipped loading Catalogue icon file from {}. The file name '{}' contained illegal characters '/' or '\\'", data.getDisplayName(), imageIcon);
                return;
            }

            ClientServices.PLATFORM.loadNativeImage(data.getModId(), imageIcon, image -> {
                TextureManager textureManager = this.minecraft.getTextureManager();
                IMAGE_ICON_CACHE.put(data.getModId(), Pair.of(textureManager.register("catalogueicon", this.createLogoTexture(image, false)), new Dimension(image.getWidth(), image.getHeight())));
            });
            return;
        }

        // Attempts to use the logo file if it's a square
        String logoFile = data.getBanner();
        if(logoFile != null && !logoFile.isEmpty())
        {
            if(ClientServices.PLATFORM.isForge() && (logoFile.contains("/") || logoFile.contains("\\")))
            {
                Constants.LOG.warn("Skipped loading logo file from {}. The file name '{}' contained illegal characters '/' or '\\'", data.getDisplayName(), imageIcon);
                return;
            }

            ClientServices.PLATFORM.loadNativeImage(data.getModId(), imageIcon, image ->
            {
                if(image.getWidth() == image.getHeight())
                {
                    TextureManager textureManager = this.minecraft.getTextureManager();
                    String modId = data.getModId();

                    /* The first selected mod will have it's logo cached before the icon, so we
                     * can just use the logo instead of loading the image again. */
                    if(BANNER_CACHE.containsKey(modId))
                    {
                        if(BANNER_CACHE.get(modId).getLeft() != null)
                        {
                            IMAGE_ICON_CACHE.put(modId, BANNER_CACHE.get(modId));
                            return;
                        }
                    }

                    /* Since the icon will be same as the logo, we can cache into both icon and logo cache */
                    DynamicTexture texture = this.createLogoTexture(image, data.isLogoSmooth());
                    Dimension size = new Dimension(image.getWidth(), image.getHeight());
                    ResourceLocation textureId = textureManager.register("catalogueicon", texture);
                    IMAGE_ICON_CACHE.put(modId, Pair.of(textureId, size));
                    BANNER_CACHE.put(modId, Pair.of(textureId, size));
                }
            });
        }
    }

    private void loadAndCacheBackground(IModData data)
    {
        // Deletes the last cached background since they are large images
        if(cachedBackground != null)
        {
            TextureManager textureManager = this.minecraft.getTextureManager();
            textureManager.release(cachedBackground);
        }
        cachedBackground = null;

        String background = data.getBackground();
        if(background != null && !background.isEmpty())
        {
            if(ClientServices.PLATFORM.isForge() && (background.contains("/") || background.contains("\\")))
            {
                Constants.LOG.warn("Skipped loading Catalogue background file from {}. The file name '{}' contained illegal characters '/' or '\\'", data.getDisplayName(), background);
                return;
            }

            ClientServices.PLATFORM.loadNativeImage(data.getModId(), background, image ->
            {
                if(image.getWidth() != 512 || image.getHeight() != 256)
                    return;
                TextureManager textureManager = this.minecraft.getTextureManager();
                cachedBackground = textureManager.register("cataloguebackground", this.createLogoTexture(image, false));
            });
        }
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

    private class ModList extends ObjectSelectionList<ModListEntry>
    {
        public ModList()
        {
            super(CatalogueModListScreen.this.minecraft, 150, CatalogueModListScreen.this.height, 46, CatalogueModListScreen.this.height - 35, 26);
        }

        @Override
        protected int getScrollbarPosition()
        {
            return this.x0 + this.width - 6;
        }

        @Override
        public int getRowLeft()
        {
            return this.x0;
        }

        @Override
        public int getRowWidth()
        {
            return this.width;
        }

        public void filterAndUpdateList(String text)
        {
            Predicate<IModData> filter = ClientServices.PLATFORM.isForge() ?
                    data -> !updatesButton.selected() || data.getUpdate() != null :
                    data -> data.getType() == IModData.Type.DEFAULT || data.getModId().equals("minecraft") || data.getModId().equals("fabric-api") || updatesButton.selected();
            List<ModListEntry> entries = CACHED_MODS.values().stream()
                .filter(info -> info.getDisplayName().toLowerCase(Locale.ENGLISH).contains(text.toLowerCase(Locale.ENGLISH)))
                .filter(filter)
                .map(info -> new ModListEntry(info, this))
                .sorted(SORT)
                .collect(Collectors.toList());
            this.replaceEntries(entries);
            this.setScrollAmount(0);
        }

        @Nullable
        public ModListEntry getEntryFromInfo(IModData data)
        {
            return this.children().stream().filter(entry -> entry.data == data).findFirst().orElse(null);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
        {
            ClientHelper.scissor(this.getRowLeft(), this.getTop(), this.getWidth(), this.getBottom() - this.getTop());
            super.render(poseStack, mouseX, mouseY, partialTicks);
            RenderSystem.disableScissor();
        }

        @Override
        public boolean keyPressed(int key, int scanCode, int modifiers)
        {
            if(key == GLFW.GLFW_KEY_ENTER && this.getSelected() != null)
            {
                CatalogueModListScreen.this.setSelectedModData(this.getSelected().data);
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
        public void centerScrollOn(ModListEntry entry)
        {
            super.centerScrollOn(entry);
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

    private class ModListEntry extends ObjectSelectionList.Entry<ModListEntry>
    {
        private final IModData data;
        private final ModList list;

        public ModListEntry(IModData data, ModList list)
        {
            this.data = data;
            this.list = list;
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            // Draws mod name and version
            drawString(poseStack, CatalogueModListScreen.this.font, this.getFormattedModName(), left + 24, top + 2, 0xFFFFFF);
            drawString(poseStack, CatalogueModListScreen.this.font, Component.literal(this.data.getVersion().toString()).withStyle(ChatFormatting.GRAY), left + 24, top + 12, 0xFFFFFF);

            CatalogueModListScreen.this.loadAndCacheIcon(this.data);

            // Draw icon
            if(IMAGE_ICON_CACHE.containsKey(this.data.getModId()) && IMAGE_ICON_CACHE.get(this.data.getModId()).getLeft() != null)
            {
                ResourceLocation logoResource = TextureManager.INTENTIONAL_MISSING_TEXTURE;
                Dimension size = new Dimension(16, 16);

                Pair<ResourceLocation, Dimension> logoInfo = IMAGE_ICON_CACHE.get(this.data.getModId());
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
                    CatalogueModListScreen.this.minecraft.getItemRenderer().renderGuiItem(poseStack, this.getItemIcon(), left + 4, top + 2);
                }
                catch(Exception e)
                {
                    ITEM_ICON_CACHE.put(this.data.getModId(), new ItemStack(Items.GRASS_BLOCK));
                }
            }

            // Draws an icon if there is an update for the mod
            IModData.Update update = this.data.getUpdate();
            if(ClientServices.PLATFORM.isForge() && update != null)
            {
                RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
                RenderSystem.setShaderTexture(0, VERSION_CHECK_ICONS);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                int vOffset = update.animated() && (System.currentTimeMillis() / 800 & 1) == 1 ? 8 : 0;
                Screen.blit(poseStack, left + rowWidth - 8 - 10, top + 6, update.texOffset() * 8, vOffset, 8, 8, 64, 16);
            }
        }

        private ItemStack getItemIcon()
        {
            if(ITEM_ICON_CACHE.containsKey(this.data.getModId()))
            {
                return ITEM_ICON_CACHE.get(this.data.getModId());
            }

            // Put grass as default item icon
            ITEM_ICON_CACHE.put(this.data.getModId(), new ItemStack(Items.GRASS_BLOCK));

            // Special case for Forge to set item icon to anvil
            if(this.data.getModId().equals("forge"))
            {
                ItemStack item = new ItemStack(Items.ANVIL);
                ITEM_ICON_CACHE.put("forge", item);
                return item;
            }

            String itemIcon = this.data.getItemIcon();
            if(itemIcon != null && !itemIcon.isEmpty())
            {
                try
                {
                    ItemParser.ItemResult result = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), new StringReader(itemIcon));
                    ItemStack item = new ItemStack(result.item().value(), 1);
                    item.setTag(result.nbt());
                    ITEM_ICON_CACHE.put(this.data.getModId(), item);
                    return item;
                }
                catch (CommandSyntaxException ignored) {}
            }

            // If the mod doesn't specify an item to use, Catalogue will attempt to get an item from the mod
            Optional<ItemStack> optional = BuiltInRegistries.ITEM.stream().filter(item -> item.builtInRegistryHolder().key().location().getNamespace().equals(this.data.getModId())).map(ItemStack::new).findFirst();
            if(optional.isPresent())
            {
                ItemStack item = optional.get();
                if(item.getItem() != Items.AIR)
                {
                    ITEM_ICON_CACHE.put(this.data.getModId(), item);
                    return item;
                }
            }

            return new ItemStack(Items.GRASS_BLOCK);
        }

        private Component getFormattedModName()
        {
            String name = this.data.getDisplayName();
            int width = this.list.getRowWidth() - (this.list.getMaxScroll() > 0 ? 30 : 24);
            if(CatalogueModListScreen.this.font.width(name) > width)
            {
                name = CatalogueModListScreen.this.font.plainSubstrByWidth(name, width - 10) + "...";
            }
            MutableComponent title = Component.literal(name);
            if(this.data.getModId().equals("forge") || this.data.getModId().equals("minecraft"))
            {
                title.withStyle(ChatFormatting.DARK_GRAY);
            }
            return title;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            CatalogueModListScreen.this.setSelectedModData(this.data);
            this.list.setSelected(this);
            return false;
        }

        public IModData getData()
        {
            return this.data;
        }

        @Override
        public Component getNarration()
        {
            return Component.literal(this.data.getDisplayName());
        }
    }

    private class StringList extends AbstractSelectionList<StringEntry>
    {
        public StringList(int width, int height, int left, int top)
        {
            super(CatalogueModListScreen.this.minecraft, width, CatalogueModListScreen.this.height, top, top + height, 10);
            this.setLeftPos(left);
        }

        public void setTextFromInfo(IModData data)
        {
            this.clearEntries();
            CatalogueModListScreen.this.font.getSplitter().splitLines(data.getDescription().trim(), this.getRowWidth(), Style.EMPTY).forEach(text -> {
                this.addEntry(new StringEntry(text.getString().replace("\n", "").replace("\r", "").trim()));
            });
        }

        @Override
        public void setSelected(@Nullable StringEntry entry) {}

        @Override
        protected int getScrollbarPosition()
        {
            return this.x0 + this.width - 7;
        }

        @Override
        public int getRowLeft()
        {
            return this.x0;
        }

        @Override
        public int getRowWidth()
        {
            return this.width - 10;
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
        {
            ClientHelper.scissor(this.x0, this.y0, this.width, this.y1 - this.y0);
            super.render(poseStack, mouseX, mouseY, partialTicks);
            RenderSystem.disableScissor();
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {}
    }

    private class StringEntry extends ObjectSelectionList.Entry<StringEntry>
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

        @Override
        public Component getNarration()
        {
            return Component.literal(this.line);
        }
    }
    
    private record Dimension(int width, int height) {}
}
