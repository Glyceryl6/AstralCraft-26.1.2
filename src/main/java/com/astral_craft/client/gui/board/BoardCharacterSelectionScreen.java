package com.astral_craft.client.gui.board;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.c2s.BoardCharacterSelectionPayload;
import com.astral_craft.common.network.s2c.BoardCharacterAvailability;
import com.astral_craft.common.network.s2c.BoardCharacterSelectionEntry;
import com.astral_craft.common.network.s2c.OpenBoardCharacterSelectionPayload;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Board lobby selector with four live preview slots; slot order is also the later turn order. */
public class BoardCharacterSelectionScreen extends Screen {

    private static final int MARGIN = 10;
    private static final int GAP = 5;
    private static final int CHARACTER_W = 48;
    private static final int CHARACTER_H = 40;
    private static final int SKIN_W = 46;
    private static final int SKIN_H = 44;
    private static final int SLOT_COUNT = 4;
    private final UUID boardId;
    private final List<CharacterDefinition> characters;
    private final Set<Identifier> occupied;
    private final Map<Identifier, BoardCharacterAvailability> availability = new HashMap<>();
    private final AstralCharacterEntity[] slotPreviews = new AstralCharacterEntity[SLOT_COUNT];
    private List<BoardCharacterSelectionEntry> lobbyEntries;
    private Identifier selectedCharacter;
    private String selectedSkin;
    private int timeoutTicks;
    private int timeoutDurationTicks;
    private boolean submitted;
    private boolean selectionLocked;
    private float characterScroll;
    private float skinScroll;

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
        this.ensureAvailableSelection();
        this.ensureSkin();
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
                    && screen.boardId.equals(payload.boardId())) {
                screen.refresh(payload);
                return;
            }

            minecraft.setScreen(new BoardCharacterSelectionScreen(payload));
        });
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
        this.characterScroll = Mth.clamp(this.characterScroll, 0.0F, this.maximumCharacterScroll(this.layout()));
        this.skinScroll = Mth.clamp(this.skinScroll, 0.0F, this.maximumSkinScroll(this.layout()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0) this.timeoutTicks--;
        for (AstralCharacterEntity preview : this.slotPreviews) {
            if (preview != null) preview.tickCount++;
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(0, 0, this.width, this.height, 0xE6090912);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelRight(), layout.panelBottom(), 0xCC11111C);
        graphics.text(this.font, this.title, layout.panelX() + 8, layout.panelY() + 6, 0xFFFFFFFF, false);
        this.renderLobbySlots(graphics, layout);
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

    private void renderLobbySlots(GuiGraphicsExtractor graphics, Layout layout) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int x = layout.slotX(slot);
            int y = layout.slotY();
            BoardCharacterSelectionEntry entry = this.entry(slot);
            int border = entry != null && entry.confirmed() ? 0xFFFFD34E : 0xFF626273;
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

            if (entry != null && entry.confirmed()) {
                Component ok = Component.literal("OK").withStyle(ChatFormatting.BOLD);
                float scale = 1.45F;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + layout.slotW() - 5, y + 6);
                graphics.pose().scale(scale, scale);
                graphics.text(this.font, ok, -this.font.width(ok), 0, 0xFFFFE06C, true);
                graphics.pose().popMatrix();
            }

            String playerName = entry == null ? "" : entry.playerName();
            String shown = this.font.plainSubstrByWidth(playerName, layout.slotW() - 6);
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
            boolean occupiedCard = this.occupied.contains(definition.id());
            boolean unlocked = this.characterUnlocked(definition.id());
            boolean unavailable = this.selectionLocked || this.submitted || occupiedCard || !unlocked;
            boolean hovered = inside(mouseX, mouseY, position.x(), position.y(), CHARACTER_W, CHARACTER_H);
            AstralFancyButton.renderIconFrame(graphics, position.x(), position.y(), CHARACTER_W, CHARACTER_H, selectedCard, hovered && !unavailable);
            if (occupiedCard) {
                int frameColor = 0xFFFFC65C;
                graphics.fill(position.x(), position.y(), position.x() + CHARACTER_W, position.y() + 2, frameColor);
                graphics.fill(position.x(), position.y() + CHARACTER_H - 2, position.x() + CHARACTER_W, position.y() + CHARACTER_H, frameColor);
                graphics.fill(position.x(), position.y(), position.x() + 2, position.y() + CHARACTER_H, frameColor);
                graphics.fill(position.x() + CHARACTER_W - 2, position.y(), position.x() + CHARACTER_W, position.y() + CHARACTER_H, frameColor);
                if (!selectedCard) graphics.fill(position.x(), position.y(), position.x() + CHARACTER_W, position.y() + CHARACTER_H, 0x882A2026);
            }

            String skinId = this.preferredSkin(definition.id());
            int alpha = occupiedCard && !selectedCard ? 90 : 255;
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, definition.id(), skinId,
                    position.x() + 12, position.y() + 2, 24, alpha, !unlocked);
            if (!unlocked) graphics.fill(position.x(), position.y(), position.x() + CHARACTER_W,
                    position.y() + CHARACTER_H, 0x66101010);
            Component characterName = Component.translatable(definition.getDescriptionId());
            int textColor = !unlocked || occupiedCard && !selectedCard ? 0xFF7C7478 : 0xFFFFFFFF;
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
            boolean unlocked = this.skinUnlocked(this.selected().id(), skin.id());
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (this.selectionLocked || this.submitted) return true;
        Layout layout = this.layout();
        for (int index = 0; index < this.characters.size(); index++) {
            CardPosition position = layout.characterPosition(index, this.characterScroll);
            if (!inside(event.x(), event.y(), position.x(), position.y(), CHARACTER_W, CHARACTER_H)
                    || event.y() < layout.gridTop() || event.y() > layout.gridBottom()) continue;
            CharacterDefinition definition = this.characters.get(index);
            if (!this.occupied.contains(definition.id()) && this.characterUnlocked(definition.id())) {
                this.selectedCharacter = definition.id();
                this.selectedSkin = this.preferredSkin(definition.id());
                this.ensureSkin();
                this.skinScroll = 0.0F;
                this.sendSelection(false);
            }

            return true;
        }

        List<CharacterSkinDefinition> skins = this.selected().skins();
        if (event.x() >= layout.gridX() && event.x() <= layout.skinRight()
                && event.y() >= layout.skinY() && event.y() <= layout.skinBottom()) {
            for (int index = 0; index < skins.size(); index++) {
                int x = layout.gridX() + index * (SKIN_W + GAP) - Math.round(this.skinScroll);
                if (inside(event.x(), event.y(), x, layout.skinY(), SKIN_W, SKIN_H)) {
                    CharacterSkinDefinition skin = skins.get(index);
                    if (this.skinUnlocked(this.selectedCharacter, skin.id())) {
                        this.selectedSkin = skin.id();
                        this.sendSelection(false);
                    }
                    return true;
                }
            }
        }

        if (inside(event.x(), event.y(), layout.buttonX(), layout.buttonY(), layout.buttonW(), layout.buttonH())) {
            this.submit();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Layout layout = this.layout();
        float delta = (float) (deltaY + deltaX) * 34.0F;
        if (mouseX >= layout.gridX() && mouseX <= layout.skinRight() && mouseY >= layout.skinTitleY() && mouseY <= layout.skinBottom()) {
            this.skinScroll = Mth.clamp(this.skinScroll - delta, 0.0F, this.maximumSkinScroll(layout));
        } else if (mouseX >= layout.gridX() && mouseX <= layout.gridRight() && mouseY >= layout.gridTop() && mouseY <= layout.gridBottom()) {
            this.characterScroll = Mth.clamp(this.characterScroll - delta, 0.0F, this.maximumCharacterScroll(layout));
        } else {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        return true;
    }

    private void submit() {
        if (this.submitted || !this.characterUnlocked(this.selectedCharacter)
                || !this.skinUnlocked(this.selectedCharacter, this.selectedSkin)) return;
        this.submitted = true;
        this.sendSelection(true);
    }

    private void sendSelection(boolean confirmed) {
        ClientPacketDistributor.sendToServer(new BoardCharacterSelectionPayload(this.boardId, this.selectedCharacter,
                BoardParticipant.skinIdentifier(this.selectedCharacter, this.selectedSkin), confirmed));
    }

    private CharacterDefinition selected() {
        return this.characters.stream()
                .filter(value -> value.id().equals(this.selectedCharacter))
                .findFirst().orElse(this.characters.getFirst());
    }

    private BoardCharacterSelectionEntry entry(int slot) {
        return this.lobbyEntries.stream().filter(value -> value.slot() == slot).findFirst().orElse(null);
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
            CharacterDefinition definition = this.characters.stream()
                    .filter(value -> value.id().equals(entry.characterId()))
                    .findFirst().orElse(this.characters.getFirst());
            preview.setAnimationAction(definition.previewAction());
        }

        return preview;
    }

    private void ensureAvailableSelection() {
        if (this.selectionLocked) return;
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
                && this.skinUnlocked(definition.id(), this.selectedSkin);
        if (valid) return;
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
        BoardCharacterAvailability value = this.availability.get(characterId);
        return value == null ? "default" : value.preferredSkinId().getPath();
    }

    private float maximumCharacterScroll(Layout layout) {
        int rows = (this.characters.size() + layout.columns() - 1) / layout.columns();
        return Math.max(0, rows * (CHARACTER_H + GAP) - GAP - (layout.gridBottom() - layout.gridTop()));
    }

    private float maximumSkinScroll(Layout layout) {
        int content = this.selected().skins().size() * (SKIN_W + GAP) - GAP;
        return Math.max(0, content - (layout.skinRight() - layout.gridX()));
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
        int gridTitleY = slotY + slotH + 7;
        int gridTop = gridTitleY + 13;
        int buttonW = Math.clamp((panelRight - panelX) / 4, 76, 116);
        int buttonH = 25;
        int buttonX = panelRight - buttonW - 8;
        int buttonY = panelBottom - buttonH - 7;
        int skinBottom = panelBottom - 7;
        int skinY = skinBottom - SKIN_H;
        int skinTitleY = skinY - 13;
        int skinRight = Math.max(gridX + 1, buttonX - 6);
        int gridBottom = Math.max(gridTop + 1, skinTitleY - 5);
        int columns = Math.max(1, (gridRight - gridX + GAP) / (CHARACTER_W + GAP));
        return new Layout(panelX, panelY, panelRight, panelBottom, slotsX, slotY, slotW, slotH,
                slotGap, gridX, gridRight, gridTitleY, gridTop, gridBottom, skinTitleY, skinY,
                skinBottom, skinRight, columns, buttonX, buttonY, buttonW, buttonH);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record CardPosition(int x, int y) {}

    private record Layout(int panelX, int panelY, int panelRight, int panelBottom,
                          int slotsX, int slotY, int slotW, int slotH, int slotGap,
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

}