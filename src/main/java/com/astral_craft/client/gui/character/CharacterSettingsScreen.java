package com.astral_craft.client.gui.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.components.AstralDropdown;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralHorizontalScrollbar;
import com.astral_craft.client.gui.components.AstralVerticalScrollbar;
import com.astral_craft.client.model.character.AstralGeoAnimationManager;
import com.astral_craft.client.model.character.AstralGeoPose;
import com.astral_craft.client.render.character.AstralCharacterRenderState;
import com.astral_craft.client.text.AstralInlineTextFormatter;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.*;
import com.astral_craft.common.network.OpenCharacterSettingsPayload;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;

@SuppressWarnings("SameParameterValue")
public class CharacterSettingsScreen extends Screen {

    protected final List<CharacterDefinition> characters;
    protected final Map<MainTab, CharacterDetailPage> detailPages = new EnumMap<>(MainTab.class);
    protected Identifier selectedCharacterId;
    protected Identifier equippedCharacterId;
    protected String selectedSkinId;
    protected String equippedSkinId;
    protected Set<Identifier> unlockedCharacterIds;
    protected Set<String> unlockedSkinIds;
    protected Map<Identifier, CharacterProgressEntry> progressEntries;
    protected int level;
    protected int experience;
    protected int friendship;
    protected ScreenMode mode = ScreenMode.LIST;
    protected MainTab mainTab = MainTab.ARCHIVE;
    protected ArchiveTab archiveTab = ArchiveTab.SKILLS;
    protected CharacterSkillDefinition.SkillMode skillMode = CharacterSkillDefinition.SkillMode.PVP;
    protected CharacterSortMode sortMode = CharacterSortMode.DEFAULT;
    protected String characterNamespaceFilter = "";
    protected float characterScroll;
    protected float bodyScroll;
    protected float skinScroll;
    protected float previewYaw = -225.0F;
    protected float previewPitch = -10.0F;
    protected float previewRoll = 0.0F;
    protected float previewZoom = 1.0F;
    protected String previewAnimationAction = "idle";
    protected boolean previewAnimationPlaying = true;
    protected boolean previewAnimationDropdownOpen;
    protected boolean sourceDropdownOpen;
    protected boolean sortDropdownOpen;
    protected float previewAnimationTimeSeconds;
    protected boolean draggingPreview;
    protected ScrollTarget draggingScrollbar = ScrollTarget.NONE;
    protected int dragButton;
    protected double lastDragX;
    protected double lastDragY;
    protected final Map<String, AstralCharacterEntity> previewEntities = new HashMap<>();
    protected boolean hoveredClickable;

    public CharacterSettingsScreen(
            List<CharacterDefinition> characters, Identifier selectedCharacterId, String selectedSkinId,
            int level, int experience, int friendship, Set<Identifier> unlockedCharacterIds,
            Set<String> unlockedSkinIds, Map<Identifier, CharacterProgressEntry> progressEntries) {
        super(Component.translatable("gui.astral_craft.character_settings.title"));
        this.characters = characters.isEmpty() ? List.of(CharacterDefinition.builtinDefault()) : characters;
        this.selectedCharacterId = selectedCharacterId;
        this.equippedCharacterId = selectedCharacterId;
        this.selectedSkinId = selectedSkinId == null || selectedSkinId.isBlank() ? "default" : selectedSkinId;
        this.equippedSkinId = this.selectedSkinId;
        this.unlockedCharacterIds = new HashSet<>(unlockedCharacterIds);
        this.unlockedSkinIds = new HashSet<>(unlockedSkinIds);
        this.progressEntries = new HashMap<>(progressEntries);
        this.level = CharacterProgressEntry.clampPveLevel(level);
        this.experience = Math.max(0, experience);
        this.friendship = CharacterProgressEntry.clampFriendshipLevel(friendship);
        this.syncDefaultUnlocks();
        if (!this.hasCharacter(this.equippedCharacterId)) {
            this.equippedCharacterId = this.firstDisplayCharacterId();
            this.equippedSkinId = this.skinIdFor(this.selectedCharacter());
        }

        this.selectedCharacterId = this.firstDisplayCharacterId();
        this.selectedSkinId = this.skinIdFor(this.selectedCharacter());
        this.registerDetailPages();
        this.syncSelectedProgress();
        this.previewAnimationAction = this.selectedCharacter().previewAction();
    }

    public static void open(OpenCharacterSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            List<CharacterDefinition> definitions = CharacterCodecLines.decode(payload.encodedCharacters());
            Identifier selected = definitions.stream().findFirst().map(CharacterDefinition::id).orElse(CharacterDefinition.builtinDefault().id());
            try {
                selected = Identifier.parse(payload.selectedCharacterId());
            } catch (Exception ignored) {}
            Minecraft.getInstance().setScreen(new CharacterSettingsScreen(definitions, selected, payload.selectedSkinId(), payload.level(), payload.experience(), payload.friendship(),
                    decodeIdentifierSet(payload.unlockedCharacterIds()), decodeStringSet(payload.unlockedSkinIds()), decodeProgressEntries(payload.encodedProgressEntries())));
        });
    }

    @Override
    protected void init() {
        this.clearWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.previewAnimationPlaying) {
            this.previewAnimationTimeSeconds += 0.05F;
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.clampScrolls();
        this.hoveredClickable = false;
        CharacterLayout layout = this.layout();
        this.renderShell(graphics, layout, mouseX, mouseY);
        this.renderPreview(graphics, layout, mouseX, mouseY);
        if (this.mode == ScreenMode.LIST) {
            this.renderCharacterListPage(graphics, layout, mouseX, mouseY);
        } else {
            this.renderDetailPage(graphics, layout, mouseX, mouseY);
        }

        AstralFancyButton.setHandCursor(this.hoveredClickable);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        AstralFancyButton.setHandCursor(false);
        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        CharacterLayout layout = this.layout();
        if (event.button() == 0 && this.tryStartScrollbarDrag(layout, mouseX, mouseY)) {
            return true;
        }

        if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.backX, layout.backY, layout.backW, layout.backH)) {
            this.handleBack();
            return true;
        }

        if (event.button() == 0 && this.handlePreviewAnimationClick(layout, mouseX, mouseY)) {
            return true;
        }

        if (this.isOverPreview(layout, mouseX, mouseY)) {
            if (doubleClick) {
                this.resetPreviewRotation();
                return true;
            }

            this.draggingPreview = true;
            this.dragButton = event.button();
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
            return true;
        }

        if (this.mode == ScreenMode.LIST) {
            if (event.button() == 0 && this.isInside(mouseX, mouseY, layout.detailButtonX, layout.detailButtonY, layout.detailButtonW, layout.detailButtonH)) {
                this.mode = ScreenMode.DETAIL;
                this.mainTab = MainTab.ARCHIVE;
                this.archiveTab = ArchiveTab.SKILLS;
                this.bodyScroll = 0.0F;
                this.skinScroll = 0.0F;
                return true;
            }

            if (event.button() == 0 && this.handleCharacterListDropdownClick(layout, mouseX, mouseY)) {
                return true;
            }

            if (event.button() == 0 && this.handleCharacterGridClick(layout, mouseX, mouseY)) {
                return true;
            }
        } else {
            if (event.button() == 0 && this.handleMainTabClick(layout, mouseX, mouseY)) {
                return true;
            }

            if (event.button() == 0 && this.currentDetailPage().mouseClicked(layout, mouseX, mouseY)) {
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar != ScrollTarget.NONE) {
            this.updateScrollbarDrag(this.layout(), event.x(), event.y());
            return true;
        }

        if (this.draggingPreview) {
            float dx = (float) (event.x() - this.lastDragX);
            float dy = (float) (event.y() - this.lastDragY);
            if (this.dragButton == 1) {
                this.previewRoll += dx * 0.55F;
            } else {
                this.previewYaw += dx * 0.82F;
                this.previewPitch = Mth.clamp(this.previewPitch - dy * 0.62F, -72.0F, 72.0F);
            }

            this.lastDragX = event.x();
            this.lastDragY = event.y();
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (this.draggingScrollbar != ScrollTarget.NONE && event.button() == 0) {
            this.draggingScrollbar = ScrollTarget.NONE;
            return true;
        }

        if (this.draggingPreview && event.button() == this.dragButton) {
            this.draggingPreview = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        CharacterLayout layout = this.layout();
        if (this.isOverPreview(layout, mouseX, mouseY)) {
            this.previewZoom = Mth.clamp(this.previewZoom + (float) deltaY * 0.08F, 0.72F, 1.34F);
            return true;
        }

        if (this.mode == ScreenMode.LIST && this.isInside(mouseX, mouseY, layout.gridX, layout.gridY, layout.gridW, layout.gridH)) {
            this.characterScroll = Mth.clamp(this.characterScroll - (float) deltaY * 28.0F, 0.0F, this.maxCharacterScroll(layout));
            return true;
        }

        if (this.mode == ScreenMode.DETAIL && this.isInside(mouseX, mouseY, layout.bodyX, layout.bodyY, layout.bodyW, layout.bodyH)) {
            CharacterDetailPage page = this.currentDetailPage();
            if (page.usesSkinScroll()) {
                this.skinScroll = Mth.clamp(this.skinScroll - (float) deltaY * 36.0F, 0.0F, this.maxSkinScroll(layout));
                return true;
            }

            float maxScroll = page.maxScroll(layout);
            if (maxScroll > 0.5F) {
                this.bodyScroll = Mth.clamp(this.bodyScroll - (float) deltaY * 22.0F, 0.0F, maxScroll);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.handleBack();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_R) {
            this.resetPreviewRotation();
            return true;
        }

        return super.keyPressed(event);
    }

    protected void renderShell(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        AstralFancyButton.renderOutlinedBox(graphics, layout.leftX - 3, layout.topY - 3, layout.leftW + 6, layout.totalH + 6, 0xD214141D, 0xFF3B4056, 0xFF101018, 2, 2);
        AstralFancyButton.renderOutlinedBox(graphics, layout.rightX - 3, layout.topY - 3, layout.rightW + 6, layout.totalH + 6, 0xD016161F, 0xFF3B4056, 0xFF101018, 2, 2);
        graphics.fill(layout.leftX + 3, layout.topY + 3, layout.leftX + layout.leftW - 3, layout.topY + 30, 0xCC232333);
        graphics.fill(layout.rightX + 3, layout.topY + 3, layout.rightX + layout.rightW - 3, layout.topY + 36, 0xCC232333);
        graphics.fill(layout.leftX - 1, layout.topY - 1, layout.leftX + layout.leftW + 1, layout.topY, 0x66FFFFFF);
        graphics.fill(layout.rightX - 1, layout.topY - 1, layout.rightX + layout.rightW + 1, layout.topY, 0x66FFFFFF);
        boolean backHover = this.isInside(mouseX, mouseY, layout.backX, layout.backY, layout.backW, layout.backH);
        MutableComponent backText = Component.translatable(this.mode == ScreenMode.DETAIL ? "gui.astral_craft.character_settings.back" : "gui.astral_craft.character_settings.close");
        this.renderFancyButton(graphics, backText, layout.backX, layout.backY, layout.backW, layout.backH, false, backHover, this.backButtonStyle());
        MutableComponent title = Component.translatable(this.mode == ScreenMode.LIST ? "gui.astral_craft.character_settings.character_select" : "gui.astral_craft.character_settings.character_detail");
        graphics.text(this.font, title, layout.rightX + 14, layout.topY + 13, 0xFFFFFFFF, false);
    }

    protected void renderPreview(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        CharacterDefinition selected = this.selectedCharacter();
        int previewTop = layout.previewY;
        int previewBottom = layout.previewY + layout.previewH;
        int controlsTop = previewBottom - 30;
        graphics.fill(layout.previewX, previewTop, layout.previewX + layout.previewW, previewBottom, 0x99000000);
        graphics.fill(layout.previewX + 2, previewTop + 2, layout.previewX + layout.previewW - 2, previewBottom - 2, 0x55303548);
        graphics.fill(layout.previewX + 4, previewTop + 4, layout.previewX + layout.previewW - 4, previewTop + 5, 0x99FF4FAE);
        LivingEntity entity = this.previewEntity();
        if (entity != null) {
            this.renderEntityModel(graphics, entity, layout.previewX + 8, previewTop + 8,
                    layout.previewX + layout.previewW - 8, controlsTop - 4,
                    this.previewYaw, this.previewPitch, this.previewRoll,
                    layout.previewEntityScale * this.previewZoom);
        }

        this.renderPreviewAnimationControls(graphics, layout, mouseX, mouseY);
        int infoY = layout.previewY + layout.previewH + 10;
        graphics.text(this.font, Component.translatable(selected.nameKey()), layout.leftX + 12, infoY, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.translatable(selected.titleKey()).withStyle(ChatFormatting.YELLOW), layout.leftX + 12, infoY + 13, 0xFFFFFFFF, false);
    }

    protected void renderCharacterListPage(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        boolean detailHover = this.isInside(mouseX, mouseY, layout.detailButtonX, layout.detailButtonY, layout.detailButtonW, layout.detailButtonH);
        MutableComponent detailText = Component.translatable("gui.astral_craft.character_settings.character_detail");
        this.renderFancyButton(graphics, detailText, layout.detailButtonX, layout.detailButtonY, layout.detailButtonW, layout.detailButtonH, false, detailHover, this.pinkButtonStyle());
        this.renderCharacterListDropdownButtons(graphics, layout, mouseX, mouseY);
        graphics.fill(layout.gridX - 6, layout.gridY - 6, layout.gridX + layout.gridW + 6, layout.gridY + layout.gridH + 6, 0x66101018);
        graphics.enableScissor(layout.gridX, layout.gridY, layout.gridX + layout.gridW, layout.gridY + layout.gridH);
        this.renderCharacterGroups(graphics, layout, mouseX, mouseY);
        graphics.disableScissor();
        this.renderVerticalScrollbar(graphics, layout.gridX + layout.gridW + 2, layout.gridY, layout.gridH, this.characterScroll, this.maxCharacterScroll(layout));
        this.renderCharacterListDropdownMenus(graphics, layout);
    }

    protected void renderCharacterGroups(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        int columns = this.characterGridColumns(layout);
        int cardW = layout.characterCardW;
        int cardH = layout.characterCardH;
        int cursorY = layout.gridY - Math.round(this.characterScroll);
        for (Map.Entry<String, List<CharacterDefinition>> group : this.displayCharacterGroups().entrySet()) {
            if (cursorY + 20 >= layout.gridY && cursorY <= layout.gridY + layout.gridH) {
                MutableComponent header = Component.translatable("gui.astral_craft.character_settings.addon_header", this.displayNamespaceName(group.getKey()));
                graphics.fill(layout.gridX, cursorY + 1, layout.gridX + layout.gridW, cursorY + 18, 0xAA000000);
                graphics.text(this.font, this.ellipsize(header, layout.gridW - 8), layout.gridX + 6, cursorY + 5, 0xFFFFF5FF, false);
            }

            cursorY += 22;
            List<CharacterDefinition> definitions = group.getValue();
            for (int i = 0; i < definitions.size(); i++) {
                int row = i / columns;
                int column = i % columns;
                int cardX = layout.gridX + column * (cardW + CharacterLayout.GRID_GAP);
                int cardY = cursorY + row * (cardH + CharacterLayout.GRID_GAP);
                if (cardY + cardH < layout.gridY || cardY > layout.gridY + layout.gridH) continue;
                CharacterDefinition definition = definitions.get(i);
                boolean selected = definition.id().equals(this.selectedCharacterId);
                boolean equipped = definition.id().equals(this.equippedCharacterId);
                boolean unlocked = this.isCharacterUnlocked(definition);
                boolean hovered = this.isInside(mouseX, mouseY, cardX, cardY, cardW, cardH);
                this.renderCharacterCard(graphics, definition, this.entityFor(definition, this.skinIdFor(definition)), cardX, cardY, cardW, cardH, selected, equipped, unlocked, hovered, layout.cardEntityScale);
            }

            int rows = (definitions.size() + columns - 1) / columns;
            cursorY += rows * (cardH + CharacterLayout.GRID_GAP) + 8;
        }
    }

    protected void renderDetailPage(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        this.renderMainTabs(graphics, layout, mouseX, mouseY);
        this.currentDetailPage().render(graphics, layout, mouseX, mouseY);
    }

    protected void renderMainTabs(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        int x = layout.mainTabX;
        int tabW = layout.mainTabW;
        for (MainTab value : MainTab.values()) {
            boolean selected = this.mainTab == value;
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.mainTabY, tabW, layout.mainTabH);
            this.renderTab(graphics, x, layout.mainTabY, tabW, layout.mainTabH, value.translationKey(), selected, hovered, selected ? this.selectedButtonStyle() : this.pinkButtonStyle());
            x += tabW + CharacterLayout.TAB_GAP;
        }
    }

    protected void renderArchiveTabs(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        int x = layout.subTabX;
        int tabW = layout.subTabW;
        for (ArchiveTab value : ArchiveTab.values()) {
            boolean selected = this.archiveTab == value;
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.subTabY, tabW, layout.subTabH);
            MutableComponent text = Component.translatable(value.titleKey());
            this.renderFancyButton(graphics, text, x, layout.subTabY, tabW, layout.subTabH, selected, hovered, selected ? this.selectedButtonStyle() : this.pinkButtonStyle());
            x += tabW + CharacterLayout.TAB_GAP;
        }
    }

    protected void renderBodyContainer(GuiGraphicsExtractor graphics, CharacterLayout layout) {
        graphics.fill(layout.bodyX, layout.bodyY, layout.bodyX + layout.bodyW, layout.bodyY + layout.bodyH, 0xD00B0B11);
        graphics.fill(layout.bodyX + 2, layout.bodyY + 2, layout.bodyX + layout.bodyW - 2, layout.bodyY + layout.bodyH - 2, 0x88444852);
        graphics.fill(layout.bodyX + 8, layout.bodyY + 8, layout.bodyX + layout.bodyW - 8, layout.bodyY + 9, 0x99FFFFFF);
    }

    protected void renderCharacterListDropdownButtons(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        AstralFancyButton.ButtonStyle style = this.pinkButtonStyle().withTextScale(0.88F);
        AstralDropdown.Layout sourceLayout = this.sourceDropdownLayout(layout);
        AstralDropdown.Layout sortLayout = this.sortDropdownLayout(layout);
        boolean sourceHovered = sourceLayout.containsButton(mouseX, mouseY) || (this.sourceDropdownOpen && sourceLayout.containsMenu(mouseX, mouseY, this.sourceDropdownEntries().size()));
        boolean sortHovered = sortLayout.containsButton(mouseX, mouseY) || (this.sortDropdownOpen && sortLayout.containsMenu(mouseX, mouseY, this.sortDropdownEntries().size()));
        this.hoveredClickable |= sourceHovered || sortHovered;
        AstralDropdown.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.character_settings.source.label"),
                this.sourceDropdownEntries(), this.characterNamespaceFilter, sourceLayout,
                this.sourceDropdownOpen, sourceHovered, style);
        AstralDropdown.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.character_settings.sort.label"),
                this.sortDropdownEntries(), this.sortMode.name(), sortLayout,
                this.sortDropdownOpen, sortHovered, style);
    }

    protected void renderCharacterListDropdownMenus(GuiGraphicsExtractor graphics, CharacterLayout layout) {
        AstralFancyButton.ButtonStyle style = this.pinkButtonStyle().withTextScale(0.88F);
        if (this.sourceDropdownOpen) {
            AstralDropdown.renderMenu(graphics, this.font, this.sourceDropdownEntries(), this.characterNamespaceFilter, this.sourceDropdownLayout(layout), style);
        }

        if (this.sortDropdownOpen) {
            AstralDropdown.renderMenu(graphics, this.font, this.sortDropdownEntries(), this.sortMode.name(), this.sortDropdownLayout(layout), style);
        }
    }

    protected AstralDropdown.Layout sourceDropdownLayout(CharacterLayout layout) {
        int labelWidth = 50;
        int width = Math.clamp((layout.gridW - 24) / 2 - labelWidth - AstralDropdown.LABEL_GAP, 74, 130);
        return AstralDropdown.layout(layout.gridX, layout.gridY - 30, labelWidth, width);
    }

    protected AstralDropdown.Layout sortDropdownLayout(CharacterLayout layout) {
        int labelWidth = 50;
        int sourceTotal = labelWidth + AstralDropdown.LABEL_GAP + this.sourceDropdownLayout(layout).width() + 14;
        int width = Math.clamp(layout.gridW - sourceTotal - labelWidth - AstralDropdown.LABEL_GAP, 74, 130);
        return AstralDropdown.layout(layout.gridX + sourceTotal, layout.gridY - 30, labelWidth, width);
    }

    protected List<AstralDropdown.Entry> sourceDropdownEntries() {
        List<AstralDropdown.Entry> entries = new ArrayList<>();
        for (String namespace : this.characterNamespaceOptions()) {
            MutableComponent text = namespace.isBlank()
                    ? Component.translatable("gui.astral_craft.character_settings.source.all")
                    : Component.literal(this.displayNamespaceName(namespace));
            entries.add(AstralDropdown.Entry.of(namespace, text));
        }

        return entries;
    }

    protected List<AstralDropdown.Entry> sortDropdownEntries() {
        List<AstralDropdown.Entry> entries = new ArrayList<>();
        for (CharacterSortMode value : CharacterSortMode.values()) {
            entries.add(AstralDropdown.Entry.of(value.name(), Component.translatable(value.translationKey())));
        }

        return entries;
    }

    protected void renderCharacterCard(GuiGraphicsExtractor graphics, CharacterDefinition definition, LivingEntity entity, int x, int y, int w, int h, boolean selected, boolean equipped, boolean unlocked, boolean hovered, float scale) {
        new CharacterListCardComponent(this, definition, entity, x, y, w, h, selected, unlocked, hovered, scale).render(graphics);
    }

    protected void renderLockedOverlay(GuiGraphicsExtractor graphics, int x, int y, int w, int h, MutableComponent hint) {
        graphics.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xAA000000);
        for (int row = 6; row < h - 6; row += 7) {
            int diagonalX = x + 6 + row / 2;
            int x0 = Math.min(x + w - 12, diagonalX);
            int x1 = Math.min(x + w - 4, x0 + 12);
            graphics.fill(x0, y + row, x1, y + row + 2, 0x22FFFFFF);
        }

        MutableComponent locked = Component.translatable("gui.astral_craft.character_settings.locked");
        this.drawCenteredText(graphics, locked, x, y + Math.max(8, h / 2 - 8), w, 0xFFFFFFFF);
        this.drawCenteredText(graphics, this.ellipsize(hint, w - 8), x, y + Math.max(20, h / 2 + 5), w, 0xFFB8B8B8);
    }

    protected void renderProgressCards(GuiGraphicsExtractor graphics, CharacterDefinition definition, int x, int y, int maxWidth, int minLevel, int maxLevel, int currentLevel, String translationPrefix, int accentColor) {
        for (int level = minLevel; level <= maxLevel; level++) {
            ProgressLevelCardComponent card = new ProgressLevelCardComponent(this, definition, level, currentLevel, translationPrefix, accentColor);
            card.render(graphics, x, y, maxWidth);
            y += card.height(maxWidth) + ProgressLevelCardComponent.GAP;
        }
    }

    protected int progressCardsHeight(CharacterDefinition definition, int maxWidth, int minLevel, int maxLevel, int currentLevel, String translationPrefix, int accentColor) {
        int height = 0;
        for (int level = minLevel; level <= maxLevel; level++) {
            ProgressLevelCardComponent card = new ProgressLevelCardComponent(this, definition, level, currentLevel, translationPrefix, accentColor);
            height += card.height(maxWidth) + ProgressLevelCardComponent.GAP;
        }

        return Math.max(0, height - ProgressLevelCardComponent.GAP);
    }

    protected void renderTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String translationKey, boolean selected, boolean hovered, AstralFancyButton.ButtonStyle style) {
        MutableComponent text = Component.translatable(translationKey);
        this.hoveredClickable |= hovered;
        AstralFancyButton.renderTab(graphics, this.font, text, x, y, w, h, selected, hovered, style);
    }

    protected void renderFancyButton(GuiGraphicsExtractor graphics, MutableComponent text, int x, int y, int w, int h, boolean selected, boolean hovered, AstralFancyButton.ButtonStyle style) {
        this.hoveredClickable |= hovered;
        AstralFancyButton.Button button = AstralFancyButton.button(text, x, y, w, h, style);
        button.render(graphics, this.font, selected, hovered);
    }

    protected void drawCenteredText(GuiGraphicsExtractor graphics, MutableComponent text, int x, int y, int w, int color) {
        graphics.text(this.font, text, x + Math.max(0, (w - this.font.width(text)) / 2), y, color, false);
    }

    protected void renderVerticalScrollbar(GuiGraphicsExtractor graphics, int x, int y, int h, float scroll, float maxScroll) {
        AstralVerticalScrollbar.render(graphics, x, y, h, scroll, maxScroll);
    }

    protected void renderHorizontalScrollbar(GuiGraphicsExtractor graphics, int x, int y, int w, float scroll, float maxScroll) {
        AstralHorizontalScrollbar.render(graphics, x, y, w, scroll, maxScroll);
    }

    protected boolean handleMainTabClick(CharacterLayout layout, double mouseX, double mouseY) {
        int x = layout.mainTabX;
        for (MainTab value : MainTab.values()) {
            if (this.isInside(mouseX, mouseY, x, layout.mainTabY, layout.mainTabW, layout.mainTabH)) {
                this.mainTab = value;
                this.bodyScroll = 0.0F;
                this.skinScroll = 0.0F;
                return true;
            }

            x += layout.mainTabW + CharacterLayout.TAB_GAP;
        }

        return false;
    }

    protected boolean handleArchiveTabClick(CharacterLayout layout, double mouseX, double mouseY) {
        int x = layout.subTabX;
        for (ArchiveTab value : ArchiveTab.values()) {
            if (this.isInside(mouseX, mouseY, x, layout.subTabY, layout.subTabW, layout.subTabH)) {
                this.archiveTab = value;
                this.bodyScroll = 0.0F;
                return true;
            }

            x += layout.subTabW + CharacterLayout.TAB_GAP;
        }

        return false;
    }

    protected boolean handleCharacterListDropdownClick(CharacterLayout layout, double mouseX, double mouseY) {
        AstralDropdown.Layout sourceLayout = this.sourceDropdownLayout(layout);
        AstralDropdown.Layout sortLayout = this.sortDropdownLayout(layout);
        List<AstralDropdown.Entry> sourceEntries = this.sourceDropdownEntries();
        List<AstralDropdown.Entry> sortEntries = this.sortDropdownEntries();

        if (this.sourceDropdownOpen) {
            String selected = AstralDropdown.clickedEntry(sourceEntries, sourceLayout, mouseX, mouseY);
            if (selected != null) {
                this.characterNamespaceFilter = selected;
                this.characterScroll = 0.0F;
                this.sourceDropdownOpen = false;
                Identifier first = this.firstDisplayCharacterId();
                if (!this.hasDisplayedCharacter(this.selectedCharacterId)) {
                    this.selectedCharacterId = first;
                    this.selectedSkinId = this.skinIdFor(this.selectedCharacter());
                    this.syncSelectedProgress();
                    this.resetPreviewAnimationAction();
                }

                return true;
            }

            if (!sourceLayout.containsButton(mouseX, mouseY)) {
                this.sourceDropdownOpen = false;
                return true;
            }
        }

        if (this.sortDropdownOpen) {
            String selected = AstralDropdown.clickedEntry(sortEntries, sortLayout, mouseX, mouseY);
            if (selected != null) {
                try {
                    this.sortMode = CharacterSortMode.valueOf(selected);
                } catch (IllegalArgumentException ignored) {}
                this.characterScroll = 0.0F;
                this.sortDropdownOpen = false;
                return true;
            }

            if (!sortLayout.containsButton(mouseX, mouseY)) {
                this.sortDropdownOpen = false;
                return true;
            }
        }

        if (sourceLayout.containsButton(mouseX, mouseY)) {
            this.sourceDropdownOpen = !this.sourceDropdownOpen;
            this.sortDropdownOpen = false;
            return true;
        }

        if (sortLayout.containsButton(mouseX, mouseY)) {
            this.sortDropdownOpen = !this.sortDropdownOpen;
            this.sourceDropdownOpen = false;
            return true;
        }

        return false;
    }

    protected boolean handleCharacterGridClick(CharacterLayout layout, double mouseX, double mouseY) {
        int columns = this.characterGridColumns(layout);
        int cardW = layout.characterCardW;
        int cardH = layout.characterCardH;
        int cursorY = layout.gridY - Math.round(this.characterScroll);
        for (Map.Entry<String, List<CharacterDefinition>> group : this.displayCharacterGroups().entrySet()) {
            cursorY += 22;
            List<CharacterDefinition> definitions = group.getValue();
            for (int i = 0; i < definitions.size(); i++) {
                int row = i / columns;
                int column = i % columns;
                int x = layout.gridX + column * (cardW + CharacterLayout.GRID_GAP);
                int y = cursorY + row * (cardH + CharacterLayout.GRID_GAP);
                if (this.isInside(mouseX, mouseY, x, y, cardW, cardH)) {
                    CharacterDefinition definition = definitions.get(i);
                    this.selectedCharacterId = definition.id();
                    this.selectedSkinId = this.progressEntry(definition.id()).selectedSkin();
                    if (definition.skins().stream().noneMatch(skin -> skin.id().equals(this.selectedSkinId))) {
                        this.selectedSkinId = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
                    }

                    this.syncSelectedProgress();
                    this.resetPreviewAnimationAction();
                    this.bodyScroll = 0.0F;
                    this.skinScroll = 0.0F;
                    return true;
                }
            }

            int rows = (definitions.size() + columns - 1) / columns;
            cursorY += rows * (cardH + CharacterLayout.GRID_GAP) + 8;
        }

        return false;
    }

    protected void handleBack() {
        if (this.mode == ScreenMode.DETAIL) {
            this.mode = ScreenMode.LIST;
            this.bodyScroll = 0.0F;
            this.skinScroll = 0.0F;
            return;
        }

        this.onClose();
    }

    protected boolean isOverPreview(CharacterLayout layout, double mouseX, double mouseY) {
        return this.isInside(mouseX, mouseY, layout.previewX, layout.previewY, layout.previewW, layout.previewH);
    }

    protected boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    protected void resetPreviewRotation() {
        this.previewYaw = -225.0F;
        this.previewPitch = -10.0F;
        this.previewRoll = 0.0F;
        this.previewZoom = 1.0F;
    }

    protected CharacterDefinition selectedCharacter() {
        for (CharacterDefinition definition : this.characters) {
            if (definition.id().equals(this.selectedCharacterId)) {
                return definition;
            }
        }

        return this.characters.getFirst();
    }

    protected boolean isCharacterUnlocked(CharacterDefinition definition) {
        return definition.unlockedByDefault() || this.unlockedCharacterIds.contains(definition.id());
    }

    protected boolean isSkinUnlocked(CharacterDefinition definition, CharacterSkinDefinition skin) {
        return skin.unlockedByDefault() || this.unlockedSkinIds.contains(this.skinKey(definition.id(), skin.id())) || this.unlockedSkinIds.contains(skin.id());
    }

    protected String skinKey(Identifier characterId, String skinId) {
        return characterId + "#" + skinId;
    }

    protected void syncDefaultUnlocks() {
        for (CharacterDefinition definition : this.characters) {
            CharacterProgressEntry entry = this.progressEntry(definition.id());
            if (definition.unlockedByDefault()) {
                this.unlockedCharacterIds.add(definition.id());
                entry = entry.unlock();
            }

            for (CharacterSkinDefinition skin : definition.skins()) {
                if (skin.unlockedByDefault()) {
                    this.unlockedSkinIds.add(this.skinKey(definition.id(), skin.id()));
                    entry = entry.unlockSkin(skin.id());
                }
            }

            this.progressEntries.put(definition.id(), entry);
        }
    }

    protected static Set<Identifier> decodeIdentifierSet(String encoded) {
        Set<Identifier> result = new HashSet<>();
        if (encoded == null || encoded.isBlank()) return result;
        for (String entry : encoded.split(",")) {
            try {
                result.add(Identifier.parse(entry));
            } catch (Exception ignored) {}
        }

        return result;
    }

    protected static Set<String> decodeStringSet(String encoded) {
        Set<String> result = new HashSet<>();
        if (encoded == null || encoded.isBlank()) return result;
        for (String entry : encoded.split(",")) {
            if (!entry.isBlank()) {
                result.add(entry);
            }
        }

        return result;
    }

    protected static Map<Identifier, CharacterProgressEntry> decodeProgressEntries(String encoded) {
        Map<Identifier, CharacterProgressEntry> result = new HashMap<>();
        if (encoded == null || encoded.isBlank()) return result;
        for (String entry : encoded.split(";")) {
            String[] parts = entry.split("\\|", -1);
            if (parts.length < 6) continue;
            try {
                Identifier id = Identifier.parse(parts[0]);
                boolean unlocked = Boolean.parseBoolean(parts[1]);
                String selectedSkin = parts[2].isBlank() ? "default" : parts[2];
                int level = parseInt(parts[3], CharacterProgressEntry.MIN_PVE_LEVEL);
                int experience = parseInt(parts[4], 0);
                int friendship = parseInt(parts[5], CharacterProgressEntry.MIN_FRIENDSHIP_LEVEL);
                Set<String> skins = parts.length > 6 ? decodeStringSet(parts[6]) : Set.of("default");
                result.put(id, new CharacterProgressEntry(unlocked, selectedSkin, level, experience, friendship, skins));
            } catch (Exception ignored) {}
        }

        return result;
    }

    protected static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    protected Identifier firstDisplayCharacterId() {
        return this.displayCharacters().stream().findFirst().map(CharacterDefinition::id).orElse(this.characters.getFirst().id());
    }

    protected boolean hasCharacter(Identifier characterId) {
        if (characterId == null) return false;
        return this.characters.stream().anyMatch(definition -> definition.id().equals(characterId));
    }

    protected boolean hasDisplayedCharacter(Identifier characterId) {
        if (characterId == null) return false;
        return this.displayCharacters().stream().anyMatch(definition -> definition.id().equals(characterId));
    }

    protected List<String> characterNamespaceOptions() {
        List<String> result = new ArrayList<>();
        result.add("");
        result.addAll(this.characterNamespaces());
        return result;
    }

    protected List<String> characterNamespaces() {
        List<String> namespaces = new ArrayList<>();
        for (CharacterDefinition definition : this.characters) {
            String namespace = definition.id().getNamespace();
            if (!namespaces.contains(namespace)) {
                namespaces.add(namespace);
            }
        }

        namespaces.sort((left, right) -> {
            if (left.equals(AstralCraft.MOD_ID)) return -1;
            if (right.equals(AstralCraft.MOD_ID)) return 1;
            return left.compareTo(right);
        });

        return namespaces;
    }

    protected String displayNamespaceName(String namespace) {
        if (namespace == null || namespace.isBlank()) return AstralCraft.MOD_ID;
        return namespace;
    }

    protected Map<String, List<CharacterDefinition>> displayCharacterGroups() {
        Map<String, List<CharacterDefinition>> result = new LinkedHashMap<>();
        for (String namespace : this.characterNamespaces()) {
            if (!this.characterNamespaceFilter.isBlank() && !this.characterNamespaceFilter.equals(namespace)) continue;
            List<CharacterDefinition> group = this.displayCharacters(namespace);
            if (!group.isEmpty()) {
                result.put(namespace, group);
            }
        }

        return result;
    }

    protected List<CharacterDefinition> displayCharacters() {
        List<CharacterDefinition> result = new ArrayList<>();
        for (List<CharacterDefinition> group : this.displayCharacterGroups().values()) {
            result.addAll(group);
        }

        return result;
    }

    protected List<CharacterDefinition> displayCharacters(String namespace) {
        List<CharacterDefinition> result = new ArrayList<>();
        for (CharacterDefinition definition : this.characters) {
            if (definition.id().getNamespace().equals(namespace)) {
                result.add(definition);
            }
        }

        Comparator<CharacterDefinition> fallback = Comparator.comparingInt(CharacterDefinition::sortOrder).thenComparing(value -> value.id().toString());
        if (this.sortMode == CharacterSortMode.PVE_LEVEL) {
            result.sort(Comparator.<CharacterDefinition>comparingInt(value -> this.progressEntry(value.id()).level()).reversed().thenComparing(fallback));
        } else if (this.sortMode == CharacterSortMode.FRIENDSHIP_LEVEL) {
            result.sort(Comparator.<CharacterDefinition>comparingInt(value -> this.progressEntry(value.id()).friendship()).reversed().thenComparing(fallback));
        } else {
            result.sort(fallback);
        }

        return result;
    }

    protected CharacterProgressEntry progressEntry(Identifier characterId) {
        Identifier safeId = characterId == null ? this.characters.getFirst().id() : characterId;
        CharacterProgressEntry entry = this.progressEntries.get(safeId);
        if (entry == null) {
            CharacterDefinition definition = this.characters.stream().filter(value -> value.id().equals(safeId)).findFirst().orElse(this.characters.getFirst());
            entry = definition.unlockedByDefault() ? CharacterProgressEntry.unlockedDefault() : CharacterProgressEntry.locked();
            for (CharacterSkinDefinition skin : definition.skins()) {
                if (skin.unlockedByDefault()) {
                    entry = entry.unlockSkin(skin.id());
                }
            }

            this.progressEntries.put(safeId, entry);
        }

        return entry;
    }

    protected void syncSelectedProgress() {
        CharacterProgressEntry entry = this.progressEntry(this.selectedCharacterId);
        this.level = entry.level();
        this.experience = entry.experience();
        this.friendship = entry.friendship();
    }

    protected AstralFancyButton.ButtonStyle backButtonStyle() {
        return AstralFancyButton.ButtonStyle.button(0xCC2E74FF).withTextScale(1.05F)
                .withTextShadowColors(0x00000000, 0x00000000, 0x00000000)
                .withBorderColors(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF)
                .withBoxMetrics(2, 2, 3, 3);
    }

    protected AstralFancyButton.ButtonStyle pinkButtonStyle() {
        return AstralFancyButton.ButtonStyle.button(0xFFE83CA8).withTextScale(1.0F)
                .withTextShadowColors(0x00000000, 0x00000000, 0x00000000)
                .withBorderColors(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF)
                .withBoxMetrics(2, 2, 3, 3);
    }

    protected AstralFancyButton.ButtonStyle selectedButtonStyle() {
        return AstralFancyButton.ButtonStyle.button(0xFFE83CA8)
                .withBackgroundGradientColors(0xFFE83CA8, 0xFFC92588, 0xFFFF77C8, 0xFFE83CA8, 0xFF92FF22, 0xFF57C800)
                .withTextColors(0xFFFFFFFF, 0xFF101018, 0xFF101018)
                .withTextShadowColors(0x00000000, 0x00000000, 0x00000000)
                .withBorderColors(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF)
                .withBoxMetrics(2, 2, 3, 3);
    }

    protected int drawHeader(GuiGraphicsExtractor graphics, MutableComponent component, int x, int y, int color, int maxWidth) {
        graphics.fill(x - 6, y - 2, x + maxWidth, y + 12, 0xAA000000);
        graphics.text(this.font, this.ellipsize(component, maxWidth - 6), x, y, color, false);
        return y + 16;
    }

    protected int drawLine(GuiGraphicsExtractor graphics, MutableComponent component, int x, int y, int color, int maxWidth) {
        graphics.text(this.font, this.ellipsize(component, maxWidth), x, y, color, false);
        return y + 12;
    }

    protected int drawWrapped(GuiGraphicsExtractor graphics, MutableComponent component, int x, int y, int color, int maxWidth) {
        return AstralInlineTextFormatter.draw(graphics, this.font, component, x, y, maxWidth, color, false);
    }

    protected List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String normalized = text == null ? "" : text.replace("\\n", "\n");
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (character == '\n') {
                lines.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(character);
            if (this.font.width(current.toString()) >= maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
        }

        lines.add(current.toString());
        while (!lines.isEmpty() && lines.getLast().isEmpty()) {
            lines.removeLast();
        }

        return lines;
    }

    protected MutableComponent ellipsize(MutableComponent input, int maxWidth) {
        String text = input.getString();
        if (this.font.width(text) <= maxWidth) return input;
        String suffix = "...";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (this.font.width(out.toString()) + this.font.width(suffix) >= maxWidth) break;
            out.append(text.charAt(i));
        }

        return Component.literal(out + suffix);
    }

    protected void renderPreviewAnimationControls(GuiGraphicsExtractor graphics, CharacterLayout layout, int mouseX, int mouseY) {
        int y = layout.previewY + layout.previewH - 26;
        int playX = layout.previewX + 8;
        int playW = Math.clamp(layout.previewW / 3, 34, 46);
        int dropX = playX + playW + 5;
        int dropW = Math.max(42, layout.previewX + layout.previewW - dropX - 8);
        boolean playHover = this.isInside(mouseX, mouseY, playX, y, playW, 18);
        boolean dropHover = this.isInside(mouseX, mouseY, dropX, y, dropW, 18);
        MutableComponent playText = Component.translatable(this.previewAnimationPlaying ? "gui.astral_craft.character_settings.animation.pause" : "gui.astral_craft.character_settings.animation.play");
        this.renderFancyButton(graphics, playText, playX, y, playW, 18, false, playHover, this.backButtonStyle().withTextScale(0.88F));
        MutableComponent actionText = Component.literal(this.previewAnimationAction());
        this.renderFancyButton(graphics, this.ellipsize(actionText, dropW - 14), dropX, y, dropW, 18, this.previewAnimationDropdownOpen, dropHover, this.pinkButtonStyle().withTextScale(0.88F));
        if (this.previewAnimationDropdownOpen) {
            List<String> actions = this.availablePreviewAnimations();
            int rowH = 17;
            int menuH = Math.min(actions.size(), 6) * rowH;
            int menuY = y - menuH - 3;
            AstralFancyButton.renderOutlinedBox(graphics, dropX, menuY, dropW, menuH, 0xEE11111A, 0xFFFFFFFF, 0xFF101018, 1, 1);
            for (int i = 0; i < Math.min(actions.size(), 6); i++) {
                int rowY = menuY + i * rowH;
                String action = actions.get(i);
                boolean selected = action.equals(this.previewAnimationAction());
                boolean hovered = this.isInside(mouseX, mouseY, dropX, rowY, dropW, rowH);
                int fill = selected ? 0x8832B900 : hovered ? 0x66373748 : 0x00000000;
                if ((fill >>> 24) != 0) {
                    graphics.fill(dropX + 2, rowY + 1, dropX + dropW - 2, rowY + rowH - 1, fill);
                }

                graphics.text(this.font, this.ellipsize(Component.literal(action), dropW - 8), dropX + 5, rowY + 5, selected ? 0xFFFFFFFF : 0xFFD8D8E8, false);
            }
        }
    }

    protected boolean handlePreviewAnimationClick(CharacterLayout layout, double mouseX, double mouseY) {
        int y = layout.previewY + layout.previewH - 26;
        int playX = layout.previewX + 8;
        int playW = Math.clamp(layout.previewW / 3, 34, 46);
        int dropX = playX + playW + 5;
        int dropW = Math.max(42, layout.previewX + layout.previewW - dropX - 8);
        if (this.previewAnimationDropdownOpen) {
            List<String> actions = this.availablePreviewAnimations();
            int rowH = 17;
            int menuH = Math.min(actions.size(), 6) * rowH;
            int menuY = y - menuH - 3;
            for (int i = 0; i < Math.min(actions.size(), 6); i++) {
                int rowY = menuY + i * rowH;
                if (this.isInside(mouseX, mouseY, dropX, rowY, dropW, rowH)) {
                    this.previewAnimationAction = actions.get(i);
                    this.previewAnimationTimeSeconds = 0.0F;
                    this.previewAnimationPlaying = true;
                    this.previewAnimationDropdownOpen = false;
                    return true;
                }
            }
        }

        if (this.isInside(mouseX, mouseY, playX, y, playW, 18)) {
            this.previewAnimationPlaying = !this.previewAnimationPlaying;
            this.previewAnimationDropdownOpen = false;
            return true;
        }

        if (this.isInside(mouseX, mouseY, dropX, y, dropW, 18)) {
            this.previewAnimationDropdownOpen = !this.previewAnimationDropdownOpen;
            return true;
        }

        if (this.previewAnimationDropdownOpen && this.isOverPreview(layout, mouseX, mouseY)) {
            this.previewAnimationDropdownOpen = false;
            return true;
        }

        return false;
    }

    protected List<String> availablePreviewAnimations() {
        List<String> names = AstralGeoAnimationManager.INSTANCE.animationNames(this.selectedCharacter().animationSetKey());
        return names.isEmpty() ? List.of("idle") : names;
    }

    protected String previewAnimationAction() {
        if (this.previewAnimationAction == null || this.previewAnimationAction.isBlank()) {
            return this.selectedCharacter().previewAction();
        }

        return this.previewAnimationAction;
    }

    protected void resetPreviewAnimationAction() {
        String action = this.selectedCharacter().previewAction();
        List<String> actions = this.availablePreviewAnimations();
        this.previewAnimationAction = actions.contains(action) ? action : actions.getFirst();
        this.previewAnimationTimeSeconds = 0.0F;
        this.previewAnimationDropdownOpen = false;
        this.previewAnimationPlaying = true;
    }

    protected void renderEntityModel(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, float yaw, float pitch, float roll, float scaleMultiplier) {
        EntityRenderState renderState = this.extractEntityRenderState(entity);
        if (renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 0.0F;
            livingState.yRot = 0.0F;
            livingState.xRot = 0.0F;
            livingState.scale = 1.0F;
        }

        float boxWidth = Math.max(0.35F, renderState.boundingBoxWidth);
        float boxHeight = Math.max(1.2F, renderState.boundingBoxHeight);
        float viewWidth = Math.max(1.0F, x1 - x0);
        float viewHeight = Math.max(1.0F, y1 - y0);
        float scale = Math.min(viewWidth / (boxWidth * 1.35F), viewHeight / (boxHeight * 1.10F)) * scaleMultiplier;
        scale = Mth.clamp(scale, 8.0F, 94.0F);
        AstralGeoPose pose = renderState instanceof AstralCharacterRenderState astralState ? astralState.rootPose : AstralGeoPose.IDENTITY;
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.toRadians(180.0F))
                .rotateX((float) Math.toRadians(pitch + pose.rotation().x()))
                .rotateY((float) Math.toRadians(yaw + pose.rotation().y()))
                .rotateZ((float) Math.toRadians(roll + pose.rotation().z()));
        Vector3f translation = new Vector3f(pose.position().x() / 16.0F, boxHeight * 0.48F - pose.position().y() / 16.0F, pose.position().z() / 16.0F);
        graphics.entity(renderState, scale, translation, rotation, null, x0, y0, x1, y1);
    }

    protected EntityRenderState extractEntityRenderState(LivingEntity entity) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }

    protected LivingEntity previewEntity() {
        CharacterDefinition definition = this.selectedCharacter();
        return this.entityFor(definition, this.selectedSkinId);
    }

    protected LivingEntity entityFor(CharacterDefinition definition, String skinId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || definition == null) return null;
        String safeSkin = skinId == null || skinId.isBlank() ? this.skinIdFor(definition) : skinId;
        String key = definition.id() + "#" + safeSkin;
        AstralCharacterEntity entity = this.previewEntities.get(key);
        if (entity == null) {
            entity = new AstralCharacterEntity(AstralEntities.ASTRAL_CHARACTER.get(), minecraft.level);
            this.previewEntities.put(key, entity);
        }

        CharacterProgressEntry progress = this.progressEntry(definition.id());
        entity.setCharacterId(definition.id());
        entity.setSkinId(safeSkin);
        entity.setCharacterLevel(progress.level());
        entity.setFriendship(progress.friendship());
        entity.setAnimationAction(this.previewAnimationAction());
        entity.tickCount = Math.max(0, Math.round(this.previewAnimationTimeSeconds * 20.0F));
        return entity;
    }

    protected String skinIdFor(CharacterDefinition definition) {
        if (definition == null) return "default";
        String skinId = this.progressEntry(definition.id()).selectedSkin();
        if (definition.skins().stream().noneMatch(skin -> skin.id().equals(skinId))) {
            return definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
        }

        return skinId;
    }

    protected CharacterLayout layout() {
        return CharacterLayout.create(this.width, this.height, this.mode, this.mainTab);
    }

    protected int characterGridColumns(CharacterLayout layout) {
        return Math.max(1, (layout.gridW + CharacterLayout.GRID_GAP) / (layout.characterCardW + CharacterLayout.GRID_GAP));
    }

    protected float maxCharacterScroll(CharacterLayout layout) {
        int columns = this.characterGridColumns(layout);
        int content = 0;
        for (List<CharacterDefinition> group : this.displayCharacterGroups().values()) {
            int rows = (group.size() + columns - 1) / columns;
            content += 22;
            if (rows > 0) {
                content += rows * (layout.characterCardH + CharacterLayout.GRID_GAP) - CharacterLayout.GRID_GAP;
            }

            content += 8;
        }

        return Math.max(0.0F, content - layout.gridH);
    }

    protected float maxSkinScroll(CharacterLayout layout) {
        CharacterDefinition definition = this.selectedCharacter();
        int listWidth = Math.max(10, layout.bodyW - 28);
        int content = definition.skins().size() * (layout.skinCardW + CharacterLayout.GRID_GAP) - CharacterLayout.GRID_GAP;
        return Math.max(0.0F, content - listWidth);
    }

    protected float maxBodyScroll(CharacterLayout layout) {
        if (this.mode != ScreenMode.DETAIL) return 0.0F;
        CharacterDetailPage page = this.currentDetailPage();
        return page.usesSkinScroll() ? 0.0F : page.maxScroll(layout);
    }

    protected boolean shouldRenderProfileSectionHeader(CharacterProfileSection section) {
        return section.titleKey() != null && !section.titleKey().isBlank();
    }

    protected int wrappedHeight(MutableComponent component, int maxWidth) {
        return AstralInlineTextFormatter.height(this.font, component, maxWidth, 0xFFFFFFFF);
    }

    protected void clampScrolls() {
        CharacterLayout layout = this.layout();
        this.characterScroll = Mth.clamp(this.characterScroll, 0.0F, this.maxCharacterScroll(layout));
        this.bodyScroll = Mth.clamp(this.bodyScroll, 0.0F, this.maxBodyScroll(layout));
        this.skinScroll = Mth.clamp(this.skinScroll, 0.0F, this.maxSkinScroll(layout));
    }

    protected void registerDetailPages() {
        this.detailPages.put(MainTab.ARCHIVE, new ArchiveDetailPage(this));
        this.detailPages.put(MainTab.CONTRACT, new ContractDetailPage(this));
        this.detailPages.put(MainTab.SKINS, new SkinsDetailPage(this));
        this.detailPages.put(MainTab.EMOTES, new EmotesDetailPage(this));
    }

    protected CharacterDetailPage currentDetailPage() {
        return this.detailPages.getOrDefault(this.mainTab, this.detailPages.get(MainTab.ARCHIVE));
    }

    public Font font() {
        return this.font;
    }

    protected boolean tryStartScrollbarDrag(CharacterLayout layout, double mouseX, double mouseY) {
        if (this.mode == ScreenMode.LIST) {
            float maxScroll = this.maxCharacterScroll(layout);
            if (AstralVerticalScrollbar.contains(mouseX, mouseY, layout.gridX + layout.gridW + 2, layout.gridY, layout.gridH, maxScroll)) {
                this.draggingScrollbar = ScrollTarget.CHARACTERS;
                this.updateScrollbarDrag(layout, mouseX, mouseY);
                return true;
            }
        } else {
            CharacterDetailPage page = this.currentDetailPage();
            if (page.usesSkinScroll()) {
                int scrollX = layout.bodyX + 14;
                int scrollY = layout.bodyY + layout.bodyH - 18;
                int scrollW = Math.max(10, layout.bodyW - 28);
                if (AstralHorizontalScrollbar.contains(mouseX, mouseY, scrollX, scrollY, scrollW, this.maxSkinScroll(layout))) {
                    this.draggingScrollbar = ScrollTarget.SKINS;
                    this.updateScrollbarDrag(layout, mouseX, mouseY);
                    return true;
                }
            } else {
                int scrollX = layout.bodyX + layout.bodyW - 5;
                int scrollY = layout.bodyY + 38;
                int scrollH = Math.max(10, layout.bodyH - 50);
                float maxScroll = page.maxScroll(layout);
                if (AstralVerticalScrollbar.contains(mouseX, mouseY, scrollX, scrollY, scrollH, maxScroll)) {
                    this.draggingScrollbar = ScrollTarget.BODY;
                    this.updateScrollbarDrag(layout, mouseX, mouseY);
                    return true;
                }
            }
        }

        return false;
    }

    protected void updateScrollbarDrag(CharacterLayout layout, double mouseX, double mouseY) {
        if (this.draggingScrollbar == ScrollTarget.CHARACTERS) {
            this.characterScroll = AstralVerticalScrollbar.scrollFromMouse(mouseY, layout.gridY, layout.gridH, this.maxCharacterScroll(layout));
        } else if (this.draggingScrollbar == ScrollTarget.SKINS) {
            this.skinScroll = AstralHorizontalScrollbar.scrollFromMouse(mouseX, layout.bodyX + 14, Math.max(10, layout.bodyW - 28), this.maxSkinScroll(layout));
        } else if (this.draggingScrollbar == ScrollTarget.BODY) {
            this.bodyScroll = AstralVerticalScrollbar.scrollFromMouse(mouseY, layout.bodyY + 38, Math.max(10, layout.bodyH - 50), this.currentDetailPage().maxScroll(layout));
        }
    }

    public enum ScreenMode {
        LIST,
        DETAIL
    }

    public enum ScrollTarget {
        NONE,
        CHARACTERS,
        BODY,
        SKINS
    }

    public enum CharacterSortMode {

        DEFAULT("gui.astral_craft.character_settings.sort.default"),
        PVE_LEVEL("gui.astral_craft.character_settings.sort.pve"),
        FRIENDSHIP_LEVEL("gui.astral_craft.character_settings.sort.friendship");

        private final String translationKey;

        CharacterSortMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }

    }

    public enum MainTab {

        ARCHIVE("gui.astral_craft.character_settings.main.archive"),
        CONTRACT("gui.astral_craft.character_settings.main.contract"),
        SKINS("gui.astral_craft.character_settings.main.skins"),
        EMOTES("gui.astral_craft.character_settings.main.emotes");

        private final String translationKey;

        MainTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }

    }

    public enum ArchiveTab {

        SKILLS("gui.astral_craft.character_settings.archive.skills", 0xFFFFF2A0),
        LEVEL("gui.astral_craft.character_settings.archive.level", 0xFF8CFF20),
        POTENTIAL("gui.astral_craft.character_settings.archive.potential", 0xFFDFA0FF),
        PROFILE("gui.astral_craft.character_settings.archive.profile", 0xFFFFA0FF);

        private final String translationKey;
        private final int color;

        ArchiveTab(String translationKey, int color) {
            this.translationKey = translationKey;
            this.color = color;
        }

        public String titleKey() {
            return this.translationKey;
        }

        public int headerColor() {
            return this.color;
        }

    }

}
