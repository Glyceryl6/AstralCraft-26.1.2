package com.astral_craft.client.gui.board;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardDeveloperService;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterStatsDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.c2s.BoardCharacterSelectionPayload;
import com.astral_craft.common.network.c2s.BoardDeveloperConfigPayload;
import com.astral_craft.common.network.s2c.BoardCharacterAvailability;
import com.astral_craft.common.network.s2c.BoardCharacterSelectionEntry;
import com.astral_craft.common.network.s2c.OpenBoardCharacterSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardDeveloperPayload;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/** Board lobby selector and the development-only live bot editor. */
public class BoardCharacterSelectionScreen extends Screen {

    private static final int MARGIN = 10;
    private static final int GAP = 5;
    private static final int CHARACTER_W = 48;
    private static final int CHARACTER_H = 40;
    private static final int SKIN_W = 46;
    private static final int SKIN_H = 44;
    private static final int SLOT_COUNT = 4;
    private static final int BOT_COUNT = 3;
    private static final int DEVELOPER_TAB_H = 20;
    private static final int DEVELOPER_ROW_H = 22;
    private static final int DEVELOPER_CARD_TILE_W = 84;
    private static final int DEVELOPER_CARD_CONTROL_H = 20;
    private static final int DEVELOPER_CARD_TILE_H = HandCardRenderHelper.FRAMED_CARD_H + DEVELOPER_CARD_CONTROL_H + 4;
    private static final int DEVELOPER_CARD_BUTTON_W = 20;
    private static final int DEVELOPER_CARD_INPUT_W = 34;
    private final UUID boardId;
    private final List<CharacterDefinition> characters;
    private final Set<Identifier> occupied;
    private final Map<Identifier, BoardCharacterAvailability> availability = new HashMap<>();
    private final AstralCharacterEntity[] slotPreviews = new AstralCharacterEntity[SLOT_COUNT];
    private final boolean developerMode;
    private final boolean developerLive;
    private final List<Identifier> developerCardIds;
    private final Set<Identifier> developerBotSelectable;
    private final String developerHumanName;
    private final BotDraft[] developerBots = new BotDraft[BOT_COUNT];
    private final Map<Identifier, EditBox> developerCardCountBoxes = new LinkedHashMap<>();
    private final Map<Identifier, DeveloperCardView> developerCardViews = new LinkedHashMap<>();
    private Identifier developerHumanCharacter;
    private Identifier developerHumanSkin;
    private List<BoardCharacterSelectionEntry> lobbyEntries;
    private Identifier selectedCharacter;
    private String selectedSkin;
    private int timeoutTicks;
    private int timeoutDurationTicks;
    private boolean submitted;
    private boolean selectionLocked;
    private float characterScroll;
    private float skinScroll;
    private float developerScroll;
    private int selectedDeveloperSlot;
    private boolean syncingCardInputs;
    private DeveloperTab developerTab = DeveloperTab.CHARACTER;

    public BoardCharacterSelectionScreen(OpenBoardCharacterSelectionPayload payload) {
        super(Component.translatable("gui.astral_craft.board.character_select"));
        this.boardId = payload.boardId();
        List<CharacterDefinition> definitions = payload.characters();
        this.characters = definitions.isEmpty() ? List.of(CharacterManager.INSTANCE.defaultCharacter()) : definitions;
        ClientCharacterDefinitionCache.INSTANCE.replace(this.characters);
        this.occupied = new HashSet<>(payload.occupiedCharacterIds());
        this.replaceAvailability(payload.availability());
        this.lobbyEntries = payload.lobbyEntries();
        this.selectionLocked = payload.selectionLocked();
        this.submitted = this.selectionLocked;
        this.selectedCharacter = payload.selectedCharacterId();
        this.selectedSkin = payload.selectedSkinId().getPath();
        this.timeoutTicks = Math.max(1, payload.timeoutTicks());
        this.timeoutDurationTicks = Math.max(1, payload.timeoutDurationTicks());
        this.developerMode = false;
        this.developerLive = false;
        this.developerCardIds = List.of();
        this.developerBotSelectable = Set.of();
        this.developerHumanName = "";
        this.developerHumanCharacter = this.selectedCharacter;
        this.developerHumanSkin = BoardParticipant.skinIdentifier(this.selectedCharacter, this.selectedSkin);
        this.ensureAvailableSelection();
        this.ensureSkin();
    }

    public BoardCharacterSelectionScreen(OpenBoardDeveloperPayload payload) {
        super(Component.translatable("gui.astral_craft.board.developer.title"));
        this.boardId = payload.boardId();
        List<CharacterDefinition> definitions = payload.characters();
        this.characters = definitions.isEmpty() ? List.of(CharacterManager.INSTANCE.defaultCharacter()) : definitions;
        ClientCharacterDefinitionCache.INSTANCE.replace(this.characters);
        this.occupied = new HashSet<>();
        this.lobbyEntries = List.of();
        this.selectionLocked = false;
        this.submitted = false;
        this.timeoutTicks = 1;
        this.timeoutDurationTicks = 1;
        this.developerMode = true;
        this.developerLive = payload.live();
        this.developerCardIds = payload.cardIds();
        this.developerBotSelectable = new HashSet<>(payload.botSelectableCharacterIds());
        this.developerHumanName = payload.human().playerName();
        this.developerHumanCharacter = payload.human().characterId();
        this.developerHumanSkin = payload.human().skinId();
        for (int index = 0; index < Math.min(BOT_COUNT, payload.bots().size()); index++) {
            this.developerBots[index] = new BotDraft(payload.bots().get(index));
        }
        this.selectedDeveloperSlot = this.developerLive ? 1 : 0;
        this.syncSelectedDeveloperSlot();
        this.prepareDeveloperCardViews();
    }

    public static void open(OpenBoardCharacterSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (payload.characters().isEmpty()) {
                if (minecraft.screen instanceof BoardCharacterSelectionScreen screen
                        && screen.boardId.equals(payload.boardId())) minecraft.setScreen(null);
                return;
            }

            if (payload.refresh() && minecraft.screen instanceof BoardCharacterSelectionScreen screen
                    && !screen.developerMode && screen.boardId.equals(payload.boardId())) {
                screen.refresh(payload);
                return;
            }

            minecraft.setScreen(new BoardCharacterSelectionScreen(payload));
        });
    }

    public static void openDeveloper(OpenBoardDeveloperPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardCharacterSelectionScreen(payload)));
    }

    private void refresh(OpenBoardCharacterSelectionPayload payload) {
        this.occupied.clear();
        this.occupied.addAll(payload.occupiedCharacterIds());
        this.replaceAvailability(payload.availability());
        this.lobbyEntries = payload.lobbyEntries();
        this.timeoutTicks = Math.max(1, payload.timeoutTicks());
        this.timeoutDurationTicks = Math.max(1, payload.timeoutDurationTicks());
        this.selectionLocked = payload.selectionLocked();
        this.submitted = this.selectionLocked;
        this.selectedCharacter = payload.selectedCharacterId();
        this.selectedSkin = payload.selectedSkinId().getPath();
        this.ensureAvailableSelection();
        this.ensureSkin();
    }

    @Override
    protected void init() {
        super.init();
        Layout layout = this.layout();
        this.characterScroll = Mth.clamp(this.characterScroll, 0.0F, this.maximumCharacterScroll(layout));
        this.skinScroll = Mth.clamp(this.skinScroll, 0.0F, this.maximumSkinScroll(layout));
        this.developerScroll = Mth.clamp(this.developerScroll, 0.0F, this.maximumDeveloperScroll(layout));
        if (this.developerMode) this.createDeveloperCardInputs();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.developerMode && this.timeoutTicks > 0) this.timeoutTicks--;
        for (AstralCharacterEntity preview : this.slotPreviews) {
            if (preview != null) preview.tickCount++;
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        if (this.developerMode) this.updateDeveloperCardInputs(layout);
        graphics.fill(0, 0, this.width, this.height, 0xE6090912);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelRight(), layout.panelBottom(), 0xCC11111C);
        graphics.text(this.font, this.title, layout.panelX() + 8, layout.panelY() + 6, 0xFFFFFFFF, false);
        this.renderLobbySlots(graphics, layout);
        if (this.developerMode) this.renderDeveloper(graphics, layout, mouseX, mouseY);
        else this.renderNormalSelection(graphics, layout, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderNormalSelection(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.characters"),
                layout.gridX(), layout.gridTitleY(), 0xFFBFC8FF, false);
        this.renderCharacterGrid(graphics, layout, mouseX, mouseY);
        this.renderSkinGrid(graphics, layout, mouseX, mouseY);
        boolean hover = inside(mouseX, mouseY, layout.buttonX(), layout.buttonY(), layout.buttonW(), layout.buttonH());
        boolean waiting = this.selectionLocked || this.submitted;
        Component confirm = Component.translatable(waiting
                ? "gui.astral_craft.board.character_waiting" : "gui.astral_craft.board.confirm");
        AstralFancyButton.renderButton(graphics, this.font, confirm, layout.buttonX(), layout.buttonY(),
                layout.buttonW(), layout.buttonH(), waiting, hover && !waiting,
                ButtonStyle.button(waiting ? 0xFF4C7658 : 0xFFD64B91));
        BoardDecisionProgressBar.render(graphics, this.font, this.selectedCharacter,
                BoardParticipant.skinIdentifier(this.selectedCharacter, this.selectedSkin),
                this.timeoutTicks, this.timeoutDurationTicks, this.width / 2,
                this.height - 13, Math.min(270, this.width - 44));
    }

    private void renderDeveloper(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        this.renderDeveloperTabs(graphics, layout, mouseX, mouseY);
        if (this.developerTab == DeveloperTab.CHARACTER) {
            graphics.text(this.font, Component.translatable("gui.astral_craft.board.developer.characters"),
                    layout.gridX(), layout.gridTitleY(), 0xFFBFC8FF, false);
            this.renderCharacterGrid(graphics, layout, mouseX, mouseY);
            this.renderSkinGrid(graphics, layout, mouseX, mouseY);
        } else if (this.developerTab == DeveloperTab.STATS) {
            this.renderDeveloperStats(graphics, layout, mouseX, mouseY);
        } else {
            this.renderDeveloperCards(graphics, layout, mouseX, mouseY);
        }

        boolean hover = inside(mouseX, mouseY, layout.buttonX(), layout.buttonY(), layout.buttonW(), layout.buttonH());
        Component apply = Component.translatable(this.developerLive
                ? "gui.astral_craft.board.developer.apply" : "gui.astral_craft.board.developer.start");
        AstralFancyButton.renderButton(graphics, this.font, apply, layout.buttonX(), layout.buttonY(),
                layout.buttonW(), layout.buttonH(), false, hover, ButtonStyle.button(0xFFD64B91));
        Component hint = Component.translatable(this.developerLive
                ? "gui.astral_craft.board.developer.live_hint" : "gui.astral_craft.board.developer.setup_hint");
        String shownHint = this.font.plainSubstrByWidth(hint.getString(), Math.max(20, layout.buttonX() - layout.gridX() - 8));
        graphics.text(this.font, shownHint, layout.gridX(), layout.buttonY() + 8, 0xFFAEB7DC, false);
        if (this.developerTab == DeveloperTab.STATS) {
            Component adjustHint = Component.translatable("gui.astral_craft.board.developer.adjust_hint");
            String shownAdjustHint = this.font.plainSubstrByWidth(adjustHint.getString(),
                    Math.max(20, layout.gridRight() - layout.gridX()));
            graphics.text(this.font, shownAdjustHint, layout.gridX(), layout.buttonY() - 11, 0xFF8E96B7, false);
        } else if (this.developerTab == DeveloperTab.CARDS) {
            Component cardsHint = Component.translatable("gui.astral_craft.board.developer.cards_hint");
            String shownCardsHint = this.font.plainSubstrByWidth(cardsHint.getString(),
                    Math.max(20, layout.gridRight() - layout.gridX()));
            graphics.text(this.font, shownCardsHint, layout.gridX(), layout.buttonY() - 11, 0xFF8E96B7, false);
        }
    }

    private void renderDeveloperTabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int width = Math.max(1, (layout.gridRight() - layout.gridX() - GAP * 2) / DeveloperTab.values().length);
        for (int index = 0; index < DeveloperTab.values().length; index++) {
            DeveloperTab tab = DeveloperTab.values()[index];
            int x = layout.gridX() + index * (width + GAP);
            boolean enabled = this.selectedDeveloperSlot > 0 || tab == DeveloperTab.CHARACTER;
            boolean hovered = enabled && inside(mouseX, mouseY, x, layout.developerTabY(), width, DEVELOPER_TAB_H);
            AstralFancyButton.renderTab(graphics, this.font, Component.translatable(tab.translationKey), x,
                    layout.developerTabY(), width, DEVELOPER_TAB_H, this.developerTab == tab, hovered,
                    enabled ? 0xFFD64B91 : 0xFF59596A);
        }
    }

    private void renderLobbySlots(GuiGraphicsExtractor graphics, Layout layout) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int x = layout.slotX(slot);
            int y = layout.slotY();
            BoardCharacterSelectionEntry entry = this.developerMode ? this.developerEntry(slot) : this.entry(slot);
            boolean selectedDeveloperSlot = this.developerMode && slot == this.selectedDeveloperSlot;
            int border = selectedDeveloperSlot ? 0xFFD64B91
                    : entry != null && entry.confirmed() ? 0xFFFFD34E : 0xFF626273;
            graphics.fill(x, y, x + layout.slotW(), y + layout.slotH(), 0xD0080810);
            graphics.fill(x, y, x + layout.slotW(), y + 2, border);
            graphics.fill(x, y + layout.slotH() - 2, x + layout.slotW(), y + layout.slotH(), border);
            graphics.fill(x, y, x + 2, y + layout.slotH(), border);
            graphics.fill(x + layout.slotW() - 2, y, x + layout.slotW(), y + layout.slotH(), border);
            if (entry != null && entry.selected()) {
                AstralCharacterEntity preview = this.slotPreview(slot, entry);
                BoardScreenEntityRenderer.render(graphics, preview, x + 4, y + 4,
                        x + layout.slotW() - 4, y + layout.slotH() - 17, -225.0F);
            } else {
                Component waiting = Component.literal("?");
                graphics.text(this.font, waiting, x + layout.slotW() / 2 - this.font.width(waiting) / 2,
                        y + layout.slotH() / 2 - 8, 0xFF777784, true);
            }

            if (!this.developerMode && entry != null && entry.confirmed()) {
                Component ok = Component.literal("OK").withStyle(ChatFormatting.BOLD);
                float scale = 1.45F;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + layout.slotW() - 5, y + 6);
                graphics.pose().scale(scale, scale);
                graphics.text(this.font, ok, -this.font.width(ok), 0, 0xFFFFE06C, true);
                graphics.pose().popMatrix();
            }

            Component label = this.developerMode && slot > 0
                    ? Component.translatable("gui.astral_craft.board.developer.bot", slot)
                    : Component.literal(entry == null ? "" : entry.playerName());
            String shown = this.font.plainSubstrByWidth(label.getString(), layout.slotW() - 6);
            graphics.fill(x + 2, y + layout.slotH() - 16, x + layout.slotW() - 2,
                    y + layout.slotH() - 2, 0xC0000000);
            graphics.text(this.font, shown, x + layout.slotW() / 2 - this.font.width(shown) / 2,
                    y + layout.slotH() - 13, 0xFFFFFFFF, false);
        }
    }

    private void renderCharacterGrid(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.enableScissor(layout.gridX(), layout.gridTop(), layout.gridRight(), layout.gridBottom());
        for (int index = 0; index < this.characters.size(); index++) {
            CharacterDefinition definition = this.characters.get(index);
            CardPosition position = layout.characterPosition(index, this.characterScroll);
            if (position.y() + CHARACTER_H < layout.gridTop() || position.y() > layout.gridBottom()) continue;
            boolean selectedCard = definition.id().equals(this.selectedCharacter);
            boolean occupiedCard = this.developerMode
                    ? this.characterUsedByOtherSeat(definition.id()) : this.occupied.contains(definition.id());
            boolean unlocked = this.developerMode || this.characterUnlocked(definition.id());
            boolean developerAllowed = !this.developerMode || this.selectedDeveloperSlot == 0
                    || this.developerBotSelectable.contains(definition.id());
            boolean unavailable = this.developerMode ? occupiedCard || !developerAllowed
                    : this.selectionLocked || this.submitted || occupiedCard || !unlocked;
            boolean hovered = inside(mouseX, mouseY, position.x(), position.y(), CHARACTER_W, CHARACTER_H);
            AstralFancyButton.renderIconFrame(graphics, position.x(), position.y(), CHARACTER_W, CHARACTER_H,
                    selectedCard, hovered && !unavailable);
            if (occupiedCard) {
                int frameColor = 0xFFFFC65C;
                graphics.fill(position.x(), position.y(), position.x() + CHARACTER_W, position.y() + 2, frameColor);
                graphics.fill(position.x(), position.y() + CHARACTER_H - 2, position.x() + CHARACTER_W, position.y() + CHARACTER_H, frameColor);
                graphics.fill(position.x(), position.y(), position.x() + 2, position.y() + CHARACTER_H, frameColor);
                graphics.fill(position.x() + CHARACTER_W - 2, position.y(), position.x() + CHARACTER_W, position.y() + CHARACTER_H, frameColor);
                if (!selectedCard) graphics.fill(position.x(), position.y(), position.x() + CHARACTER_W, position.y() + CHARACTER_H, 0x882A2026);
            }

            String skinId = selectedCard ? this.selectedSkin : this.preferredSkin(definition.id());
            int alpha = occupiedCard && !selectedCard || !developerAllowed ? 90 : 255;
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, definition.id(), skinId,
                    position.x() + 12, position.y() + 2, 24, alpha, !unlocked);
            if (!unlocked) graphics.fill(position.x(), position.y(), position.x() + CHARACTER_W,
                    position.y() + CHARACTER_H, 0x66101010);
            Component characterName = Component.translatable(definition.getDescriptionId());
            int textColor = !unlocked || occupiedCard && !selectedCard || !developerAllowed ? 0xFF7C7478 : 0xFFFFFFFF;
            graphics.text(this.font, this.font.plainSubstrByWidth(characterName.getString(), CHARACTER_W - 4),
                    position.x() + 2, position.y() + 29, textColor, false);
        }
        graphics.disableScissor();
    }

    private void renderSkinGrid(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.skins"), layout.gridX(), layout.skinTitleY(), 0xFFBFC8FF, false);
        graphics.enableScissor(layout.gridX(), layout.skinY(), layout.skinRight(), layout.skinBottom());
        List<CharacterSkinDefinition> skins = this.selected().skins();
        for (int index = 0; index < skins.size(); index++) {
            CharacterSkinDefinition skin = skins.get(index);
            int x = layout.gridX() + index * (SKIN_W + GAP) - Math.round(this.skinScroll);
            boolean active = skin.id().equals(this.selectedSkin);
            boolean unlocked = this.developerMode || this.skinUnlocked(this.selected().id(), skin.id());
            boolean hovered = unlocked && !this.selectionLocked && !this.submitted
                    && inside(mouseX, mouseY, x, layout.skinY(), SKIN_W, SKIN_H);
            AstralFancyButton.renderIconFrame(graphics, x, layout.skinY(), SKIN_W, SKIN_H, active, hovered);
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, this.selected().id(), skin.id(),
                    x + 8, layout.skinY() + 3, 30, 255, !unlocked);
            if (!unlocked) graphics.fill(x, layout.skinY(), x + SKIN_W, layout.skinY() + SKIN_H, 0x66101010);
            graphics.text(this.font, this.font.plainSubstrByWidth(Component.translatable(skin.nameKey()).getString(), SKIN_W - 4),
                    x + 2, layout.skinY() + 33, unlocked ? 0xFFFFFFFF : 0xFF777777, false);
        }
        graphics.disableScissor();
    }

    private void renderDeveloperStats(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        BotDraft draft = this.selectedBotDraft();
        if (draft == null) return;
        int top = layout.gridTitleY();
        int bottom = layout.buttonY() - 18;
        int resetW = Math.min(130, layout.gridRight() - layout.gridX());
        boolean resetHover = inside(mouseX, mouseY, layout.gridX(), top, resetW, DEVELOPER_TAB_H);
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.developer.reset_stats"), layout.gridX(), top,
                resetW, DEVELOPER_TAB_H, false, resetHover, ButtonStyle.button(0xFF646477));
        int listTop = top + DEVELOPER_TAB_H + GAP;
        graphics.enableScissor(layout.gridX(), listTop, layout.gridRight(), bottom);
        for (int index = 0; index < StatField.values().length; index++) {
            StatField field = StatField.values()[index];
            int y = listTop + index * (DEVELOPER_ROW_H + 2) - Math.round(this.developerScroll);
            if (y + DEVELOPER_ROW_H < listTop || y > bottom) continue;
            this.renderStatRow(graphics, layout, draft, field, y, mouseX, mouseY);
        }
        graphics.disableScissor();
    }

    private void renderStatRow(GuiGraphicsExtractor graphics, Layout layout, BotDraft draft, StatField field,
                               int y, int mouseX, int mouseY) {
        int minusW = 24;
        int plusW = 24;
        int plusX = layout.gridRight() - plusW;
        int minusX = plusX - GAP - minusW;
        int valueRight = minusX - 8;
        graphics.fill(layout.gridX(), y, layout.gridRight(), y + DEVELOPER_ROW_H, 0x66191928);
        graphics.text(this.font, Component.translatable(field.translationKey), layout.gridX() + 5, y + 7, 0xFFFFFFFF, false);
        String value = Integer.toString(field.getter.applyAsInt(draft));
        graphics.text(this.font, value, valueRight - this.font.width(value), y + 7, 0xFFFFD1E8, false);
        AstralFancyButton.renderButton(graphics, this.font, Component.literal("-"), minusX, y, minusW,
                DEVELOPER_ROW_H, false, inside(mouseX, mouseY, minusX, y, minusW, DEVELOPER_ROW_H),
                ButtonStyle.button(0xFF646477));
        AstralFancyButton.renderButton(graphics, this.font, Component.literal("+"), plusX, y, plusW,
                DEVELOPER_ROW_H, false, inside(mouseX, mouseY, plusX, y, plusW, DEVELOPER_ROW_H),
                ButtonStyle.button(0xFF4C8EC9));
    }

    private void renderDeveloperCards(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        BotDraft draft = this.selectedBotDraft();
        if (draft == null) {
            Component hint = Component.translatable("gui.astral_craft.board.developer.cards_bot_only");
            graphics.centeredText(this.font, hint, (layout.gridX() + layout.gridRight()) / 2,
                    layout.gridTitleY() + 34, 0xFFAEB7DC);
            return;
        }
        int top = layout.gridTitleY();
        int bottom = layout.buttonY() - 18;
        Component total = Component.translatable("gui.astral_craft.board.developer.card_total", draft.totalCards());
        graphics.text(this.font, total, layout.gridX(), top + 6, 0xFFFFD1E8, false);
        int clearW = 92;
        int clearX = layout.gridRight() - clearW;
        boolean clearHover = inside(mouseX, mouseY, clearX, top, clearW, DEVELOPER_TAB_H);
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.developer.clear_cards"), clearX, top,
                clearW, DEVELOPER_TAB_H, false, clearHover, ButtonStyle.button(0xFF646477));
        int listTop = this.developerCardListTop(layout);
        graphics.enableScissor(layout.gridX(), listTop, layout.gridRight(), bottom);
        for (int index = 0; index < this.developerCardIds.size(); index++) {
            Identifier cardId = this.developerCardIds.get(index);
            DeveloperCardPosition position = this.developerCardPosition(layout, index);
            if (position.y() + DEVELOPER_CARD_TILE_H < listTop || position.y() > bottom) continue;
            this.renderDeveloperCard(graphics, draft, cardId, position, mouseX, mouseY);
        }
        graphics.disableScissor();
    }

    private void renderDeveloperCard(GuiGraphicsExtractor graphics, BotDraft draft, Identifier cardId,
                                     DeveloperCardPosition position, int mouseX, int mouseY) {
        DeveloperCardView view = this.developerCardViews.get(cardId);
        if (view == null) return;
        int count = draft.cards.getOrDefault(cardId, 0);
        int cardX = position.x() + (DEVELOPER_CARD_TILE_W - HandCardRenderHelper.FRAMED_CARD_W) / 2;
        boolean selected = count > 0;
        if (selected) {
            graphics.fill(position.x(), position.y(), position.x() + DEVELOPER_CARD_TILE_W,
                    position.y() + DEVELOPER_CARD_TILE_H, 0x443D2E5A);
        }
        HandCardRenderHelper.renderFramedCard(graphics, this.font, view.definition().type(), view.texture(),
                view.definition().displayName(view.stack()), cardX, position.y(), mouseX, mouseY, false);
        if (count > 0) HandCardRenderHelper.renderCardCount(graphics, this.font, count, cardX, position.y());
        int controlsY = position.controlsY();
        AstralFancyButton.renderButton(graphics, this.font, Component.literal("-"), position.minusX(), controlsY,
                DEVELOPER_CARD_BUTTON_W, DEVELOPER_CARD_CONTROL_H, count <= 0,
                count > 0 && inside(mouseX, mouseY, position.minusX(), controlsY,
                        DEVELOPER_CARD_BUTTON_W, DEVELOPER_CARD_CONTROL_H),
                ButtonStyle.button(0xFF646477));
        AstralFancyButton.renderButton(graphics, this.font, Component.literal("+"), position.plusX(), controlsY,
                DEVELOPER_CARD_BUTTON_W, DEVELOPER_CARD_CONTROL_H,
                draft.totalCards() >= BoardParticipant.MAX_SUPPORTED_HAND_SIZE,
                inside(mouseX, mouseY, position.plusX(), controlsY,
                        DEVELOPER_CARD_BUTTON_W, DEVELOPER_CARD_CONTROL_H),
                ButtonStyle.button(0xFF4C8EC9));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.developerMode) {
            if (event.button() < 0 || event.button() > 2) return super.mouseClicked(event, doubleClick);
            return this.handleDeveloperClick(event) || super.mouseClicked(event, doubleClick);
        }
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (this.selectionLocked || this.submitted) return true;
        Layout layout = this.layout();
        if (this.handleCharacterClick(layout, event.x(), event.y(), true)) return true;
        if (this.handleSkinClick(layout, event.x(), event.y(), true)) return true;
        if (inside(event.x(), event.y(), layout.buttonX(), layout.buttonY(), layout.buttonW(), layout.buttonH())) {
            this.submit();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleDeveloperClick(MouseButtonEvent event) {
        if (this.submitted) return true;
        Layout layout = this.layout();
        int firstSelectableSlot = this.developerLive ? 1 : 0;
        for (int slot = firstSelectableSlot; event.button() == 0 && slot < SLOT_COUNT; slot++) {
            if (!inside(event.x(), event.y(), layout.slotX(slot), layout.slotY(), layout.slotW(), layout.slotH())) continue;
            this.selectedDeveloperSlot = slot;
            if (slot == 0) this.developerTab = DeveloperTab.CHARACTER;
            this.syncSelectedDeveloperSlot();
            return true;
        }
        int tabW = Math.max(1, (layout.gridRight() - layout.gridX() - GAP * 2) / DeveloperTab.values().length);
        for (int index = 0; event.button() == 0 && index < DeveloperTab.values().length; index++) {
            int x = layout.gridX() + index * (tabW + GAP);
            if (!inside(event.x(), event.y(), x, layout.developerTabY(), tabW, DEVELOPER_TAB_H)) continue;
            DeveloperTab next = DeveloperTab.values()[index];
            if (this.selectedDeveloperSlot == 0 && next != DeveloperTab.CHARACTER) return true;
            this.developerTab = next;
            this.developerScroll = 0.0F;
            this.updateDeveloperCardInputs(layout);
            return true;
        }
        if (this.developerTab == DeveloperTab.CHARACTER && event.button() == 0) {
            if (this.handleCharacterClick(layout, event.x(), event.y(), false)) return true;
            if (this.handleSkinClick(layout, event.x(), event.y(), false)) return true;
        } else if (this.developerTab == DeveloperTab.STATS
                && this.handleDeveloperStatClick(layout, event.x(), event.y(), developerStep(event.button()))) {
            return true;
        } else if (this.developerTab == DeveloperTab.CARDS && event.button() == 0
                && this.handleDeveloperCardClick(layout, event.x(), event.y())) {
            return true;
        }
        if (event.button() == 0 && inside(event.x(), event.y(), layout.buttonX(), layout.buttonY(), layout.buttonW(), layout.buttonH())) {
            this.submitDeveloper();
            return true;
        }
        return false;
    }

    private boolean handleCharacterClick(Layout layout, double mouseX, double mouseY, boolean send) {
        for (int index = 0; index < this.characters.size(); index++) {
            CardPosition position = layout.characterPosition(index, this.characterScroll);
            if (!inside(mouseX, mouseY, position.x(), position.y(), CHARACTER_W, CHARACTER_H)
                    || mouseY < layout.gridTop() || mouseY > layout.gridBottom()) continue;
            CharacterDefinition definition = this.characters.get(index);
            boolean allowed = this.developerMode
                    ? !this.characterUsedByOtherSeat(definition.id())
                    && (this.selectedDeveloperSlot == 0 || this.developerBotSelectable.contains(definition.id()))
                    : !this.occupied.contains(definition.id()) && this.characterUnlocked(definition.id());
            if (allowed) {
                this.selectedCharacter = definition.id();
                this.selectedSkin = this.developerMode ? this.firstSkin(definition) : this.preferredSkin(definition.id());
                this.ensureSkin();
                this.skinScroll = 0.0F;
                if (this.developerMode) {
                    Identifier skinId = BoardParticipant.skinIdentifier(this.selectedCharacter, this.selectedSkin);
                    if (this.selectedDeveloperSlot == 0) {
                        this.developerHumanCharacter = this.selectedCharacter;
                        this.developerHumanSkin = skinId;
                    } else {
                        BotDraft draft = this.selectedBotDraft();
                        if (draft != null) draft.setIdentity(this.selectedCharacter, skinId);
                    }
                } else if (send) {
                    this.sendSelection(false);
                }
            }
            return true;
        }
        return false;
    }

    private boolean handleSkinClick(Layout layout, double mouseX, double mouseY, boolean send) {
        List<CharacterSkinDefinition> skins = this.selected().skins();
        if (mouseX < layout.gridX() || mouseX > layout.skinRight()
                || mouseY < layout.skinY() || mouseY > layout.skinBottom()) return false;
        for (int index = 0; index < skins.size(); index++) {
            int x = layout.gridX() + index * (SKIN_W + GAP) - Math.round(this.skinScroll);
            if (!inside(mouseX, mouseY, x, layout.skinY(), SKIN_W, SKIN_H)) continue;
            CharacterSkinDefinition skin = skins.get(index);
            if (this.developerMode || this.skinUnlocked(this.selectedCharacter, skin.id())) {
                this.selectedSkin = skin.id();
                if (this.developerMode) {
                    Identifier skinId = BoardParticipant.skinIdentifier(this.selectedCharacter, skin.id());
                    if (this.selectedDeveloperSlot == 0) this.developerHumanSkin = skinId;
                    else {
                        BotDraft draft = this.selectedBotDraft();
                        if (draft != null) draft.skinId = skinId;
                    }
                } else if (send) {
                    this.sendSelection(false);
                }
            }
            return true;
        }
        return false;
    }

    private boolean handleDeveloperStatClick(Layout layout, double mouseX, double mouseY, int step) {
        BotDraft draft = this.selectedBotDraft();
        if (draft == null) return false;
        int top = layout.gridTitleY();
        int resetW = Math.min(130, layout.gridRight() - layout.gridX());
        if (step == 1 && inside(mouseX, mouseY, layout.gridX(), top, resetW, DEVELOPER_TAB_H)) {
            this.resetStats(draft);
            return true;
        }
        int listTop = top + DEVELOPER_TAB_H + GAP;
        int bottom = layout.buttonY() - 18;
        int plusX = layout.gridRight() - 24;
        int minusX = plusX - GAP - 24;
        for (int index = 0; index < StatField.values().length; index++) {
            int y = listTop + index * (DEVELOPER_ROW_H + 2) - Math.round(this.developerScroll);
            if (y + DEVELOPER_ROW_H < listTop || y > bottom) continue;
            StatField field = StatField.values()[index];
            if (inside(mouseX, mouseY, minusX, y, 24, DEVELOPER_ROW_H)) {
                this.adjustStat(draft, field, -step);
                return true;
            }
            if (inside(mouseX, mouseY, plusX, y, 24, DEVELOPER_ROW_H)) {
                this.adjustStat(draft, field, step);
                return true;
            }
        }
        return false;
    }

    private boolean handleDeveloperCardClick(Layout layout, double mouseX, double mouseY) {
        BotDraft draft = this.selectedBotDraft();
        if (draft == null) return false;
        int top = layout.gridTitleY();
        int clearW = 92;
        int clearX = layout.gridRight() - clearW;
        if (inside(mouseX, mouseY, clearX, top, clearW, DEVELOPER_TAB_H)) {
            draft.cards.clear();
            this.syncDeveloperCardInputs();
            return true;
        }
        int listTop = this.developerCardListTop(layout);
        int bottom = layout.buttonY() - 18;
        if (mouseY < listTop || mouseY > bottom) return false;
        for (int index = 0; index < this.developerCardIds.size(); index++) {
            Identifier cardId = this.developerCardIds.get(index);
            DeveloperCardPosition position = this.developerCardPosition(layout, index);
            if (position.y() + DEVELOPER_CARD_TILE_H < listTop || position.y() > bottom) continue;
            int count = draft.cards.getOrDefault(cardId, 0);
            if (inside(mouseX, mouseY, position.minusX(), position.controlsY(),
                    DEVELOPER_CARD_BUTTON_W, DEVELOPER_CARD_CONTROL_H)) {
                if (count > 0) this.setDeveloperCardCount(cardId, count - 1);
                return true;
            }
            if (inside(mouseX, mouseY, position.plusX(), position.controlsY(),
                    DEVELOPER_CARD_BUTTON_W, DEVELOPER_CARD_CONTROL_H)) {
                if (draft.totalCards() < BoardParticipant.MAX_SUPPORTED_HAND_SIZE) this.setDeveloperCardCount(cardId, count + 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Layout layout = this.layout();
        float delta = (float) (deltaY + deltaX) * 34.0F;
        if (this.developerMode && this.developerTab != DeveloperTab.CHARACTER) {
            this.developerScroll = Mth.clamp(this.developerScroll - delta, 0.0F, this.maximumDeveloperScroll(layout));
        } else if (mouseX >= layout.gridX() && mouseX <= layout.skinRight()
                && mouseY >= layout.skinTitleY() && mouseY <= layout.skinBottom()) {
            this.skinScroll = Mth.clamp(this.skinScroll - delta, 0.0F, this.maximumSkinScroll(layout));
        } else if (mouseX >= layout.gridX() && mouseX <= layout.gridRight()
                && mouseY >= layout.gridTop() && mouseY <= layout.gridBottom()) {
            this.characterScroll = Mth.clamp(this.characterScroll - delta, 0.0F, this.maximumCharacterScroll(layout));
        } else {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        return true;
    }

    @Override
    public void onClose() {
        if (this.developerMode && !this.submitted) {
            ClientPacketDistributor.sendToServer(new BoardDeveloperConfigPayload(this.boardId,
                    this.developerHumanCharacter, this.developerHumanSkin, List.of()));
        }
        super.onClose();
    }

    private void submit() {
        if (this.submitted || !this.characterUnlocked(this.selectedCharacter)
                || !this.skinUnlocked(this.selectedCharacter, this.selectedSkin)) return;
        this.submitted = true;
        this.sendSelection(true);
    }

    private void submitDeveloper() {
        List<BoardDeveloperConfigPayload.BotSetup> setups = new ArrayList<>();
        for (BotDraft draft : this.developerBots) {
            if (draft == null) return;
            List<BoardDeveloperConfigPayload.CardCount> cards = draft.cards.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(entry -> new BoardDeveloperConfigPayload.CardCount(entry.getKey(), entry.getValue())).toList();
            BoardDeveloperConfigPayload.BotStats stats = new BoardDeveloperConfigPayload.BotStats(
                    draft.baseAttack, draft.baseDefense, draft.maxHealth, draft.health, draft.starCoins, draft.stars,
                    draft.cardPlaysPerTurn, draft.cardPlaysRemaining, draft.nextMoveFixed);
            setups.add(new BoardDeveloperConfigPayload.BotSetup(draft.slotId, draft.characterId, draft.skinId, stats,
                    draft.skillCooldownTurns, draft.knockedDownTurns, draft.cardPlaysUsed, draft.maxHandSize, cards));
        }
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardDeveloperConfigPayload(this.boardId,
                this.developerHumanCharacter, this.developerHumanSkin, setups));
        super.onClose();
    }

    private void sendSelection(boolean confirmed) {
        ClientPacketDistributor.sendToServer(new BoardCharacterSelectionPayload(this.boardId, this.selectedCharacter,
                BoardParticipant.skinIdentifier(this.selectedCharacter, this.selectedSkin), confirmed));
    }

    private void syncSelectedDeveloperSlot() {
        if (this.selectedDeveloperSlot == 0) {
            this.selectedCharacter = this.developerHumanCharacter;
            this.selectedSkin = this.developerHumanSkin.getPath();
        } else {
            BotDraft draft = this.selectedBotDraft();
            if (draft == null) return;
            this.selectedCharacter = draft.characterId;
            this.selectedSkin = draft.skinId.getPath();
        }
        this.characterScroll = 0.0F;
        this.skinScroll = 0.0F;
        this.developerScroll = 0.0F;
        this.ensureSkin();
        this.syncDeveloperCardInputs();
    }

    private void resetStats(BotDraft draft) {
        CharacterStatsDefinition stats = this.selected().baseStats();
        draft.baseAttack = stats.attack();
        draft.baseDefense = stats.defense();
        draft.maxHealth = Math.max(1, stats.health());
        draft.health = draft.maxHealth;
        draft.starCoins = BoardSessionManager.PVP_INITIAL_STAR_COINS;
        draft.stars = 0;
        draft.cardPlaysPerTurn = 1;
        draft.cardPlaysRemaining = 1;
        draft.nextMoveFixed = 0;
        draft.skillCooldownTurns = 0;
        draft.knockedDownTurns = 0;
        draft.cardPlaysUsed = 0;
        draft.maxHandSize = Math.max(7, draft.totalCards());
    }

    private void adjustStat(BotDraft draft, StatField field, int delta) {
        int current = field.getter.applyAsInt(draft);
        int maximum = field == StatField.HEALTH ? draft.maxHealth : field.maximum;
        int next = Math.clamp(current + delta, field.minimum, maximum);
        field.setter.accept(draft, next);
        if (field == StatField.MAX_HEALTH) draft.health = Math.min(draft.health, draft.maxHealth);
    }

    private CharacterDefinition selected() {
        return this.characters.stream().filter(value -> value.id().equals(this.selectedCharacter))
                .findFirst().orElse(this.characters.getFirst());
    }

    private BoardCharacterSelectionEntry entry(int slot) {
        return this.lobbyEntries.stream().filter(value -> value.slot() == slot).findFirst().orElse(null);
    }

    private BoardCharacterSelectionEntry developerEntry(int slot) {
        if (slot == 0) return new BoardCharacterSelectionEntry(0, this.developerHumanName,
                this.developerHumanCharacter, this.developerHumanSkin, true, true);
        BotDraft draft = this.developerBots[slot - 1];
        if (draft == null) return null;
        return new BoardCharacterSelectionEntry(slot,
                Component.translatable("gui.astral_craft.board.developer.bot", slot).getString(),
                draft.characterId, draft.skinId, true, true);
    }

    private AstralCharacterEntity slotPreview(int slot, BoardCharacterSelectionEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.slotPreviews[slot] == null && minecraft.level != null) {
            this.slotPreviews[slot] = new AstralCharacterEntity(AstralEntities.ASTRAL_CHARACTER.get(), minecraft.level);
        }
        AstralCharacterEntity preview = this.slotPreviews[slot];
        if (preview != null) {
            preview.setCharacterId(entry.characterId());
            preview.setSkinId(entry.skinId().getPath());
            CharacterDefinition definition = this.characters.stream().filter(value -> value.id().equals(entry.characterId()))
                    .findFirst().orElseGet(() -> ClientCharacterDefinitionCache.INSTANCE.getOrFallback(entry.characterId()));
            preview.setAnimationAction(definition.previewAction());
        }
        return preview;
    }

    private void ensureAvailableSelection() {
        if (this.developerMode || this.selectionLocked) return;
        boolean unavailable = this.occupied.contains(this.selectedCharacter) || !this.characterUnlocked(this.selectedCharacter);
        if (!unavailable) return;
        this.selectedCharacter = this.characters.stream().map(CharacterDefinition::id)
                .filter(this::characterUnlocked).filter(id -> !this.occupied.contains(id))
                .findFirst().orElse(this.selectedCharacter);
        this.selectedSkin = this.preferredSkin(this.selectedCharacter);
    }

    private void ensureSkin() {
        CharacterDefinition definition = this.selected();
        boolean valid = definition.skins().stream().anyMatch(value -> value.id().equals(this.selectedSkin))
                && (this.developerMode || this.skinUnlocked(definition.id(), this.selectedSkin));
        if (valid) return;
        if (this.developerMode) {
            this.selectedSkin = this.firstSkin(definition);
            Identifier skinId = BoardParticipant.skinIdentifier(definition.id(), this.selectedSkin);
            if (this.selectedDeveloperSlot == 0) this.developerHumanSkin = skinId;
            else {
                BotDraft draft = this.selectedBotDraft();
                if (draft != null) draft.skinId = skinId;
            }
            return;
        }
        String preferred = this.preferredSkin(definition.id());
        this.selectedSkin = definition.skins().stream().filter(skin -> skin.id().equals(preferred))
                .filter(skin -> this.skinUnlocked(definition.id(), skin.id())).map(CharacterSkinDefinition::id)
                .findFirst().orElseGet(() -> definition.skins().stream()
                        .filter(skin -> this.skinUnlocked(definition.id(), skin.id())).map(CharacterSkinDefinition::id)
                        .findFirst().orElse("default"));
    }

    private void replaceAvailability(List<BoardCharacterAvailability> values) {
        this.availability.clear();
        for (BoardCharacterAvailability value : values) this.availability.put(value.characterId(), value);
    }

    private boolean characterUnlocked(Identifier characterId) {
        BoardCharacterAvailability value = this.availability.get(characterId);
        return value == null || value.unlocked();
    }

    private boolean skinUnlocked(Identifier characterId, String skinId) {
        BoardCharacterAvailability value = this.availability.get(characterId);
        return value == null || value.isSkinUnlocked(skinId);
    }

    private String preferredSkin(Identifier characterId) {
        if (this.developerMode) {
            CharacterDefinition definition = this.characters.stream().filter(value -> value.id().equals(characterId))
                    .findFirst().orElse(this.characters.getFirst());
            return this.firstSkin(definition);
        }
        BoardCharacterAvailability value = this.availability.get(characterId);
        return value == null ? "default" : value.preferredSkinId().getPath();
    }

    private String firstSkin(CharacterDefinition definition) {
        return definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
    }

    private boolean characterUsedByOtherSeat(Identifier characterId) {
        if (this.selectedDeveloperSlot != 0 && this.developerHumanCharacter.equals(characterId)) return true;
        for (int index = 0; index < this.developerBots.length; index++) {
            if (index == this.selectedDeveloperSlot - 1 || this.developerBots[index] == null) continue;
            if (this.developerBots[index].characterId.equals(characterId)) return true;
        }
        return false;
    }

    private BotDraft selectedBotDraft() {
        int botIndex = this.selectedDeveloperSlot - 1;
        return botIndex < 0 || botIndex >= this.developerBots.length ? null : this.developerBots[botIndex];
    }

    private Component cardName(Identifier cardId) {
        DeveloperCardView view = this.developerCardViews.get(cardId);
        if (view != null) return view.definition().displayName(view.stack());
        Item item = BuiltInRegistries.ITEM.getValue(cardId);
        return item == null ? Component.literal(cardId.toString()) : new ItemStack(item).getHoverName();
    }

    private void prepareDeveloperCardViews() {
        this.developerCardViews.clear();
        for (Identifier cardId : this.developerCardIds) {
            Item item = BuiltInRegistries.ITEM.getValue(cardId);
            if (!(item instanceof BaseHandCard card)) continue;
            ItemStack stack = new ItemStack(item);
            CardDefinition definition = card.definition(stack);
            this.developerCardViews.put(cardId,
                    new DeveloperCardView(stack, definition, definition.largeFrontTexture(stack)));
        }
    }

    private void createDeveloperCardInputs() {
        this.developerCardCountBoxes.clear();
        for (Identifier cardId : this.developerCardIds) {
            EditBox box = this.addRenderableWidget(new EditBox(this.font, 0, 0, DEVELOPER_CARD_INPUT_W,
                    DEVELOPER_CARD_CONTROL_H, this.cardName(cardId)));
            box.setMaxLength(Integer.toString(BoardParticipant.MAX_SUPPORTED_HAND_SIZE).length());
            box.setFilter(BoardCharacterSelectionScreen::validDeveloperCardCountInput);
            box.setResponder(value -> this.updateDeveloperCardCountFromInput(cardId, value));
            box.setVisible(false);
            this.developerCardCountBoxes.put(cardId, box);
        }
        this.syncDeveloperCardInputs();
        this.updateDeveloperCardInputs(this.layout());
    }

    private void updateDeveloperCardInputs(Layout layout) {
        boolean showing = this.developerMode && this.developerTab == DeveloperTab.CARDS
                && this.selectedDeveloperSlot > 0 && !this.submitted;
        int listTop = this.developerCardListTop(layout);
        int bottom = layout.buttonY() - 18;
        for (int index = 0; index < this.developerCardIds.size(); index++) {
            Identifier cardId = this.developerCardIds.get(index);
            EditBox box = this.developerCardCountBoxes.get(cardId);
            if (box == null) continue;
            DeveloperCardPosition position = this.developerCardPosition(layout, index);
            boolean visible = showing && position.controlsY() >= listTop
                    && position.controlsY() + DEVELOPER_CARD_CONTROL_H <= bottom;
            box.setPosition(position.inputX(), position.controlsY());
            box.setVisible(visible);
            box.active = visible;
        }
    }

    private void syncDeveloperCardInputs() {
        if (this.developerCardCountBoxes.isEmpty()) return;
        BotDraft draft = this.selectedBotDraft();
        this.syncingCardInputs = true;
        try {
            for (Map.Entry<Identifier, EditBox> entry : this.developerCardCountBoxes.entrySet()) {
                int count = draft == null ? 0 : draft.cards.getOrDefault(entry.getKey(), 0);
                entry.getValue().setValue(count <= 0 ? "" : Integer.toString(count));
            }
        } finally {
            this.syncingCardInputs = false;
        }
    }

    private void updateDeveloperCardCountFromInput(Identifier cardId, String value) {
        if (this.syncingCardInputs) return;
        BotDraft draft = this.selectedBotDraft();
        if (draft == null) return;
        int requested = value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        int current = draft.cards.getOrDefault(cardId, 0);
        int available = Math.max(0, BoardParticipant.MAX_SUPPORTED_HAND_SIZE - (draft.totalCards() - current));
        int next = Math.min(requested, available);
        if (next <= 0) draft.cards.remove(cardId);
        else draft.cards.put(cardId, next);
        draft.maxHandSize = Math.max(draft.maxHandSize, draft.totalCards());
        if (next != requested) this.syncDeveloperCardInputs();
    }

    private void setDeveloperCardCount(Identifier cardId, int requested) {
        BotDraft draft = this.selectedBotDraft();
        if (draft == null) return;
        int current = draft.cards.getOrDefault(cardId, 0);
        int available = Math.max(0, BoardParticipant.MAX_SUPPORTED_HAND_SIZE - (draft.totalCards() - current));
        int next = Math.clamp(requested, 0, available);
        if (next == 0) draft.cards.remove(cardId);
        else draft.cards.put(cardId, next);
        draft.maxHandSize = Math.max(draft.maxHandSize, draft.totalCards());
        this.syncDeveloperCardInputs();
    }

    private int developerCardColumns(Layout layout) {
        return Math.max(1, (layout.gridRight() - layout.gridX() + GAP) / (DEVELOPER_CARD_TILE_W + GAP));
    }

    private int developerCardListTop(Layout layout) {
        return layout.gridTitleY() + DEVELOPER_TAB_H + GAP;
    }

    private DeveloperCardPosition developerCardPosition(Layout layout, int index) {
        int columns = this.developerCardColumns(layout);
        int rowsWidth = columns * DEVELOPER_CARD_TILE_W + Math.max(0, columns - 1) * GAP;
        int startX = layout.gridX() + Math.max(0, (layout.gridRight() - layout.gridX() - rowsWidth) / 2);
        int x = startX + index % columns * (DEVELOPER_CARD_TILE_W + GAP);
        int y = this.developerCardListTop(layout) + index / columns * (DEVELOPER_CARD_TILE_H + GAP)
                - Math.round(this.developerScroll);
        int controlsY = y + HandCardRenderHelper.FRAMED_CARD_H + 4;
        int minusX = x;
        int inputX = minusX + DEVELOPER_CARD_BUTTON_W + GAP;
        int plusX = inputX + DEVELOPER_CARD_INPUT_W + GAP;
        return new DeveloperCardPosition(x, y, controlsY, minusX, inputX, plusX);
    }

    private static boolean validDeveloperCardCountInput(String value) {
        if (value == null || value.isEmpty()) return true;
        if (value.charAt(0) < '1' || value.charAt(0) > '9') return false;
        for (int index = 1; index < value.length(); index++) {
            if (value.charAt(index) < '0' || value.charAt(index) > '9') return false;
        }
        try {
            return Integer.parseInt(value) <= BoardParticipant.MAX_SUPPORTED_HAND_SIZE;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private float maximumCharacterScroll(Layout layout) {
        int rows = (this.characters.size() + layout.columns() - 1) / layout.columns();
        return Math.max(0, rows * (CHARACTER_H + GAP) - GAP - (layout.gridBottom() - layout.gridTop()));
    }

    private float maximumSkinScroll(Layout layout) {
        int content = this.selected().skins().size() * (SKIN_W + GAP) - GAP;
        return Math.max(0, content - (layout.skinRight() - layout.gridX()));
    }

    private float maximumDeveloperScroll(Layout layout) {
        if (this.developerTab == DeveloperTab.CHARACTER) return 0.0F;
        if (this.developerTab == DeveloperTab.CARDS) {
            int columns = this.developerCardColumns(layout);
            int rows = (this.developerCardIds.size() + columns - 1) / columns;
            int content = Math.max(0, rows * (DEVELOPER_CARD_TILE_H + GAP) - GAP);
            int available = Math.max(1, layout.buttonY() - 18 - this.developerCardListTop(layout));
            return Math.max(0, content - available);
        }
        int content = StatField.values().length * (DEVELOPER_ROW_H + 2) - 2;
        int available = Math.max(1, layout.buttonY() - 18 - (layout.gridTitleY() + DEVELOPER_TAB_H + GAP));
        return Math.max(0, content - available);
    }

    private Layout layout() {
        int panelX = MARGIN;
        int panelY = MARGIN;
        int panelRight = Math.max(panelX + 1, this.width - MARGIN);
        int panelBottom = Math.max(panelY + 1, this.height - MARGIN);
        int slotGap = Math.clamp(this.width / 70, 4, 8);
        int slotW = Math.clamp((panelRight - panelX - 36 - slotGap * 3L) / 4, 48, 76);
        int slotH = Math.clamp(this.height / 4, 56, 88);
        int slotsWidth = slotW * 4 + slotGap * 3;
        int slotsX = panelX + (panelRight - panelX - slotsWidth) / 2;
        int slotY = panelY + 19;
        int gridX = panelX + 10;
        int gridRight = panelRight - 10;
        int developerTabY = this.developerMode ? slotY + slotH + 6 : 0;
        int gridTitleY = this.developerMode ? developerTabY + DEVELOPER_TAB_H + 6 : slotY + slotH + 7;
        int gridTop = gridTitleY + 13;
        int buttonW = Math.clamp((panelRight - panelX) / 4, 76, 116);
        int buttonH = 25;
        int buttonX = panelRight - buttonW - 8;
        int buttonY = panelBottom - buttonH - 16;
        int skinBottom = panelBottom - 7;
        int skinY = skinBottom - SKIN_H;
        int skinTitleY = skinY - 13;
        int skinRight = Math.max(gridX + 1, buttonX - 6);
        int gridBottom = Math.max(gridTop + 1, skinTitleY - 5);
        int columns = Math.max(1, (gridRight - gridX + GAP) / (CHARACTER_W + GAP));
        return new Layout(panelX, panelY, panelRight, panelBottom, slotsX, slotY, slotW, slotH,
                slotGap, developerTabY, gridX, gridRight, gridTitleY, gridTop, gridBottom, skinTitleY, skinY,
                skinBottom, skinRight, columns, buttonX, buttonY, buttonW, buttonH);
    }

    private static int developerStep(int button) {
        return button == 2 ? 100 : button == 1 ? 10 : 1;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record CardPosition(int x, int y) {}

    private record DeveloperCardPosition(int x, int y, int controlsY, int minusX, int inputX, int plusX) {}

    private record DeveloperCardView(ItemStack stack, CardDefinition definition, Identifier texture) {}

    private record Layout(int panelX, int panelY, int panelRight, int panelBottom,
                          int slotsX, int slotY, int slotW, int slotH, int slotGap, int developerTabY,
                          int gridX, int gridRight, int gridTitleY, int gridTop, int gridBottom,
                          int skinTitleY, int skinY, int skinBottom, int skinRight, int columns,
                          int buttonX, int buttonY, int buttonW, int buttonH) {

        int slotX(int slot) {
            return this.slotsX + slot * (this.slotW + this.slotGap);
        }

        CardPosition characterPosition(int index, float scroll) {
            return new CardPosition(this.gridX + index % this.columns * (CHARACTER_W + GAP),
                    this.gridTop + index / this.columns * (CHARACTER_H + GAP) - Math.round(scroll));
        }
    }

    private enum DeveloperTab {
        CHARACTER("gui.astral_craft.board.developer.tab.character"),
        STATS("gui.astral_craft.board.developer.tab.stats"),
        CARDS("gui.astral_craft.board.developer.tab.cards");

        private final String translationKey;

        DeveloperTab(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private enum StatField {
        ATTACK("gui.astral_craft.board.developer.stat.attack", draft -> draft.baseAttack,
                (draft, value) -> draft.baseAttack = value, -BoardDeveloperService.MAX_BASE_STAT, BoardDeveloperService.MAX_BASE_STAT),
        DEFENSE("gui.astral_craft.board.developer.stat.defense", draft -> draft.baseDefense,
                (draft, value) -> draft.baseDefense = value, -BoardDeveloperService.MAX_BASE_STAT, BoardDeveloperService.MAX_BASE_STAT),
        MAX_HEALTH("gui.astral_craft.board.developer.stat.max_health", draft -> draft.maxHealth,
                (draft, value) -> draft.maxHealth = value, 1, BoardDeveloperService.MAX_HEALTH),
        HEALTH("gui.astral_craft.board.developer.stat.health", draft -> draft.health,
                (draft, value) -> draft.health = value, 0, BoardDeveloperService.MAX_HEALTH),
        STAR_COINS("gui.astral_craft.board.developer.stat.star_coins", draft -> draft.starCoins,
                (draft, value) -> draft.starCoins = value, 0, BoardDeveloperService.MAX_STAR_COINS),
        STARS("gui.astral_craft.board.developer.stat.stars", draft -> draft.stars,
                (draft, value) -> draft.stars = value, 0, BoardDeveloperService.MAX_STARS),
        CARD_PLAYS("gui.astral_craft.board.developer.stat.card_plays", draft -> draft.cardPlaysPerTurn,
                (draft, value) -> draft.cardPlaysPerTurn = value, 0, BoardDeveloperService.MAX_CARD_PLAYS),
        CARD_PLAYS_REMAINING("gui.astral_craft.board.developer.stat.card_plays_remaining", draft -> draft.cardPlaysRemaining,
                (draft, value) -> draft.cardPlaysRemaining = value, 0, BoardDeveloperService.MAX_CARD_PLAYS),
        CARD_PLAYS_USED("gui.astral_craft.board.developer.stat.card_plays_used", draft -> draft.cardPlaysUsed,
                (draft, value) -> draft.cardPlaysUsed = value, 0, BoardDeveloperService.MAX_CARD_PLAYS),
        FIXED_MOVE("gui.astral_craft.board.developer.stat.fixed_move", draft -> draft.nextMoveFixed,
                (draft, value) -> draft.nextMoveFixed = value, 0, BoardDeveloperService.MAX_FIXED_MOVE),
        SKILL_COOLDOWN("gui.astral_craft.board.developer.stat.skill_cooldown", draft -> draft.skillCooldownTurns,
                (draft, value) -> draft.skillCooldownTurns = value, 0, BoardDeveloperService.MAX_SKILL_COOLDOWN),
        KNOCKDOWN_TURNS("gui.astral_craft.board.developer.stat.knockdown_turns", draft -> draft.knockedDownTurns,
                (draft, value) -> draft.knockedDownTurns = value, 0, BoardDeveloperService.MAX_KNOCKDOWN_TURNS),
        MAX_HAND_SIZE("gui.astral_craft.board.developer.stat.max_hand_size", draft -> draft.maxHandSize,
                (draft, value) -> draft.maxHandSize = value, 1, BoardParticipant.MAX_SUPPORTED_HAND_SIZE);

        private final String translationKey;
        private final ToIntFunction<BotDraft> getter;
        private final BiConsumer<BotDraft, Integer> setter;
        private final int minimum;
        private final int maximum;

        StatField(String translationKey, ToIntFunction<BotDraft> getter, BiConsumer<BotDraft, Integer> setter,
                  int minimum, int maximum) {
            this.translationKey = translationKey;
            this.getter = getter;
            this.setter = setter;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }

    private static class BotDraft {
        private final UUID slotId;
        private Identifier characterId;
        private Identifier skinId;
        private int baseAttack;
        private int baseDefense;
        private int maxHealth;
        private int health;
        private int starCoins;
        private int stars;
        private int cardPlaysPerTurn;
        private int cardPlaysRemaining;
        private int nextMoveFixed;
        private int skillCooldownTurns;
        private int knockedDownTurns;
        private int cardPlaysUsed;
        private int maxHandSize;
        private final Map<Identifier, Integer> cards = new LinkedHashMap<>();

        private BotDraft(OpenBoardDeveloperPayload.BotView view) {
            this.slotId = view.slotId();
            this.characterId = view.characterId();
            this.skinId = view.skinId();
            this.baseAttack = view.stats().baseAttack();
            this.baseDefense = view.stats().baseDefense();
            this.maxHealth = view.stats().maxHealth();
            this.health = view.stats().health();
            this.starCoins = view.stats().starCoins();
            this.stars = view.stats().stars();
            this.cardPlaysPerTurn = view.stats().cardPlaysPerTurn();
            this.cardPlaysRemaining = view.stats().cardPlaysRemaining();
            this.nextMoveFixed = view.stats().nextMoveFixed();
            this.skillCooldownTurns = view.skillCooldownTurns();
            this.knockedDownTurns = view.knockedDownTurns();
            this.cardPlaysUsed = view.cardPlaysUsed();
            this.maxHandSize = view.maxHandSize();
            for (Identifier cardId : view.hand()) this.cards.merge(cardId, 1, Integer::sum);
        }

        private void setIdentity(Identifier characterId, Identifier skinId) {
            this.characterId = characterId;
            this.skinId = skinId;
        }

        private int totalCards() {
            return this.cards.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

}