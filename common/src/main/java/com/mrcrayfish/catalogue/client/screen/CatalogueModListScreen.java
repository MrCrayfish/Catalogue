package com.mrcrayfish.catalogue.client.screen;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mrcrayfish.catalogue.Constants;
import com.mrcrayfish.catalogue.Utils;
import com.mrcrayfish.catalogue.client.Branding;
import com.mrcrayfish.catalogue.client.ClientHelper;
import com.mrcrayfish.catalogue.client.IModData;
import com.mrcrayfish.catalogue.client.ImageInfo;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import com.mrcrayfish.catalogue.client.screen.widget.DropdownMenu;
import com.mrcrayfish.catalogue.platform.ClientServices;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class CatalogueModListScreen extends Screen implements DropdownMenuHandler
{
    private static final Favourites FAVOURITES = new Favourites();
    private static final Comparator<ModListEntry> SORT_ALPHABETICALLY = Comparator.comparing(o -> o.getData().getDisplayName());
    private static final Comparator<ModListEntry> SORT_ALPHABETICALLY_REVERSED = SORT_ALPHABETICALLY.reversed();
    private static final Comparator<ModListEntry> SORT_FAVOURITES_FIRST = Comparator.comparing(ModListEntry::getData, Comparator.comparing(data -> FAVOURITES.has(data.getModId()))).reversed().thenComparing(SORT_ALPHABETICALLY);
    private static final MutableObject<String> OPTION_QUERY = new MutableObject<>("");
    private static final MutableBoolean OPTION_HIDE_LIBRARIES = new MutableBoolean(true);
    private static final MutableBoolean OPTION_CONFIGS_ONLY = new MutableBoolean(false);
    private static final MutableBoolean OPTION_UPDATES_ONLY = new MutableBoolean(false);
    private static final MutableBoolean OPTION_FAVOURITES_ONLY = new MutableBoolean(false);
    private static final MutableObject<Comparator<ModListEntry>> OPTION_SORT = new MutableObject<>(SORT_ALPHABETICALLY);
    private static final ResourceLocation MISSING_BANNER = Utils.resource("textures/gui/missing_banner.png");
    private static final ResourceLocation MISSING_BACKGROUND = Utils.resource("textures/gui/missing_background.png");
    private static final ImageInfo MISSING_BANNER_INFO = new ImageInfo(MISSING_BANNER, 120, 120, () -> {});
    private static final Map<String, ImageInfo> BANNER_CACHE = new HashMap<>();
    private static final Map<String, ImageInfo> IMAGE_ICON_CACHE = new HashMap<>();
    private static final Map<String, Item> ITEM_ICON_CACHE = new HashMap<>();
    private static final Map<String, IModData> CACHED_MODS = new HashMap<>();
    private static final Pattern MOD_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Supplier<Pair<Integer, Integer>> COUNTS = Suppliers.memoize(() -> {
        int[] counts = new int[2];
        CACHED_MODS.forEach((modId, data) -> counts[data.isLibrary() ? 1 : 0]++);
        return Pair.of(counts[0], counts[1]);
    });
    private static final Map<String, SearchFilter> SEARCH_FILTERS = ImmutableMap.<String, SearchFilter>builder()
        .put("dependencies", new SearchFilter((query, data) -> {
            IModData target = CACHED_MODS.get(query.toLowerCase(Locale.ENGLISH));
            return target != null && target.getDependencies().contains(data.getModId());
        }))
        .put("dependents", new SearchFilter((query, data) -> {
            return data.getDependencies().contains(query.toLowerCase(Locale.ENGLISH));
        })).build();
    private static final Style SEARCH_FILTER_KEY = Style.EMPTY.withColor(ChatFormatting.GOLD);
    private static final Style SEARCH_FILTER_VALUE = Style.EMPTY.withColor(ChatFormatting.WHITE);
    private static ImageInfo cachedBackground;
    private static boolean loaded = false;

    private final Screen parentScreen;
    private Button optionsButton;
    private EditBox searchTextField;
    private ModList modList;
    private IModData selectedModData;
    private Button modFolderButton;
    private Button configButton;
    private Button websiteButton;
    private Button issueButton;
    private StringList descriptionList;
    private @Nullable DropdownMenu menu;

    public CatalogueModListScreen(Screen parent)
    {
        super(CommonComponents.EMPTY);
        this.parentScreen = parent;
        if(!loaded)
        {
            ClientServices.PLATFORM.getAllModData().forEach(data -> CACHED_MODS.put(data.getModId(), data));
            CACHED_MODS.put("minecraft", new MinecraftModData()); // Override minecraft
            BANNER_CACHE.put("minecraft", new ImageInfo(LogoRenderer.MINECRAFT_LOGO, 1024, 256, () -> {}));
            FAVOURITES.load();
            loaded = true;
        }
    }

    @Override
    public void setMenu(@Nullable DropdownMenu menu)
    {
        if(this.menu != null && this.menu != menu)
        {
            this.menu.hide();
        }
        this.menu = menu;
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
        this.searchTextField = new EditBox(this.font, 10, 25, 150, 20, CommonComponents.EMPTY) {
            @Override
            public int getInnerWidth() {
                if(this.getValue().startsWith("@")) {
                    return super.getInnerWidth() - 16;
                }
                return super.getInnerWidth();
            }
        };
        this.searchTextField.addFormatter(this::formatQuery);
        this.searchTextField.setMaxLength(128);
        this.searchTextField.setValue(OPTION_QUERY.getValue());
        this.searchTextField.setResponder(s -> {
            if(!OPTION_QUERY.getValue().equals(s)) {
                OPTION_QUERY.setValue(s);
                this.updateSearchFieldSuggestion(s);
                this.modList.filterAndUpdateList();
                this.updateSelectedModList();
            }
        });
        this.addWidget(this.searchTextField);
        this.modList = new ModList();
        this.modList.setX(10);
        this.addWidget(this.modList);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, btn -> {
            this.minecraft.setScreen(this.parentScreen);
        }).pos(10, this.modList.getBottom() + 8).size(127, 20).build());
        this.modFolderButton = this.addRenderableWidget(new CatalogueIconButton(140, this.modList.getBottom() + 8, 0, 0, onPress -> {
            Util.getPlatform().openFile(ClientServices.PLATFORM.getModDirectory());
        }));
        int padding = 10;
        int contentLeft = this.modList.getRight() + 12 + padding;
        int contentWidth = this.width - contentLeft - padding;
        int buttonWidth = (contentWidth - padding) / 3;
        this.configButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft, 105, 10, 0, buttonWidth, Component.translatable("catalogue.gui.config"), onPress -> {
            if(this.selectedModData != null) {
                this.selectedModData.openConfigScreen(this);
            }
        }));
        this.configButton.visible = false;
        this.websiteButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft + buttonWidth + 5, 105, 20, 0, buttonWidth, Component.translatable("catalogue.gui.website"), onPress -> {
            this.openLink(this.selectedModData.getHomepage());
        }));
        this.websiteButton.visible = false;
        this.issueButton = this.addRenderableWidget(new CatalogueIconButton(contentLeft + buttonWidth + buttonWidth + 10, 105, 30, 0, buttonWidth, Component.translatable("catalogue.gui.bug"), onPress -> {
            this.openLink(this.selectedModData.getIssueTracker());
        }));
        this.issueButton.visible = false;
        this.descriptionList = new StringList(contentWidth + padding * 2, 50, contentLeft - padding, 130);
        this.descriptionList.visible = false;
        this.addWidget(this.descriptionList);

        DropdownMenu menu = DropdownMenu.builder(this)
            .setMinItemSize(100, 16)
            .setAlignment(DropdownMenu.Alignment.BELOW_RIGHT)
            .addMenu(Component.translatable("catalogue.gui.filters"), DropdownMenu.builder(this)
                .setMinItemSize(60, 16)
                .setAlignment(DropdownMenu.Alignment.END_TOP)
                .addCheckbox(Component.translatable("catalogue.gui.filters.configs_only"), OPTION_CONFIGS_ONLY, newValue -> {
                    this.modList.filterAndUpdateList();
                    return false;
                })
                .addCheckbox(Component.translatable("catalogue.gui.filters.updates_only"), OPTION_UPDATES_ONLY, newValue -> {
                    this.modList.filterAndUpdateList();
                    return false;
                })
                .addCheckbox(Component.translatable("catalogue.gui.filters.favourites"), OPTION_FAVOURITES_ONLY, newValue -> {
                    this.modList.filterAndUpdateList();
                    return false;
                }))
            .addMenu(Component.translatable("catalogue.gui.sort"), DropdownMenu.builder(this)
                .setMinItemSize(60, 16)
                .setAlignment(DropdownMenu.Alignment.END_TOP)
                .addItem(Component.translatable("catalogue.gui.sort.alphabetically"), () -> {
                    OPTION_SORT.setValue(SORT_ALPHABETICALLY);
                    this.modList.filterAndUpdateList();
                })
                .addItem(Component.translatable("catalogue.gui.sort.alphabetically_reverse"), () -> {
                    OPTION_SORT.setValue(SORT_ALPHABETICALLY_REVERSED);
                    this.modList.filterAndUpdateList();
                })
                .addItem(Component.translatable("catalogue.gui.sort.favourites_first"), () -> {
                    OPTION_SORT.setValue(SORT_FAVOURITES_FIRST);
                    this.modList.filterAndUpdateList();
                }))
            .addCheckbox(Component.translatable("catalogue.gui.hide_libraries"), OPTION_HIDE_LIBRARIES, newValue -> {
                this.modList.filterAndUpdateList();
                return false;
            }).build();

        this.optionsButton = this.addRenderableWidget(new CatalogueIconButton(this.modList.getRight() - 16, 6, 40, 0, 16, 16, btn -> {
            menu.toggle(btn.getRectangle());
        }));

        // Filter the mod list
        this.modList.filterAndUpdateList();

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
        this.updateSearchFieldSuggestion(this.searchTextField.getValue());
    }

    /**
     * Creates a confirmation screen to open a link
     *
     * @param url the url to open
     */
    private void openLink(@Nullable String url)
    {
        if(url != null)
        {
            ConfirmLinkScreen.confirmLinkNow(this, url, false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.drawModList(graphics, mouseX, mouseY, partialTick);
        this.drawModInfo(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        boolean inMenu = this.menu != null;
        super.render(graphics, inMenu ? -1000 : mouseX, inMenu ? -1000 : mouseY, partialTicks);

        if(OPTION_QUERY.getValue().startsWith("@"))
        {
            int iconX = this.searchTextField.getX() + this.searchTextField.getWidth() - 15;
            int iconY = this.searchTextField.getY() + (this.searchTextField.getHeight() - 10) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, CatalogueIconButton.TEXTURE, iconX, iconY, 20, 10, 10, 10, 64, 64);

            if(this.menu == null && ClientHelper.isMouseWithin(iconX, iconY, 10, 10, mouseX, mouseY))
            {
                graphics.setTooltipForNextFrame(Component.translatable("catalogue.gui.advanced_search.info"), mouseX, mouseY);
            }
        }

        Optional<IModData> optional = Optional.ofNullable(CACHED_MODS.get(Constants.MOD_ID));
        optional.ifPresent(this::loadAndCacheLogo);
        ImageInfo bannerInfo = BANNER_CACHE.get(Constants.MOD_ID);
        if(bannerInfo != null)
        {
            graphics.blit(RenderPipelines.GUI_TEXTURED, bannerInfo.resource(), 10, 9, 0, 0, 10, 10, bannerInfo.width(), bannerInfo.height(), bannerInfo.width(), bannerInfo.height());
        }

        if(this.menu != null)
        {
            this.menu.render(graphics, mouseX, mouseY, partialTicks);
        }
        else
        {
            if(ClientHelper.isMouseWithin(10, 9, 10, 10, mouseX, mouseY))
            {
                this.setTooltip(graphics, Component.translatable("catalogue.gui.info"), mouseX, mouseY + 10);
            }

            if(this.optionsButton.isMouseOver(mouseX, mouseY))
            {
                this.setTooltip(graphics, Component.translatable("catalogue.gui.options"), mouseX, mouseY + 10);
            }

            if(this.modFolderButton.isMouseOver(mouseX, mouseY))
            {
                this.setTooltip(graphics, Component.translatable("catalogue.gui.open_mods_folder"), mouseX, mouseY);
            }
        }
    }

    @Override
    public void removed()
    {
        FAVOURITES.save();
    }

    /**
     * Sets the tooltip for the next frame. This method will automatically split the given message
     * into separate lines if it reaches the maximum width.
     *
     * @param graphics a gui graphics instance
     * @param message the message to display in the tooltip
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     */
    private void setTooltip(GuiGraphics graphics, Component message, int mouseX, int mouseY)
    {
        graphics.setTooltipForNextFrame(this.font.split(message, Math.min(200, this.width)), mouseX, mouseY);
    }

    private void updateSelectedModList()
    {
        ModListEntry selectedEntry = this.modList.getEntryFromInfo(this.selectedModData);
        if(selectedEntry != null)
        {
            this.modList.setSelected(selectedEntry);
        }
    }

    private void updateSearchFieldSuggestion(String value)
    {
        if(value.isEmpty())
        {
            this.searchTextField.setSuggestion(Component.translatable("catalogue.gui.search").append(Component.literal("...")).getString());
        }
        else if(value.startsWith("@"))
        {
            // Mark as special search
            int end = value.indexOf(":");
            if(end != -1)
            {
                String type = value.substring(1, end);
                Optional<String> optional = SEARCH_FILTERS.keySet().stream().filter(filter -> {
                    return filter.startsWith(type.toLowerCase(Locale.ENGLISH));
                }).min(Comparator.comparing(String::length));
                if(optional.isPresent())
                {
                    int length = type.length();
                    this.searchTextField.setSuggestion(optional.get().substring(length));
                }
                else
                {
                    this.searchTextField.setSuggestion("");
                }
            }
            else
            {
                this.searchTextField.setSuggestion("");
            }
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
     * @param graphics     the current GuiGraphics instance
     * @param mouseX       the current mouse x position
     * @param mouseY       the current mouse y position
     * @param partialTicks the partial ticks
     */
    private void drawModList(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.modList.render(graphics, mouseX, mouseY, partialTicks);
        this.searchTextField.render(graphics, mouseX, mouseY, partialTicks);

        Component modsLabel = ClientServices.COMPONENT.createTitle().withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.WHITE);
        Component countLabel = Component.literal("(" + CACHED_MODS.size() + ")").withStyle(ChatFormatting.GRAY);
        MutableComponent title = Component.empty().append(modsLabel).append(" ").append(countLabel);
        int titleWidth = this.font.width(title);
        int titleLeft = this.modList.getX() + (this.modList.getWidth() - titleWidth) / 2;
        graphics.drawString(this.font, title, titleLeft, 10, 0xFFFFFFFF);

        int countLabelWidth = this.font.width(countLabel);
        if(ClientHelper.isMouseWithin(titleLeft + titleWidth - countLabelWidth, 10, countLabelWidth, this.font.lineHeight, mouseX, mouseY))
        {
            Pair<Integer, Integer> counts = COUNTS.get();
            List<FormattedCharSequence> lines = List.of(
                Component.translatable("catalogue.gui.mod_count", counts.getLeft()).getVisualOrderText(),
                Component.translatable("catalogue.gui.library_count", counts.getRight()).getVisualOrderText()
            );
            graphics.setTooltipForNextFrame(lines, mouseX, mouseY + 10);
        }
    }

    /**
     * Draws everything considered right of the screen; logo, mod title, description and more.
     *
     * @param graphics     the current GuiGraphics instance
     * @param mouseX       the current mouse x position
     * @param mouseY       the current mouse y position
     * @param partialTicks the partial ticks
     */
    private void drawModInfo(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        int listRight = this.modList.getRight();
        graphics.vLine(listRight + 11, -1, this.height, 0xFF707070);
        graphics.fill(listRight + 12, 0, this.width, this.height, 0x66000000);
        this.descriptionList.render(graphics, mouseX, mouseY, partialTicks);

        int contentLeft = listRight + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;

        if(this.selectedModData != null)
        {
            this.drawBackground(graphics, this.width - contentLeft + 10, listRight + 12, 0);

            // Draw mod logo
            this.drawBanner(graphics, contentWidth, contentLeft, 10, this.width - (listRight + 12 + 10) - 10, 50);

            // Draw mod name
            graphics.pose().pushMatrix();
            graphics.pose().translate(contentLeft, 70);
            graphics.pose().scale(2.0F, 2.0F);
            graphics.drawString(this.font, this.selectedModData.getDisplayName(), 0, 0, 0xFFFFFFFF);
            graphics.pose().popMatrix();

            // Draw version
            Component modId = Component.literal("Mod ID: " + this.selectedModData.getModId()).withStyle(ChatFormatting.DARK_GRAY);
            int modIdWidth = this.font.width(modId);
            graphics.drawString(this.font, modId, contentLeft + contentWidth - modIdWidth, 92, 0xFFFFFFFF);

            // Draw version
            this.drawStringWithLabel(graphics, "catalogue.gui.version", this.selectedModData.getVersion().toString(), contentLeft, 92, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);

            // Draws an icon if there is an update for the mod
            IModData.Update update = this.selectedModData.getUpdate();
            if(update != null && update.url() != null && !update.url().isBlank())
            {
                Component version = ClientServices.COMPONENT.createVersion(this.selectedModData.getVersion());
                int versionWidth = this.font.width(version);
                this.selectedModData.drawUpdateIcon(graphics, update, contentLeft + versionWidth + 5, 92);
                if(ClientHelper.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, mouseX, mouseY))
                {
                    Component message = ClientServices.COMPONENT.createFormatted("catalogue.gui.update_available", update.url());
                    this.setTooltip(graphics, message, mouseX, mouseY);
                }
            }

            // Draw fade from the bottom
            graphics.fillGradient(listRight + 12, this.height - 50, this.width, this.height, 0x00000000, 0x66000000);

            int labelOffset = this.height - 18;

            // Draw license
            String license = this.selectedModData.getLicense();
            if(!license.isBlank())
            {
                this.drawStringWithLabel(graphics, "catalogue.gui.licenses", license, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
                labelOffset -= 15;
            }

            // Draw credits
            String credits = this.selectedModData.getCredits();
            if(credits != null && !credits.isBlank())
            {
                this.drawStringWithLabel(graphics, ClientServices.COMPONENT.getCreditsKey(), credits, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
                labelOffset -= 15;
            }

            // Draw authors
            String authors = this.selectedModData.getAuthors();
            if(authors != null && !authors.isBlank())
            {
                this.drawStringWithLabel(graphics, "catalogue.gui.authors", authors, contentLeft, labelOffset, contentWidth, mouseX, mouseY, ChatFormatting.GRAY, ChatFormatting.WHITE);
            }
        }
        else
        {
            Component message = Component.translatable("catalogue.gui.no_selection").withStyle(ChatFormatting.GRAY);
            graphics.drawCenteredString(this.font, message, contentLeft + contentWidth / 2, this.height / 2 - 5, 0xFFFFFF);
        }
    }

    /**
     * Draws a string and prepends a label. If the formed string and label is longer than the
     * specified max width, it will automatically be trimmed and allows the user to hover the
     * string with their mouse to read the full contents.
     *
     * @param graphics    the current matrix stack
     * @param format      a string to prepend to the content
     * @param text        the string to render
     * @param x           the x position
     * @param y           the y position
     * @param maxWidth    the maximum width the string can render
     * @param mouseX      the current mouse x position
     * @param mouseY      the current mouse u position
     */
    private void drawStringWithLabel(GuiGraphics graphics, String format, String text, int x, int y, int maxWidth, int mouseX, int mouseY, ChatFormatting labelColor, ChatFormatting contentColor)
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
            graphics.drawString(this.font, credits, x, y, 0xFFFFFFFF);
            if(ClientHelper.isMouseWithin(x, y, maxWidth, 9, mouseX, mouseY)) // Sets the active tool tip if string is too long so users can still read it
            {
                this.setTooltip(graphics, Component.literal(text), mouseX, mouseY);
            }
        }
        else
        {
            graphics.drawString(this.font, Component.literal(label).withStyle(labelColor).append(Component.literal(content).withStyle(contentColor)), x, y, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if(this.menu != null)
        {
            if(!this.menu.mouseClicked(event, doubleClick))
            {
                this.setMenu(null);
            }
            return true;
        }
        if(ClientHelper.isMouseWithin(10, 9, 10, 10, (int) event.x(), (int) event.y()) && event.button() == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            this.openLink("https://www.curseforge.com/minecraft/mc-mods/catalogue");
            return true;
        }
        if(this.selectedModData != null)
        {
            int contentLeft = this.modList.getRight() + 12 + 10;
            Component version = ClientServices.COMPONENT.createVersion(this.selectedModData.getVersion());
            int versionWidth = this.font.width(version);
            if(ClientHelper.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, (int) event.x(), (int) event.y()))
            {
                IModData.Update update = this.selectedModData.getUpdate();
                if(update != null && update.url() != null && !update.url().isBlank())
                {
                    this.openLink(update.url());
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if(event.key() == GLFW.GLFW_KEY_F && event.hasControlDown())
        {
            if(!this.searchTextField.isFocused())
            {
                this.setFocused(this.searchTextField);
                this.searchTextField.moveCursorToEnd(false);
                this.searchTextField.setHighlightPos(0);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * Sets the selected mod data. This handles loading the logo and background, updates the states
     * of widgets, like the config button enable state (if the mod has a config), and the description
     * test.
     *
     * @param data the mod data to set as selected
     */
    private void setSelectedModData(IModData data)
    {
        this.selectedModData = data;
        this.loadAndCacheLogo(data);
        this.reloadBackground(data);
        this.configButton.visible = true;
        this.websiteButton.visible = true;
        this.issueButton.visible = true;
        this.configButton.active = data.hasConfig();
        this.websiteButton.active = data.getHomepage() != null;
        this.issueButton.active = data.getIssueTracker() != null;
        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;
        int labelCount = this.getFooterTextElementCount(data);
        this.descriptionList.setWidth(contentWidth);
        this.descriptionList.setHeight(this.height - 135 - labelCount * 15 - 9);
        this.descriptionList.setX(contentLeft);
        this.descriptionList.setTextFromInfo(data);
        this.descriptionList.setScrollAmount(0);
    }

    /**
     * Gets the count of the footer text elements. This is used to corrrectly set the height of
     * the description widget.
     *
     * @param data the mod data
     * @return the count of footer text elements
     */
    private int getFooterTextElementCount(IModData data)
    {
        int count = 1; //1 by default since license property will always exist
        if(data.getCredits() != null && !data.getCredits().isBlank()) count++;
        if(data.getAuthors() != null && !data.getAuthors().isBlank()) count++;
        return count;
    }

    /**
     * Draws the background that is visible when a mod is selected. Backgrounds are programmatically
     * faded out to the bottom of the image.
     *
     * @param graphics a gui graphics instance
     * @param contentWidth the widget of the content area
     * @param contentLeft the x position of the content area
     * @param contentTop the y position of the content area
     */
    private void drawBackground(GuiGraphics graphics, int contentWidth, int contentLeft, int contentTop)
    {
        if(this.selectedModData == null)
            return;

        ResourceLocation textureRef = cachedBackground != null ? cachedBackground.resource() : MISSING_BACKGROUND;
        GuiRenderState state = ClientServices.PLATFORM.getGuiRenderState(graphics);
        GpuTextureView gpuTexture = this.minecraft.getTextureManager().getTexture(textureRef).getTextureView();
        BlitRenderState blit = new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(gpuTexture),
                new Matrix3x2f(graphics.pose()),
                contentLeft, contentTop, contentLeft + contentWidth, contentTop + 128, 0, 1, 0, 1,
                0xFFFFFFFF, null);
        state.submitGuiElement(new BackgroundRenderState(blit));
    }

    private void drawBanner(GuiGraphics graphics, int contentWidth, int x, int y, int maxWidth, int maxHeight)
    {
        if(this.selectedModData != null)
        {
            ImageInfo info = this.getBanner(this.selectedModData.getModId());
            int displayWidth = info.width();
            int displayHeight = info.height();
            if(info.width() > maxWidth)
            {
                displayWidth = maxWidth;
                displayHeight = (displayWidth * info.height()) / info.width();
            }
            if(displayHeight > maxHeight)
            {
                displayHeight = maxHeight;
                displayWidth = (displayHeight * info.width()) / info.height();
            }

            x += (contentWidth - displayWidth) / 2;
            y += (maxHeight - displayHeight) / 2;

            // Fix for minecraft logo
            if(info.resource() == LogoRenderer.MINECRAFT_LOGO)
            {
                y += 8;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, info.resource(), x, y, 0, 0, displayWidth, displayHeight, info.width(), info.height(), info.width(), info.height());
        }
    }

    private ImageInfo getBanner(String modId)
    {
        // Try getting the banner for the mod
        ImageInfo bannerInfo = BANNER_CACHE.get(modId);
        if(bannerInfo != null)
            return bannerInfo;

        // Try using the icon image for the banner
        ImageInfo iconInfo = IMAGE_ICON_CACHE.get(modId);
        if(iconInfo != null)
        {
            // Hack to make icon fill max banner height
            int expandedWidth = iconInfo.width() * 10;
            int expandedHeight = iconInfo.height() * 10;
            return new ImageInfo(iconInfo.resource(), expandedWidth, expandedHeight, iconInfo.unregister());
        }

        // Fallback and just use missing banner
        return MISSING_BANNER_INFO;
    }

    private void loadAndCacheLogo(IModData data)
    {
        if(BANNER_CACHE.containsKey(data.getModId()))
            return;

        // Fills an empty logo as logo may not be present
        BANNER_CACHE.put(data.getModId(), null);

        // Load the banner resource if present
        Branding.BANNER.loadResource(data).ifPresent(info -> {
            BANNER_CACHE.put(data.getModId(), info);
        });
    }

    private void loadAndCacheIcon(IModData data)
    {
        if(IMAGE_ICON_CACHE.containsKey(data.getModId()))
            return;

        // Fills an empty icon as icon may not be present
        IMAGE_ICON_CACHE.put(data.getModId(), null);

        // Load the icon branding
        Branding.ICON.loadResource(data).ifPresentOrElse(info -> {
            IMAGE_ICON_CACHE.put(data.getModId(), info);
        }, () -> {
            // If no icon, try and use the loaded banner if a square
            ImageInfo bannerInfo = BANNER_CACHE.get(data.getModId());
            if(bannerInfo != null) {
                if(bannerInfo.width() == bannerInfo.height()) {
                    IMAGE_ICON_CACHE.put(data.getModId(), bannerInfo);
                }
            } else {
                // Otherwise temporarily load the banner, use if square, otherwise free the resource
                Branding.BANNER.loadResource(data).ifPresent(info -> {
                    if(info.width() == info.height()) {
                        IMAGE_ICON_CACHE.put(data.getModId(), info);
                        BANNER_CACHE.put(data.getModId(), info); // Saves loading later
                    } else {
                        info.unregister().run();
                    }
                });
            }
        });
    }

    private void reloadBackground(IModData data)
    {
        Branding.BACKGROUND.loadResource(data).ifPresentOrElse(info -> {
            cachedBackground = info;
        }, () -> {
            if(cachedBackground != null) {
                cachedBackground.unregister().run();
                cachedBackground = null;
            }
        });
    }

    private class ModList extends ObjectSelectionList<ModListEntry>
    {
        private static final Predicate<IModData> SEARCH_PREDICATE = data -> {
            String query = OPTION_QUERY.getValue();
            if(query.startsWith("@")) {
                return performSearchFilter(query, data);
            }
            return data.getDisplayName()
                .toLowerCase(Locale.ENGLISH)
                .contains(query.toLowerCase(Locale.ENGLISH));
        };
        private static final Predicate<IModData> FILTER_PREDICATE = data -> {
            // We ignore filters when using special query
            String query = OPTION_QUERY.getValue();
            if(query.startsWith("@")) {
                return true;
            }
            if(OPTION_CONFIGS_ONLY.booleanValue() && !data.hasConfig()) {
                return false;
            }
            if(OPTION_UPDATES_ONLY.booleanValue() && data.getUpdate() == null) {
                return false;
            }
            if(OPTION_HIDE_LIBRARIES.booleanValue() && data.isLibrary()) {
                return false;
            }
            if(OPTION_FAVOURITES_ONLY.booleanValue() && !FAVOURITES.has(data.getModId())) {
                return false;
            }
            return true;
        };
        private boolean hideFavourites;

        public ModList()
        {
            super(CatalogueModListScreen.this.minecraft, 150, CatalogueModListScreen.this.height - 35 - 45, 45, 26);
            //this.setRenderBackground(false); TODO what appened
        }

        /*@Override
        protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {}*/

        @Override
        protected int scrollBarX()
        {
            return this.getX() + this.width - 6;
        }

        @Override
        public int getRowLeft()
        {
            return this.getX();
        }

        @Override
        public int getRowRight()
        {
            return this.getRowLeft() + this.getRowWidth();
        }

        @Override
        public int getRowWidth()
        {
            return this.width - (this.scrollbarVisible() ? 6 : 0);
        }

        public void filterAndUpdateList()
        {
            List<ModListEntry> entries = CACHED_MODS.values().stream()
                .filter(SEARCH_PREDICATE)
                .filter(FILTER_PREDICATE)
                .map(info -> new ModListEntry(info, this))
                .sorted(OPTION_SORT.getValue())
                .collect(Collectors.toList());
            this.replaceEntries(entries);
            this.refreshScrollAmount();
        }

        @Override
        public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY)
        {
            if(mouseX > this.getRowLeft() + this.getRowWidth())
                return Optional.empty();
            return super.getChildAt(mouseX, mouseY);
        }

        @Nullable
        public ModListEntry getEntryFromInfo(IModData data)
        {
            return this.children().stream().filter(entry -> entry.data == data).findFirst().orElse(null);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
        {
            super.renderWidget(graphics, mouseX, mouseY, partialTicks);

            if(this.children().isEmpty())
            {
                int left = this.getX() + this.getWidth() / 2;
                int top = this.getY() + (this.getHeight() - CatalogueModListScreen.this.font.lineHeight) / 2;
                graphics.drawCenteredString(CatalogueModListScreen.this.font, Component.translatable("catalogue.gui.no_mods"), left, top, 0xFFFFFFFF);
            }
        }

        @Override
        protected void renderListSeparators(GuiGraphics graphics) {}

        @Override
        protected void renderSelection(GuiGraphics graphics, ModListEntry entry, int outlineColour)
        {
            graphics.fill(entry.getX(), entry.getY(), entry.getX() + entry.getWidth(), entry.getY() + entry.getHeight(), outlineColour);
            graphics.fill(entry.getX() + 1, entry.getY() + 1, entry.getX() + entry.getWidth() - 1, entry.getY() + entry.getHeight() - 1, 0xFF000000);
        }

        @Override
        public boolean keyPressed(KeyEvent event)
        {
            if(event.key() == GLFW.GLFW_KEY_ENTER && this.getSelected() != null)
            {
                CatalogueModListScreen.this.setSelectedModData(this.getSelected().data);
                SoundManager handler = Minecraft.getInstance().getSoundManager();
                handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        protected boolean isValidClickButton(MouseButtonInfo info)
        {
            return info.button() == 0 || info.button() == 1;
        }

        @Override
        public void centerScrollOn(ModListEntry entry)
        {
            super.centerScrollOn(entry);
        }

        @Override
        public void onRelease(MouseButtonEvent event)
        {
            super.onRelease(event);
            this.hideFavourites = false;
        }

        @Override
        public boolean updateScrolling(MouseButtonEvent event)
        {
            boolean scrolling = super.updateScrolling(event);
            this.hideFavourites = scrolling;
            return scrolling;
        }

        public boolean shouldHideFavourites()
        {
            return this.hideFavourites;
        }
    }

    private static boolean performSearchFilter(String query, IModData data)
    {
        if(!query.startsWith("@"))
            return false;

        int end = query.indexOf(":");
        if(end == -1)
            return false;

        String type = query.substring(1, end).toLowerCase(Locale.ENGLISH);
        if(!SEARCH_FILTERS.containsKey(type))
            return false;

        String value = query.substring(end + 1);
        return SEARCH_FILTERS.get(type).predicate().test(value, data);
    }

    private FormattedCharSequence formatQuery(String partial, int displayPos)
    {
        String query = OPTION_QUERY.getValue();
        if(!query.startsWith("@"))
            return FormattedCharSequence.forward(partial, Style.EMPTY);

        int split = query.indexOf(":");
        if(split == -1)
            return FormattedCharSequence.forward(partial, SEARCH_FILTER_KEY);

        if(displayPos > split)
            return FormattedCharSequence.forward(partial, SEARCH_FILTER_VALUE);

        if(displayPos + partial.length() < split)
            return FormattedCharSequence.forward(partial, SEARCH_FILTER_KEY);

        split = partial.indexOf(":");
        if(split == -1)
            return FormattedCharSequence.forward(partial, SEARCH_FILTER_KEY);

        return FormattedCharSequence.composite(
            FormattedCharSequence.forward(partial.substring(0, split + 1), SEARCH_FILTER_KEY),
            FormattedCharSequence.forward(partial.substring(split + 1), SEARCH_FILTER_VALUE)
        );
    }

    private class ModListEntry extends ObjectSelectionList.Entry<ModListEntry>
    {
        private final IModData data;
        private final ModList list;
        private final PinnedButton button;
        private ItemStack icon;

        public ModListEntry(IModData data, ModList list)
        {
            this.data = data;
            this.list = list;
            this.button = new PinnedButton(data.getModId());
            this.icon = new ItemStack(this.getItemIcon());
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            // Draws mod name and version
            boolean inOptionsMenu = CatalogueModListScreen.this.menu != null;
            boolean drawFavouriteIcon = !inOptionsMenu && !this.list.shouldHideFavourites() && ClientHelper.isMouseWithin(this.getX() + this.getWidth() - this.getHeight() - 4, this.getY(), this.getHeight() + 4, this.getHeight(), mouseX, mouseY) || FAVOURITES.has(this.data.getModId());
            graphics.drawString(CatalogueModListScreen.this.font, this.getFormattedModName(drawFavouriteIcon), this.getX() + 24, this.getY() + 4, 0xFFFFFFFF);
            graphics.drawString(CatalogueModListScreen.this.font, Component.literal(this.data.getVersion()).withStyle(ChatFormatting.GRAY), this.getX() + 24, this.getY() + 14, 0xFFFFFFFF);

            // Draw image icon or fallback to item icon
            this.drawIcon(graphics, this.getX(), this.getY());

            // Draws an icon if there is an update for the mod
            IModData.Update update = this.data.getUpdate();
            if(update != null)
            {
                int iconLeft = this.getY() + this.getWidth() - 8 - 9 + (drawFavouriteIcon ? -14 : 0);
                this.data.drawUpdateIcon(graphics, update, iconLeft, this.getY() + 7);
            }

            if(drawFavouriteIcon)
            {
                this.button.setX(this.getX() + this.getWidth() - this.button.getWidth() - 8);
                this.button.setY(this.getY() + (this.getHeight() - this.button.getHeight()) / 2 - 1);
                this.button.render(graphics, mouseX, mouseY, partialTicks);
                if(!inOptionsMenu && this.button.isMouseOver(mouseX, mouseY))
                {
                    Component label = !FAVOURITES.has(this.data.getModId()) ?
                            Component.translatable("catalogue.gui.favourite") :
                            Component.translatable("catalogue.gui.remove_favourite");
                    CatalogueModListScreen.this.setTooltip(graphics, label, mouseX, mouseY);
                }
            }
        }

        private void drawIcon(GuiGraphics graphics, int left, int top)
        {
            CatalogueModListScreen.this.loadAndCacheIcon(this.data);

            ImageInfo iconInfo = IMAGE_ICON_CACHE.get(this.data.getModId());
            if(iconInfo != null)
            {
                graphics.blit(RenderPipelines.GUI_TEXTURED, iconInfo.resource(), left + 4, top + 5, 0, 0, 16, 16, iconInfo.width(), iconInfo.height(), iconInfo.width(), iconInfo.height());
                return;
            }

            try
            {
                graphics.renderFakeItem(this.icon, left + 4, top + 5);
            }
            catch(Exception e)
            {
                // Attempt to catch exceptions when rendering item. Sometime level instance isn't checked for null
                Constants.LOG.debug("Failed to draw icon for mod '{}'", this.data.getModId());
                ITEM_ICON_CACHE.put(this.data.getModId(), Items.GRASS_BLOCK);
                this.icon = new ItemStack(Items.GRASS_BLOCK);
            }
        }

        private Item getItemIcon()
        {
            if(ITEM_ICON_CACHE.containsKey(this.data.getModId()))
            {
                return ITEM_ICON_CACHE.get(this.data.getModId());
            }

            // Put grass as default item icon
            ITEM_ICON_CACHE.put(this.data.getModId(), Items.GRASS_BLOCK);

            // Special case for Forge to set item icon to anvil
            if(this.data.getModId().equals("forge"))
            {
                Item item = Items.ANVIL;
                ITEM_ICON_CACHE.put("forge", item);
                return item;
            }

            String itemIcon = this.data.getItemIcon();
            if(itemIcon != null && !itemIcon.isEmpty())
            {
                ResourceLocation resource = ResourceLocation.tryParse(itemIcon);
                if(resource != null)
                {
                    Item item = BuiltInRegistries.ITEM.getValue(resource);
                    if(item != Items.AIR)
                    {
                        ITEM_ICON_CACHE.put(this.data.getModId(), item);
                        return item;
                    }
                }
            }

            // If the mod doesn't specify an item to use, Catalogue will attempt to get an item from the mod
            Optional<Item> optional = BuiltInRegistries.ITEM.stream().filter(item -> item.builtInRegistryHolder().key().location().getNamespace().equals(this.data.getModId())).findFirst();
            if(optional.isPresent())
            {
                Item item = optional.get();
                if(item != Items.AIR)
                {
                    ITEM_ICON_CACHE.put(this.data.getModId(), item);
                    return item;
                }
            }

            return Items.GRASS_BLOCK;
        }

        private Component getFormattedModName(boolean favouriteIconVisible)
        {
            String name = this.data.getDisplayName();
            int paddingEnd = 4;
            int trimWidth = this.list.getRowWidth() - 24 - paddingEnd;
            IModData.Update update = this.data.getUpdate();
            if(update != null)
            {
                trimWidth -= 12;
            }
            if(favouriteIconVisible)
            {
                trimWidth -= 18;
            }
            if(CatalogueModListScreen.this.font.width(name) > trimWidth)
            {
                name = CatalogueModListScreen.this.font.plainSubstrByWidth(name, trimWidth - 8).trim() + "...";
            }
            MutableComponent title = Component.literal(name);
            if(this.data.isLibrary())
            {
                title.withStyle(ChatFormatting.DARK_GRAY);
            }
            return title;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
        {
            if(this.button.mouseClicked(event, doubleClick))
                return false;

            if(event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            {
                DropdownMenu menu = DropdownMenu.builder(CatalogueModListScreen.this)
                    .setMinItemSize(0, 16)
                    .setAlignment(DropdownMenu.Alignment.BELOW_LEFT)
                    .addItem(Component.translatable("catalogue.gui.show_dependencies"), () -> {
                        String filter = "@dependencies:" + this.data.getModId();
                        CatalogueModListScreen.this.searchTextField.setValue(filter);
                    })
                    .addItem(Component.translatable("catalogue.gui.show_dependents"), () -> {
                        String filter = "@dependents:" + this.data.getModId();
                        CatalogueModListScreen.this.searchTextField.setValue(filter);
                    }).build();
                menu.toggle((int) event.x(), (int) event.y());
                return false;
            }
            else if(event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT)
            {
                CatalogueModListScreen.this.setSelectedModData(this.data);
                this.list.setSelected(this);
                return true;
            }
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

        private class PinnedButton extends AbstractButton
        {
            private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons.png");

            private final String modId;

            public PinnedButton(String modId)
            {
                super(0, 0, 10, 10, CommonComponents.EMPTY);
                this.modId = modId;
            }

            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
            {
                int textureU = FAVOURITES.has(this.modId) ? 10 : 0;
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), textureU, 10, 10, 10, 64, 64);
            }

            @Override
            public void onPress(InputWithModifiers modifiers)
            {
                FAVOURITES.toggle(this.modId);
                ModListEntry.this.list.filterAndUpdateList();
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput output)
            {
                this.defaultButtonNarrationText(output);
            }
        }
    }

    private class StringList extends AbstractSelectionList<StringEntry>
    {
        private String description = "";

        public StringList(int width, int height, int left, int top)
        {
            super(CatalogueModListScreen.this.minecraft, width, height, top, 10);
            this.setX(left);
            this.setY(top);
        }

        public void setTextFromInfo(IModData data)
        {
            this.description = data.getDescription();
            this.clearEntries();
            this.visible = true;
            if(data.getDescription().trim().isBlank())
            {
                this.visible = false;
                return;
            }
            CatalogueModListScreen.this.font.getSplitter().splitLines(data.getDescription().trim(), this.getRowWidth(), Style.EMPTY).forEach(text -> {
                this.addEntry(new StringEntry(text.getString().replace("\n", "").replace("\r", "").trim()));
            });
        }

        /*@Override
        protected void renderHeader(GuiGraphics graphics, int $$1, int $$2) {}*/

        @Override
        public void setSelected(@Nullable StringEntry entry) {}

        @Override
        protected int scrollBarX()
        {
            return this.getX() + this.width - 7;
        }

        @Override
        public int getRowLeft()
        {
            return this.getX() + 8;
        }

        @Override
        public int getRowWidth()
        {
            return this.width - 16;
        }

        @Override
        protected int contentHeight()
        {
            return this.getItemCount() * this.defaultEntryHeight + 8;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
        {
            graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
            super.renderWidget(graphics, mouseX, mouseY, partialTicks);
            graphics.disableScissor();
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics)
        {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            graphics.fill(x, y + 1, x + 1, y + height - 1, 0x77000000);
            graphics.fill(x + 1, y, x + width - 1, y + height, 0x77000000);
            graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, 0x77000000);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output)
        {
            output.add(NarratedElementType.TITLE, Component.literal(this.description));
        }
    }

    private class StringEntry extends ObjectSelectionList.Entry<StringEntry>
    {
        private final String line;

        public StringEntry(String line)
        {
            this.line = line;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            graphics.drawString(CatalogueModListScreen.this.font, this.line, this.getX(), this.getY(), 0xFFFFFFFF);
        }

        @Override
        public Component getNarration()
        {
            return Component.literal(this.line);
        }

        @Override
        public void setY(int y)
        {
            // Hacky but cannot override AbstractSelectionList#getFirstEntryY since its private
            super.setY(y + 4);
        }
    }

    private record SearchFilter(BiPredicate<String, IModData> predicate) {}

    private static class Favourites
    {
        private final Set<String> mods = new HashSet<>();
        private boolean needsSave;
        private Path file;

        public void toggle(String modId)
        {
            if(!this.mods.remove(modId))
            {
                this.mods.add(modId);
            }
            this.needsSave = true;
        }

        public boolean has(String modId)
        {
            return this.mods.contains(modId);
        }

        private void init()
        {
            try
            {
                Path configDir = ClientServices.PLATFORM.getConfigDirectory();
                Path file = configDir.resolve("catalogue_favourites.txt");
                if(!Files.exists(file))
                {
                    Files.createFile(file);
                }
                this.file = file;
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        private void load()
        {
            try
            {
                this.init();
                this.mods.clear();
                Predicate<String> modIdRegex = MOD_ID_PATTERN.asMatchPredicate();
                Files.readAllLines(file).forEach(s -> {
                    if(modIdRegex.test(s) && ClientServices.PLATFORM.isModLoaded(s)) {
                        this.mods.add(s);
                    }
                });
                // Save immediately to remove invalid lines
                this.needsSave = true;
                this.save();
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        private void save()
        {
            if(!this.needsSave)
                return;

            try
            {
                this.needsSave = false;
                this.init();
                Files.write(this.file, this.mods, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
