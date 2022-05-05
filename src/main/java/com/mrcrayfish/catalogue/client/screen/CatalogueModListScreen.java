package com.mrcrayfish.catalogue.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrcrayfish.catalogue.Catalogue;
import com.mrcrayfish.catalogue.client.ScreenUtil;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueCheckBoxButton;
import com.mrcrayfish.catalogue.client.screen.widget.CatalogueIconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.util.Size2i;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.client.ConfigGuiHandler;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.packs.ModFileResourcePack;
import net.minecraftforge.fml.packs.ResourcePackLoader;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
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
    private static final Comparator<ModEntry> SORT = Comparator.comparing(o -> o.getInfo().getDisplayName());
    private static final ResourceLocation MISSING_BANNER = new ResourceLocation("catalogue", "textures/gui/missing_banner.png");
    private static final ResourceLocation VERSION_CHECK_ICONS = new ResourceLocation(ForgeVersion.MOD_ID, "textures/gui/version_check_icons.png");
    private static final Map<String, Pair<ResourceLocation, Size2i>> LOGO_CACHE = new HashMap<>();
    private static final Map<String, Pair<ResourceLocation, Size2i>> ICON_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> ITEM_CACHE = new HashMap<>();

    private TextFieldWidget searchTextField;
    private ModList modList;
    private IModInfo selectedModInfo;
    private Button modFolderButton;
    private Button configButton;
    private Button websiteButton;
    private Button issueButton;
    private CheckboxButton updatesButton;
    private StringList descriptionList;
    private int tooltipYOffset;
    private List<IReorderingProcessor> activeTooltip;

    public CatalogueModListScreen()
    {
        super(StringTextComponent.EMPTY);
    }

    @Override
    protected void init()
    {
        super.init();
        this.searchTextField = new TextFieldWidget(this.font, 11, 25, 148, 20, StringTextComponent.EMPTY);
        this.searchTextField.setResponder(s -> {
            this.updateSearchField(s);
            this.modList.filterAndUpdateList(s);
            this.updateSelectedModList();
        });
        this.children.add(this.searchTextField);
        this.modList = new ModList();
        this.modList.setLeftPos(10);
        this.modList.setRenderTopAndBottom(false);
        this.children.add(this.modList);
        this.addButton(new Button(10, this.modList.getBottom() + 8, 127, 20, DialogTexts.GUI_BACK, onPress -> {
            this.getMinecraft().setScreen(null);
        }));
        this.modFolderButton = this.addButton(new CatalogueIconButton(140, this.modList.getBottom() + 8, 0, 0, onPress -> {
            Util.getPlatform().openFile(FMLPaths.MODSDIR.get().toFile());
        }));
        int padding = 10;
        int contentLeft = this.modList.getRight() + 12 + padding;
        int contentWidth = this.width - contentLeft - padding;
        int buttonWidth = (contentWidth - padding) / 3;
        this.configButton = this.addButton(new CatalogueIconButton(contentLeft, 105, 10, 0, buttonWidth, new TranslationTextComponent("fml.menu.mods.config"), onPress ->
        {
            if(this.selectedModInfo != null)
            {
                ConfigGuiHandler.getGuiFactoryFor((ModInfo) this.selectedModInfo).map(f -> f.apply(this.minecraft, this)).ifPresent(newScreen -> this.getMinecraft().setScreen(newScreen));
            }
        }));
        this.configButton.visible = false;
        this.websiteButton = this.addButton(new CatalogueIconButton(contentLeft + buttonWidth + 5, 105, 20, 0, buttonWidth, new StringTextComponent("Website"), onPress -> {
            this.openLink("displayURL", (IConfigurable) this.selectedModInfo);
        }));
        this.websiteButton.visible = false;
        this.issueButton = this.addButton(new CatalogueIconButton(contentLeft + buttonWidth + buttonWidth + 10, 105, 30, 0, buttonWidth, new StringTextComponent("Submit Bug"), onPress -> {
            this.openLink("issueTrackerURL", this.selectedModInfo != null ? ((ModFileInfo) this.selectedModInfo.getOwningFile()) : null);
        }));
        this.issueButton.visible = false;
        this.descriptionList = new StringList(contentWidth, this.height - 135 - 55, contentLeft, 130);
        this.descriptionList.setRenderTopAndBottom(false);
        this.descriptionList.setRenderBackground(false);
        this.children.add(this.descriptionList);

        this.updatesButton = this.addButton(new CatalogueCheckBoxButton(this.modList.getRight() - 14, 7, button -> {
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

    /**
     * Opens a link with a url defined in the mod's info
     *
     * @param key the key of the config element
     */
    private void openLink(String key, @Nullable IConfigurable configurable)
    {
        if(configurable != null)
        {
            configurable.getConfigElement(key).ifPresent(o ->
            {
                this.openLink(o.toString());
            });
        }
    }

    private void openLink(String url)
    {
        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        this.handleComponentClicked(style);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.activeTooltip = null;
        this.renderBackground(matrixStack);
        this.drawModList(matrixStack, mouseX, mouseY, partialTicks);
        this.drawModInfo(matrixStack, mouseX, mouseY, partialTicks);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        Optional<? extends ModContainer> optional = net.minecraftforge.fml.ModList.get().getModContainerById("catalogue");
        optional.ifPresent(container -> this.loadAndCacheLogo(container.getModInfo()));
        Pair<ResourceLocation, Size2i> pair = LOGO_CACHE.get("catalogue");
        if(pair != null && pair.getLeft() != null)
        {
            ResourceLocation textureId = pair.getLeft();
            Size2i size = pair.getRight();
            Minecraft.getInstance().getTextureManager().bind(textureId);
            AbstractGui.blit(matrixStack, 10, 9, 10, 10, 0.0F, 0.0F, size.width, size.height, size.width, size.height);
        }

        if(ScreenUtil.isMouseWithin(10, 9, 10, 10, mouseX, mouseY))
        {
            this.setActiveTooltip(new TranslationTextComponent("catalogue.gui.info").getString());
            this.tooltipYOffset = 10;
        }

        if(this.modFolderButton.isMouseOver(mouseX, mouseY))
        {
            this.setActiveTooltip(new TranslationTextComponent("fml.button.open.mods.folder").getString());
        }

        if(this.activeTooltip != null)
        {
            this.renderToolTip(matrixStack, this.activeTooltip, mouseX, mouseY + this.tooltipYOffset, this.font);
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
            this.searchTextField.setSuggestion(new TranslationTextComponent("fml.menu.mods.search").append(new StringTextComponent("...")).getString());
        }
        else
        {
            Optional<ModInfo> optional = net.minecraftforge.fml.ModList.get().getMods().stream().filter(info -> {
                return info.getDisplayName().toLowerCase(Locale.ENGLISH).startsWith(value.toLowerCase(Locale.ENGLISH));
            }).min(Comparator.comparing(ModInfo::getDisplayName));
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
     * @param matrixStack  the current matrix stack
     * @param mouseX       the current mouse x position
     * @param mouseY       the current mouse y position
     * @param partialTicks the partial ticks
     */
    private void drawModList(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bind(VERSION_CHECK_ICONS);
        blit(matrixStack, this.modList.getRight() - 24, 10, 24, 0, 8, 8, 64, 16);

        this.modList.render(matrixStack, mouseX, mouseY, partialTicks);
        drawString(matrixStack, this.font, new StringTextComponent(ForgeI18n.parseMessage("fml.menu.mods.title")).withStyle(TextFormatting.BOLD).withStyle(TextFormatting.WHITE), 70, 10, 0xFFFFFF);
        this.searchTextField.render(matrixStack, mouseX, mouseY, partialTicks);

        if(ScreenUtil.isMouseWithin(this.modList.getRight() - 14, 7, 14, 14, mouseX, mouseY))
        {
            this.setActiveTooltip(I18n.get("fml.menu.mods.filter_updates"));
            this.tooltipYOffset = 10;
        }
    }

    /**
     * Draws everything considered right of the screen; logo, mod title, description and more.
     *
     * @param matrixStack  the current matrix stack
     * @param mouseX       the current mouse x position
     * @param mouseY       the current mouse y position
     * @param partialTicks the partial ticks
     */
    private void drawModInfo(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.vLine(matrixStack, this.modList.getRight() + 11, -1, this.height, 0xFF707070);
        fill(matrixStack, this.modList.getRight() + 12, 0, this.width, this.height, 0x66000000);
        this.descriptionList.render(matrixStack, mouseX, mouseY, partialTicks);

        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;

        if(this.selectedModInfo != null)
        {
            // Draw mod logo
            this.drawLogo(matrixStack, contentWidth, contentLeft, 10, this.width - (this.modList.getRight() + 12 + 10) - 10, 50);

            // Draw mod name
            matrixStack.pushPose();
            matrixStack.translate(contentLeft, 70, 0);
            matrixStack.scale(2.0F, 2.0F, 2.0F);
            drawString(matrixStack, this.font, this.selectedModInfo.getDisplayName(), 0, 0, 0xFFFFFF);
            matrixStack.popPose();

            // Draw version
            ITextComponent modId = new StringTextComponent("Mod ID: " + this.selectedModInfo.getModId()).withStyle(TextFormatting.DARK_GRAY);
            int modIdWidth = this.font.width(modId);
            drawString(matrixStack, this.font, modId, contentLeft + contentWidth - modIdWidth, 92, 0xFFFFFF);

            // Set tooltip for secure mod features forge has
            if(ScreenUtil.isMouseWithin(contentLeft + contentWidth - modIdWidth, 92, modIdWidth, this.font.lineHeight, mouseX, mouseY))
            {
                if(FMLEnvironment.secureJarsEnabled)
                {
                    this.setActiveTooltip(ForgeI18n.parseMessage("fml.menu.mods.info.signature", ((ModInfo) this.selectedModInfo).getOwningFile().getCodeSigningFingerprint().orElse(ForgeI18n.parseMessage("fml.menu.mods.info.signature.unsigned"))));
                    this.setActiveTooltip(ForgeI18n.parseMessage("fml.menu.mods.info.trust", ((ModInfo) this.selectedModInfo).getOwningFile().getTrustData().orElse(ForgeI18n.parseMessage("fml.menu.mods.info.trust.noauthority"))));
                }
                else
                {
                    this.setActiveTooltip(ForgeI18n.parseMessage("fml.menu.mods.info.securejardisabled"));
                }
            }

            // Draw version
            this.drawStringWithLabel(matrixStack, "fml.menu.mods.info.version", this.selectedModInfo.getVersion().toString(), contentLeft, 92, contentWidth, mouseX, mouseY, TextFormatting.GRAY, TextFormatting.WHITE);

            // Draws an icon if there is an update for the mod
            VersionChecker.CheckResult result = VersionChecker.getResult(this.selectedModInfo);
            if(result.status.shouldDraw() && result.url != null)
            {
                String version = ForgeI18n.parseMessage("fml.menu.mods.info.version", this.selectedModInfo.getVersion().toString());
                int versionWidth = this.font.width(version);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                Minecraft.getInstance().getTextureManager().bind(VERSION_CHECK_ICONS);
                int vOffset = result.status.isAnimated() && (System.currentTimeMillis() / 800 & 1) == 1 ? 8 : 0;
                AbstractGui.blit(matrixStack, contentLeft + versionWidth + 5, 92, result.status.getSheetOffset() * 8, vOffset, 8, 8, 64, 16);
                if(ScreenUtil.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, mouseX, mouseY))
                {
                    this.setActiveTooltip(ForgeI18n.parseMessage("fml.menu.mods.info.updateavailable", result.url));
                }
            }

            int labelOffset = this.height - 20;

            // Draw license
            String license = this.selectedModInfo.getOwningFile().getLicense();
            this.drawStringWithLabel(matrixStack, "fml.menu.mods.info.license", license, contentLeft, labelOffset, contentWidth, mouseX, mouseY, TextFormatting.GRAY, TextFormatting.WHITE);
            labelOffset -= 15;

            // Draw credits
            Optional<Object> credits = ((ModInfo) this.selectedModInfo).getConfigElement("credits");
            if(credits.isPresent())
            {
                this.drawStringWithLabel(matrixStack, "fml.menu.mods.info.credits", credits.get().toString(), contentLeft, labelOffset, contentWidth, mouseX, mouseY, TextFormatting.GRAY, TextFormatting.WHITE);
                labelOffset -= 15;
            }

            // Draw authors
            Optional<Object> authors = ((ModInfo) this.selectedModInfo).getConfigElement("authors");
            if(authors.isPresent())
            {
                this.drawStringWithLabel(matrixStack, "fml.menu.mods.info.authors", authors.get().toString(), contentLeft, labelOffset, contentWidth, mouseX, mouseY, TextFormatting.GRAY, TextFormatting.WHITE);
            }
        }
        else
        {
            ITextComponent message = new TranslationTextComponent("catalogue.gui.no_selection").withStyle(TextFormatting.GRAY);
            drawCenteredString(matrixStack, this.font, message, contentLeft + contentWidth / 2, this.height / 2 - 5, 0xFFFFFF);
        }
    }

    /**
     * Draws a string and prepends a label. If the formed string and label is longer than the
     * specified max width, it will automatically be trimmed and allows the user to hover the
     * string with their mouse to read the full contents.
     *
     * @param matrixStack the current matrix stack
     * @param format      a string to prepend to the content
     * @param text        the string to render
     * @param x           the x position
     * @param y           the y position
     * @param maxWidth    the maximum width the string can render
     * @param mouseX      the current mouse x position
     * @param mouseY      the current mouse u position
     */
    private void drawStringWithLabel(MatrixStack matrixStack, String format, String text, int x, int y, int maxWidth, int mouseX, int mouseY, TextFormatting labelColor, TextFormatting contentColor)
    {
        String formatted = ForgeI18n.parseMessage(format, text); // Attempting to keep Forge's lang since it's already support many languages
        String label = formatted.substring(0, formatted.indexOf(":") + 1);
        String content = formatted.substring(formatted.indexOf(":") + 1);
        if(this.font.width(formatted) > maxWidth)
        {
            content = this.font.plainSubstrByWidth(content, maxWidth - this.font.width(label) - 7) + "...";
            IFormattableTextComponent credits = new StringTextComponent(label).withStyle(labelColor);
            credits.append(new StringTextComponent(content).withStyle(contentColor));
            drawString(matrixStack, this.font, credits, x, y, 0xFFFFFF);
            if(ScreenUtil.isMouseWithin(x, y, maxWidth, 9, mouseX, mouseY)) // Sets the active tool tip if string is too long so users can still read it
            {
                this.setActiveTooltip(text);
            }
        }
        else
        {
            drawString(matrixStack, this.font, new StringTextComponent(label).withStyle(labelColor).append(new StringTextComponent(content).withStyle(contentColor)), x, y, 0xFFFFFF);
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
        if(this.selectedModInfo != null)
        {
            int contentLeft = this.modList.getRight() + 12 + 10;
            String version = ForgeI18n.parseMessage("fml.menu.mods.info.version", this.selectedModInfo.getVersion().toString());
            int versionWidth = this.font.width(version);
            if(ScreenUtil.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, (int) mouseX, (int) mouseY))
            {
                VersionChecker.CheckResult result = VersionChecker.getResult(this.selectedModInfo);
                if(result.status.shouldDraw() && result.url != null)
                {
                    Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result.url));
                    this.handleComponentClicked(style);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setActiveTooltip(String content)
    {
        this.activeTooltip = this.font.split(new StringTextComponent(content), Math.min(200, this.width));
        this.tooltipYOffset = 0;
    }

    private void setSelectedModInfo(IModInfo selectedModInfo)
    {
        this.selectedModInfo = selectedModInfo;
        this.loadAndCacheLogo(selectedModInfo);
        this.configButton.visible = true;
        this.websiteButton.visible = true;
        this.issueButton.visible = true;
        this.configButton.active = ConfigGuiHandler.getGuiFactoryFor((ModInfo) selectedModInfo).isPresent();
        this.websiteButton.active = ((ModInfo) selectedModInfo).getConfigElement("displayURL").isPresent();
        this.issueButton.active = ((ModInfo) selectedModInfo).getOwningFile().getConfigElement("issueTrackerURL").isPresent();
        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.width - contentLeft - 10;
        int labelCount = this.getLabelCount(selectedModInfo);
        this.descriptionList.updateSize(contentWidth, this.height - 135 - 10 - labelCount * 15, 130, this.height - 10 - labelCount * 15);
        this.descriptionList.setLeftPos(contentLeft);
        this.descriptionList.setTextFromInfo(selectedModInfo);
        this.descriptionList.setScrollAmount(0);
    }

    private int getLabelCount(IModInfo selectedModInfo)
    {
        int count = 1; //1 by default since license property will always exist
        if(((ModInfo) selectedModInfo).getConfigElement("credits").isPresent()) count++;
        if(((ModInfo) selectedModInfo).getConfigElement("authors").isPresent()) count++;
        return count;
    }

    private void drawLogo(MatrixStack matrixStack, int contentWidth, int x, int y, int maxWidth, int maxHeight)
    {
        if(this.selectedModInfo != null)
        {
            ResourceLocation logoResource = MISSING_BANNER;
            Size2i size = new Size2i(600, 120);

            if(LOGO_CACHE.containsKey(this.selectedModInfo.getModId()))
            {
                Pair<ResourceLocation, Size2i> logoInfo = LOGO_CACHE.get(this.selectedModInfo.getModId());
                if(logoInfo.getLeft() != null)
                {
                    logoResource = logoInfo.getLeft();
                    size = logoInfo.getRight();
                }
            }

            Minecraft.getInstance().getTextureManager().bind(logoResource);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
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

            AbstractGui.blit(matrixStack, x, y, width, height, 0.0F, 0.0F, size.width, size.height, size.width, size.height);

            RenderSystem.disableBlend();
        }
    }

    private void loadAndCacheLogo(IModInfo info)
    {
        if(LOGO_CACHE.containsKey(info.getModId()))
            return;

        // Fills an empty logo as logo may not be present
        LOGO_CACHE.put(info.getModId(), Pair.of(null, new Size2i(0, 0)));

        // Attempts to load the real logo
        ModInfo modInfo = (ModInfo) info;
        modInfo.getLogoFile().ifPresent(s ->
        {
            if(s.isEmpty())
                return;

            if(s.contains("/") || s.contains("\\"))
            {
                Catalogue.LOGGER.warn("Skipped loading logo file from {}. The file name '{}' contained illegal characters '/' or '\\'", info.getDisplayName(), s);
                return;
            }

            ModFileResourcePack resourcePack = ResourcePackLoader.getResourcePackFor(info.getModId()).orElse(ResourcePackLoader.getResourcePackFor("forge").orElseThrow(() -> new RuntimeException("Can't find forge, WHAT!")));
            try(InputStream is = resourcePack.getRootResource(s); NativeImage logo = NativeImage.read(is))
            {
                TextureManager textureManager = this.getMinecraft().getTextureManager();
                LOGO_CACHE.put(info.getModId(), Pair.of(textureManager.register("modlogo", this.createLogoTexture(logo, modInfo.getLogoBlur())), new Size2i(logo.getWidth(), logo.getHeight())));
            }
            catch(IOException ignored) {}
        });
    }

    private void loadAndCacheIcon(IModInfo info)
    {
        if(ICON_CACHE.containsKey(info.getModId()))
            return;

        // Fills an empty icon as icon may not be present
        ICON_CACHE.put(info.getModId(), Pair.of(null, new Size2i(0, 0)));

        // Attempts to load the real icon
        ModInfo modInfo = (ModInfo) info;
        if(modInfo.getModProperties().containsKey("catalogueImageIcon"))
        {
            String s = (String) modInfo.getModProperties().get("catalogueImageIcon");

            if(s.isEmpty())
                return;

            if(s.contains("/") || s.contains("\\"))
            {
                Catalogue.LOGGER.warn("Skipped loading Catalogue icon file from {}. The file name '{}' contained illegal characters '/' or '\\'", info.getDisplayName(), s);
                return;
            }

            ModFileResourcePack resourcePack = ResourcePackLoader.getResourcePackFor(info.getModId()).orElse(ResourcePackLoader.getResourcePackFor("forge").orElseThrow(() -> new RuntimeException("Can't find forge, WHAT!")));
            try(InputStream is = resourcePack.getRootResource(s); NativeImage icon = NativeImage.read(is))
            {
                TextureManager textureManager = this.getMinecraft().getTextureManager();
                ICON_CACHE.put(info.getModId(), Pair.of(textureManager.register("catalogueicon", this.createLogoTexture(icon, false)), new Size2i(icon.getWidth(), icon.getHeight())));
                return;
            }
            catch(IOException ignored) {}
        }

        // Attempts to use the logo file if it's a square
        modInfo.getLogoFile().ifPresent(s ->
        {
            if(s.isEmpty())
                return;

            if(s.contains("/") || s.contains("\\"))
            {
                Catalogue.LOGGER.warn("Skipped loading logo file from {}. The file name '{}' contained illegal characters '/' or '\\'", info.getDisplayName(), s);
                return;
            }

            ModFileResourcePack resourcePack = ResourcePackLoader.getResourcePackFor(info.getModId()).orElse(ResourcePackLoader.getResourcePackFor("forge").orElseThrow(() -> new RuntimeException("Can't find forge, WHAT!")));
            try(InputStream is = resourcePack.getRootResource(s); NativeImage logo = NativeImage.read(is))
            {
                if(logo.getWidth() == logo.getHeight())
                {
                    TextureManager textureManager = this.getMinecraft().getTextureManager();
                    String modId = info.getModId();

                    /* The first selected mod will have it's logo cached before the icon, so we
                     * can just use the logo instead of loading the image again. */
                    if(LOGO_CACHE.containsKey(modId))
                    {
                        if(LOGO_CACHE.get(modId).getLeft() != null)
                        {
                            ICON_CACHE.put(modId, LOGO_CACHE.get(modId));
                            return;
                        }
                    }

                    /* Since the icon will be same as the logo, we can cache into both icon and logo cache */
                    DynamicTexture texture = this.createLogoTexture(logo, modInfo.getLogoBlur());
                    Size2i size = new Size2i(logo.getWidth(), logo.getHeight());
                    ResourceLocation textureId = textureManager.register("catalogueicon", texture);
                    ICON_CACHE.put(modId, Pair.of(textureId, size));
                    LOGO_CACHE.put(modId, Pair.of(textureId, size));
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

    private class ModList extends ExtendedList<ModEntry>
    {
        public ModList()
        {
            super(CatalogueModListScreen.this.minecraft, 150, CatalogueModListScreen.this.height, 46, CatalogueModListScreen.this.height - 35, 26);
        }

        @Override
        protected int getScrollbarPosition()
        {
            return super.getLeft() + this.width - 6;
        }

        @Override
        public int getRowLeft()
        {
            return super.getLeft();
        }

        @Override
        public int getRowWidth()
        {
            return this.width;
        }

        public void filterAndUpdateList(String text)
        {
            List<ModEntry> entries = net.minecraftforge.fml.ModList.get().getMods().stream()
                .filter(info -> info.getDisplayName().toLowerCase(Locale.ENGLISH).contains(text.toLowerCase(Locale.ENGLISH)))
                .filter(info -> !updatesButton.selected() || VersionChecker.getResult(info).status.shouldDraw())
                .map(info -> new ModEntry(info, this))
                .sorted(SORT)
                .collect(Collectors.toList());
            this.replaceEntries(entries);
            this.setScrollAmount(0);
        }

        @Nullable
        public ModEntry getEntryFromInfo(IModInfo info)
        {
            return this.children().stream().filter(entry -> entry.info == info).findFirst().orElse(null);
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            ScreenUtil.scissor(this.getRowLeft(), this.getTop(), this.getWidth(), this.getBottom() - this.getTop());
            super.render(matrixStack, mouseX, mouseY, partialTicks);
            RenderSystem.disableScissor();
        }

        @Override
        public boolean keyPressed(int key, int scanCode, int modifiers)
        {
            if(key == GLFW.GLFW_KEY_ENTER && this.getSelected() != null)
            {
                CatalogueModListScreen.this.setSelectedModInfo(this.getSelected().info);
                SoundHandler handler = Minecraft.getInstance().getSoundManager();
                handler.play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            return super.keyPressed(key, scanCode, modifiers);
        }

        @Override
        public void centerScrollOn(ModEntry entry)
        {
            super.centerScrollOn(entry);
        }
    }

    private class ModEntry extends AbstractList.AbstractListEntry<ModEntry>
    {
        private final IModInfo info;
        private final ModList list;

        public ModEntry(IModInfo info, ModList list)
        {
            this.info = info;
            this.list = list;
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            // Draws mod name and version
            drawString(matrixStack, CatalogueModListScreen.this.font, this.getFormattedModName(), left + 24, top + 2, 0xFFFFFF);
            drawString(matrixStack, CatalogueModListScreen.this.font, new StringTextComponent(this.info.getVersion().toString()).withStyle(TextFormatting.GRAY), left + 24, top + 12, 0xFFFFFF);

            CatalogueModListScreen.this.loadAndCacheIcon(this.info);

            // Draw icon
            if(ICON_CACHE.containsKey(this.info.getModId()) && ICON_CACHE.get(this.info.getModId()).getLeft() != null)
            {
                ResourceLocation logoResource = TextureManager.INTENTIONAL_MISSING_TEXTURE;
                Size2i size = new Size2i(16, 16);

                Pair<ResourceLocation, Size2i> logoInfo = ICON_CACHE.get(this.info.getModId());
                if(logoInfo != null && logoInfo.getLeft() != null)
                {
                    logoResource = logoInfo.getLeft();
                    size = logoInfo.getRight();
                }

                Minecraft.getInstance().getTextureManager().bind(logoResource);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                AbstractGui.blit(matrixStack, left + 4, top + 2, 16, 16, 0.0F, 0.0F, size.width, size.height, size.width, size.height);
                RenderSystem.disableBlend();
            }
            else
            {
                try
                {
                    CatalogueModListScreen.this.getMinecraft().getItemRenderer().renderGuiItem(this.getItemIcon(), left + 4, top + 2);
                }
                catch(Exception e)
                {
                    ITEM_CACHE.put(this.info.getModId(), new ItemStack(Items.GRASS_BLOCK));
                }
            }

            // Draws an icon if there is an update for the mod
            VersionChecker.CheckResult result = VersionChecker.getResult(this.info);
            if(result.status.shouldDraw())
            {
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                Minecraft.getInstance().getTextureManager().bind(VERSION_CHECK_ICONS);
                int vOffset = result.status.isAnimated() && (System.currentTimeMillis() / 800 & 1) == 1 ? 8 : 0;
                AbstractGui.blit(matrixStack, left + rowWidth - 8 - 10, top + 6, result.status.getSheetOffset() * 8, vOffset, 8, 8, 64, 16);
            }
        }

        private ItemStack getItemIcon()
        {
            if(ITEM_CACHE.containsKey(this.info.getModId()))
            {
                return ITEM_CACHE.get(this.info.getModId());
            }

            // Put grass as default item icon
            ITEM_CACHE.put(this.info.getModId(), new ItemStack(Items.GRASS_BLOCK));

            // Special case for Forge to set item icon to anvil
            if(this.info.getModId().equals("forge"))
            {
                ItemStack anvil = new ItemStack(Items.ANVIL);
                ITEM_CACHE.put("forge", anvil);
                return anvil;
            }

            // Gets the raw item icon resource string
            String itemIcon = (String) this.info.getModProperties().get("catalogueItemIcon");
            if(itemIcon == null)
            {
                //Fallback to old method for backwards compatibility
                itemIcon = (String) ((ModInfo) this.info).getConfigElement("itemIcon").orElse("");
            }

            if(!itemIcon.isEmpty())
            {
                try
                {
                    ItemParser parser = new ItemParser(new StringReader(itemIcon), false).parse();
                    ItemStack item = new ItemStack(parser.getItem(), 1, parser.getNbt());
                    ITEM_CACHE.put(this.info.getModId(), item);
                    return item;
                }
                catch(CommandSyntaxException ignored) {}
            }

            // If the mod doesn't specify an item to use, Catalogue will attempt to get an item from the mod
            Optional<ItemStack> optional = ForgeRegistries.ITEMS.getValues().stream().filter(item -> item.getRegistryName().getNamespace().equals(this.info.getModId())).map(ItemStack::new).findFirst();
            if(optional.isPresent())
            {
                ItemStack item = optional.get();
                if(item.isEmpty())
                {
                    ITEM_CACHE.put(this.info.getModId(), item);
                    return item;
                }
            }

            return new ItemStack(Items.GRASS_BLOCK);
        }

        private ITextComponent getFormattedModName()
        {
            String name = this.info.getDisplayName();
            int width = this.list.getRowWidth() - (this.list.getMaxScroll() > 0 ? 30 : 24);
            if(CatalogueModListScreen.this.font.width(name) > width)
            {
                name = CatalogueModListScreen.this.font.plainSubstrByWidth(name, width - 10) + "...";
            }
            IFormattableTextComponent title = new StringTextComponent(name);
            if(this.info.getModId().equals("forge") || this.info.getModId().equals("minecraft"))
            {
                title.withStyle(TextFormatting.DARK_GRAY);
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

        public IModInfo getInfo()
        {
            return this.info;
        }
    }

    private class StringList extends ExtendedList<StringEntry>
    {
        public StringList(int width, int height, int left, int top)
        {
            super(CatalogueModListScreen.this.minecraft, width, CatalogueModListScreen.this.height, top, top + height, 10);
            this.setLeftPos(left);
        }

        public void setTextFromInfo(IModInfo info)
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
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            ScreenUtil.scissor(this.getRowLeft(), this.getTop(), this.getWidth(), this.getBottom() - this.getTop());
            super.render(matrixStack, mouseX, mouseY, partialTicks);
            RenderSystem.disableScissor();
        }
    }

    private class StringEntry extends AbstractList.AbstractListEntry<StringEntry>
    {
        private String line;

        public StringEntry(String line)
        {
            this.line = line;
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            drawString(matrixStack, CatalogueModListScreen.this.font, this.line, left, top, 0xFFFFFF);
        }
    }
}
