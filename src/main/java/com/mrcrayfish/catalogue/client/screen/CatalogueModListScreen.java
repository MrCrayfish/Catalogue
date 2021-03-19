package com.mrcrayfish.catalogue.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Size2i;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.client.ConfigGuiHandler;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.packs.ModFileResourcePack;
import net.minecraftforge.fml.packs.ResourcePackLoader;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class CatalogueModListScreen extends Screen
{
    private static final ResourceLocation MISSING_BANNER = new ResourceLocation("catalogue", "textures/gui/missing_banner.png");
    private static final Comparator<ModEntry> SORT = Comparator.comparing(o -> o.getInfo().getDisplayName());
    private static final ResourceLocation VERSION_CHECK_ICONS = new ResourceLocation(ForgeVersion.MOD_ID, "textures/gui/version_check_icons.png");
    private static Map<String, Pair<ResourceLocation, Size2i>> logoCache = new HashMap<>();

    private TextFieldWidget searchTextField;
    private ModList modList;
    private Button backButton;
    private IModInfo selectedModInfo;
    private Button configButton;
    private Button websiteButton;
    private Button issueButton;

    public CatalogueModListScreen()
    {
        super(StringTextComponent.EMPTY);
    }

    @Override
    protected void init()
    {
        super.init();
        this.searchTextField = new TextFieldWidget(this.font, 11, 25, 148, 20, StringTextComponent.EMPTY);
        this.searchTextField.setResponder(s -> this.modList.filterAndUpdateList(s));
        this.children.add(this.searchTextField);
        this.modList = new ModList();
        this.modList.setLeftPos(10);
        this.children.add(this.modList);
        this.backButton = this.addButton(new Button(9, this.modList.getBottom() + 8, 152, 20, new StringTextComponent("Back"), onPress -> {
            this.getMinecraft().setScreen(null);
        }));
        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.width - (this.modList.getRight() + 12 + 10) - 10;
        int buttonWidth = (contentWidth - 10) / 3;
        this.configButton = this.addButton(new Button(contentLeft, 105, buttonWidth, 20, new StringTextComponent("Config"), onPress ->
        {
            if(this.selectedModInfo != null)
            {
                ConfigGuiHandler.getGuiFactoryFor((ModInfo) this.selectedModInfo).map(f -> f.apply(this.minecraft, this)).ifPresent(newScreen -> this.getMinecraft().setScreen(newScreen));
            }
        }));
        this.configButton.visible = false;
        this.websiteButton = this.addButton(new Button(contentLeft + buttonWidth + 5, 105, buttonWidth, 20, new StringTextComponent("Website"), onPress -> {
            this.getMinecraft().setScreen(null);
        }));
        this.websiteButton.visible = false;
        this.issueButton = this.addButton(new Button(contentLeft + buttonWidth + buttonWidth + 10, 105, buttonWidth, 20, new StringTextComponent("Submit Bug"), onPress -> {
            this.getMinecraft().setScreen(null);
        }));
        this.issueButton.visible = false;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        this.modList.render(matrixStack, mouseX, mouseY, partialTicks);
        this.modList.setRenderTopAndBottom(false);
        //this.hLine(matrixStack, this.modList.getLeft(), this.modList.getLeft() + this.modList.getWidth() - 1, this.modList.getBottom(), 0xFFA0A0A0);
        //this.vLine(matrixStack, this.modList.getLeft() - 1, this.modList.getTop() - 1, this.modList.getBottom() + 1, 0xFFA0A0A0);
        //this.vLine(matrixStack, this.modList.getRight(), this.modList.getTop() - 1, this.modList.getBottom() + 1, 0xFFA0A0A0);
        drawString(matrixStack, this.font, new StringTextComponent("Mods").withStyle(TextFormatting.BOLD).withStyle(TextFormatting.WHITE), 70, 10, 0xFFFFFF);
        this.searchTextField.render(matrixStack, mouseX, mouseY, partialTicks);
        this.vLine(matrixStack, this.modList.getRight() + 11, -1, this.height, 0xFF707070);
        fill(matrixStack, this.modList.getRight() + 12, 0, this.width, this.height, 0x66000000);
        if(this.selectedModInfo != null)
        {
            int contentLeft = this.modList.getRight() + 12 + 10;
            this.drawLogo(matrixStack, contentLeft, 10, this.width - (this.modList.getRight() + 12 + 10) - 10, 50);

            // Draw mod name
            matrixStack.pushPose();
            matrixStack.translate(contentLeft, 10 + 50 + 10, 0);
            matrixStack.scale(2.0F, 2.0F, 2.0F);
            ITextComponent modName = new StringTextComponent(this.selectedModInfo.getDisplayName());
            int width = this.font.width(modName);
            drawString(matrixStack, this.font, modName, 0, 0, 0xFFFFFF);
            matrixStack.popPose();

            // Draw version
            drawString(matrixStack, this.font, "v" + this.selectedModInfo.getVersion().toString(), contentLeft + width * 2 + 5, 10 + 50 + 10 + 7, 0xFFFFFF);

            // Draw authors
            String author = (String) ((ModInfo) this.selectedModInfo).getConfigElement("authors").orElse("Unknown");
            drawString(matrixStack, this.font, new StringTextComponent("By " + author).withStyle(TextFormatting.GRAY), contentLeft, 92, 0xFFFFFF);

            // Draw description
            String description = this.selectedModInfo.getDescription().trim();
            this.font.drawWordWrap(new StringTextComponent(description), contentLeft, 10 + 50 + 75, 200, 0xFFFFFFFF);

            String license = this.selectedModInfo.getOwningFile().getLicense();
            drawString(matrixStack, this.font, "Copyright © " + license, contentLeft, this.height - 20, 0xBBBBBB);
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void setSelectedModInfo(IModInfo selectedModInfo)
    {
        this.selectedModInfo = selectedModInfo;
        this.loadAndCacheLogo(selectedModInfo);
        this.configButton.visible = true;
        this.websiteButton.visible = true;
        this.issueButton.visible = true;
        this.configButton.active = ConfigGuiHandler.getGuiFactoryFor((ModInfo) selectedModInfo).isPresent();
        this.websiteButton.active = ((ModInfo) selectedModInfo).getConfigElement("displayURL").isPresent();
        this.issueButton.active = ((ModInfo) selectedModInfo).getOwningFile().getConfigElement("issueTrackerURL").isPresent();
    }

    private void drawLogo(MatrixStack matrixStack, int x, int y, int maxWidth, int maxHeight)
    {
        if(this.selectedModInfo != null)
        {
            ResourceLocation logoResource = MISSING_BANNER;
            Size2i size = new Size2i(600, 120);

            if(logoCache.containsKey(this.selectedModInfo.getModId()))
            {
                Pair<ResourceLocation, Size2i> logoInfo = logoCache.get(this.selectedModInfo.getModId());
                if(logoInfo.getLeft() != null)
                {
                    logoResource = logoInfo.getLeft();
                    size = logoInfo.getRight();
                }
            }

            Minecraft.getInstance().getTextureManager().bind(logoResource);
            RenderSystem.enableBlend();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
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

            AbstractGui.blit(matrixStack, x, y, width, height, 0.0F, 0.0F, size.width, size.height, size.width, size.height);
        }
    }

    private void loadAndCacheLogo(IModInfo info)
    {
        if(logoCache.containsKey(info.getModId()))
             return;

        // Fills an empty logo as logo may not be present
        logoCache.put(info.getModId(), Pair.of(null, new Size2i(0, 0)));

        // Attempts to load the real logo
        ModInfo modInfo = (ModInfo) info;
        modInfo.getLogoFile().ifPresent(s ->
        {
            ModFileResourcePack resourcePack = ResourcePackLoader.getResourcePackFor(info.getModId()).orElse(ResourcePackLoader.getResourcePackFor("forge").orElseThrow(() -> new RuntimeException("Can't find forge, WHAT!")));
            try(InputStream is = resourcePack.getRootResource(s))
            {
                NativeImage logo = NativeImage.read(is);
                TextureManager textureManager = this.getMinecraft().getTextureManager();
                logoCache.put(info.getModId(), Pair.of(textureManager.register("modlogo", this.createLogoTexture(logo, modInfo)), new Size2i(logo.getWidth(), logo.getHeight())));
            }
            catch(IOException ignored) {}
        });
    }

    private DynamicTexture createLogoTexture(NativeImage image, ModInfo info)
    {
        return new DynamicTexture(image)
        {
            @Override
            public void upload()
            {
                this.bind();
                image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), info.getLogoBlur(), false, false, false);
            }
        };
    }

    private class ModList extends ExtendedList<ModEntry>
    {
        public ModList()
        {
            super(CatalogueModListScreen.this.minecraft, 150, CatalogueModListScreen.this.height, 46, CatalogueModListScreen.this.height - 35, 26);
            this.filterAndUpdateList(""); // This will add all mods
        }

        @Override
        protected int getScrollbarPosition()
        {
            return this.width;
        }

        @Override
        public int getRowWidth()
        {
            return this.width;
        }

        public void filterAndUpdateList(String text)
        {
            List<ModEntry> entries = net.minecraftforge.fml.ModList.get().getMods().stream().filter(info -> {
                return info.getDisplayName().toLowerCase(Locale.ENGLISH).contains(text.toLowerCase(Locale.ENGLISH));
            }).map(info -> new ModEntry(info, this)).sorted(SORT).collect(Collectors.toList());
            this.replaceEntries(entries);
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
            drawString(matrixStack, CatalogueModListScreen.this.font, this.getFormattedModName(), left + 22, top + 2, 0xFFFFFF);
            drawString(matrixStack, CatalogueModListScreen.this.font, new StringTextComponent(this.info.getVersion().toString()).withStyle(TextFormatting.GRAY), left + 22, top + 12, 0xFFFFFF);

            // Draw item icon
            String itemIcon = (String) ((ModInfo) this.info).getConfigElement("itemIcon").orElse("");
            Item iconItem = itemIcon.isEmpty() ? Items.GRASS_BLOCK : ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemIcon));
            if(this.info.getModId().equals("forge")) iconItem = Items.ANVIL;
            CatalogueModListScreen.this.getMinecraft().getItemRenderer().renderGuiItem(new ItemStack(iconItem), left + 2, top + 2);

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

        private ITextComponent getFormattedModName()
        {
            IFormattableTextComponent title = new StringTextComponent(this.info.getDisplayName());
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
}
