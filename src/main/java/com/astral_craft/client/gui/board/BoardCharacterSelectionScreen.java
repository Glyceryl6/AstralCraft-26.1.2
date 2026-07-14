package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.AstralStatusIconRenderer;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterCodecLines;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.BoardCharacterSelectionPayload;
import com.astral_craft.common.network.OpenBoardCharacterSelectionPayload;
import com.astral_craft.common.registry.AstralEntities;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Responsive board lobby selector using the actual skin head texture for every portrait. */
public class BoardCharacterSelectionScreen extends Screen {

    private static final int MARGIN = 12;
    private static final int GAP = 6;
    private static final int CHARACTER_W = 66;
    private static final int CHARACTER_H = 78;
    private static final int SKIN_W = 54;
    private static final int SKIN_H = 58;
    private final String boardId;
    private final List<CharacterDefinition> characters;
    private final Set<Identifier> occupied;
    private Identifier selectedCharacter;
    private String selectedSkin;
    private int timeoutTicks;
    private boolean submitted;
    private float characterScroll;
    private float skinScroll;
    private AstralCharacterEntity preview;

    public BoardCharacterSelectionScreen(OpenBoardCharacterSelectionPayload payload) {
        super(Component.translatable("gui.astral_craft.board.character_select"));
        this.boardId = payload.boardId();
        List<CharacterDefinition> decoded = CharacterCodecLines.decode(payload.encodedCharacters());
        this.characters = decoded.isEmpty()
                ? List.of(CharacterManager.INSTANCE.defaultCharacter()) : decoded;
        this.occupied = parseIds(payload.occupiedCharacterIds());
        this.selectedCharacter = parse(payload.selectedCharacterId(), this.characters.getFirst().id());
        if (this.occupied.contains(this.selectedCharacter)) {
            this.selectedCharacter = this.characters.stream().map(CharacterDefinition::id)
                    .filter(id -> !this.occupied.contains(id)).findFirst().orElse(this.selectedCharacter);
        }
        this.selectedSkin = payload.selectedSkinId();
        this.timeoutTicks = Math.max(1, payload.timeoutTicks());
        this.ensureSkin();
    }

    public static void open(OpenBoardCharacterSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (payload.encodedCharacters().isBlank()) {
                if (minecraft.screen instanceof BoardCharacterSelectionScreen screen
                        && screen.boardId.equals(payload.boardId())) {
                    minecraft.setScreen(null);
                }
                return;
            }
            if (payload.refresh()) {
                if (minecraft.screen instanceof BoardCharacterSelectionScreen screen
                        && screen.boardId.equals(payload.boardId())) {
                    screen.refresh(payload);
                }
                return;
            }
            minecraft.setScreen(new BoardCharacterSelectionScreen(payload));
        });
    }


    private void refresh(OpenBoardCharacterSelectionPayload payload) {
        this.occupied.clear();
        this.occupied.addAll(parseIds(payload.occupiedCharacterIds()));
        this.timeoutTicks = Math.max(1, payload.timeoutTicks());
        if (this.occupied.contains(this.selectedCharacter)) {
            Identifier replacement = this.characters.stream().map(CharacterDefinition::id)
                    .filter(id -> !this.occupied.contains(id)).findFirst().orElse(this.selectedCharacter);
            if (!replacement.equals(this.selectedCharacter)) {
                this.selectedCharacter = replacement;
                this.selectedSkin = this.selected().skins().isEmpty()
                        ? "default" : this.selected().skins().getFirst().id();
                this.skinScroll = 0.0F;
                this.preview = null;
            }
        }
        this.ensureSkin();
    }

    @Override
    protected void init() {
        this.preview = null;
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
        if (!this.submitted && --this.timeoutTicks <= 0) this.submit();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(0, 0, this.width, this.height, 0xE6090912);
        graphics.fill(layout.leftX(), layout.top(), layout.leftRight(), layout.bottom(), 0xCC171725);
        graphics.fill(layout.rightX(), layout.top(), layout.right(), layout.bottom(), 0xCC11111C);
        graphics.text(this.font, this.title, layout.leftX() + 10, layout.top() + 8, 0xFFFFFFFF, false);

        CharacterDefinition selected = this.selected();
        Component title = Component.translatable(selected.titleKey());
        Component name = Component.translatable(selected.nameKey());
        graphics.text(this.font, title, layout.leftX() + 10, layout.top() + 28, 0xFFFFC75C, false);
        graphics.text(this.font, name, layout.leftX() + 10, layout.top() + 42, 0xFFFFFFFF, false);
        BoardScreenEntityRenderer.render(graphics, this.preview(selected), layout.leftX() + 6, layout.top() + 56,
                layout.leftRight() - 6, layout.bottom() - 10, -38.0F);

        graphics.text(this.font, Component.translatable("gui.astral_craft.board.characters"),
                layout.gridX(), layout.top() + 8, 0xFFBFC8FF, false);
        graphics.enableScissor(layout.gridX(), layout.gridTop(), layout.gridRight(), layout.gridBottom());
        for (int index = 0; index < this.characters.size(); index++) {
            CharacterDefinition definition = this.characters.get(index);
            CardPosition position = layout.characterPosition(index, this.characterScroll);
            if (position.y() + CHARACTER_H < layout.gridTop() || position.y() > layout.gridBottom()) continue;
            boolean selectedCard = definition.id().equals(this.selectedCharacter);
            boolean unavailable = this.occupied.contains(definition.id()) && !selectedCard;
            int background = unavailable ? 0xAA2A2026 : selectedCard ? 0xEE5A2D77 : 0xCC25253A;
            graphics.fill(position.x(), position.y(), position.x() + CHARACTER_W, position.y() + CHARACTER_H, background);
            String skinId = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, definition.id(), skinId,
                    position.x() + 8, position.y() + 5, 50, unavailable ? 90 : 255);
            Component characterName = Component.translatable(definition.nameKey());
            Component characterTitle = Component.translatable(definition.titleKey());
            graphics.text(this.font, this.font.plainSubstrByWidth(characterName.getString(), CHARACTER_W - 6),
                    position.x() + 3, position.y() + 57, unavailable ? 0xFF7C7478 : 0xFFFFFFFF, false);
            graphics.text(this.font, this.font.plainSubstrByWidth(characterTitle.getString(), CHARACTER_W - 6),
                    position.x() + 3, position.y() + 68, unavailable ? 0xFF6A6267 : 0xFFFFC75C, false);
        }
        graphics.disableScissor();

        graphics.text(this.font, Component.translatable("gui.astral_craft.board.skins"),
                layout.gridX(), layout.skinTitleY(), 0xFFBFC8FF, false);
        graphics.enableScissor(layout.gridX(), layout.skinY(), layout.gridRight(), layout.skinBottom());
        List<CharacterSkinDefinition> skins = selected.skins();
        for (int index = 0; index < skins.size(); index++) {
            CharacterSkinDefinition skin = skins.get(index);
            int x = layout.gridX() + index * (SKIN_W + GAP) - Math.round(this.skinScroll);
            boolean active = skin.id().equals(this.selectedSkin);
            graphics.fill(x, layout.skinY(), x + SKIN_W, layout.skinY() + SKIN_H,
                    active ? 0xEE77519A : 0xBB29293A);
            AstralStatusIconRenderer.renderCharacterSkinHead(graphics, selected.id(), skin.id(),
                    x + 8, layout.skinY() + 4, 38, 255);
            graphics.text(this.font,
                    this.font.plainSubstrByWidth(Component.translatable(skin.nameKey()).getString(), SKIN_W - 4),
                    x + 2, layout.skinY() + 46, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();

        boolean hover = inside(mouseX, mouseY, layout.buttonX(), layout.buttonY(), layout.buttonW(), layout.buttonH());
        graphics.fill(layout.buttonX(), layout.buttonY(), layout.buttonX() + layout.buttonW(),
                layout.buttonY() + layout.buttonH(), hover ? 0xFFF06AA8 : 0xFFD64B91);
        Component confirm = Component.translatable("gui.astral_craft.board.confirm");
        graphics.text(this.font, confirm, layout.buttonX() + (layout.buttonW() - this.font.width(confirm)) / 2,
                layout.buttonY() + 9, 0xFFFFFFFF, false);
        Component timer = Component.translatable("gui.astral_craft.board.timeout", (this.timeoutTicks + 19) / 20);
        graphics.text(this.font, timer, layout.buttonX() - this.font.width(timer) - 8,
                layout.buttonY() + 9, 0xFFBFC8FF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        for (int index = 0; index < this.characters.size(); index++) {
            CardPosition position = layout.characterPosition(index, this.characterScroll);
            if (!inside(event.x(), event.y(), position.x(), position.y(), CHARACTER_W, CHARACTER_H)
                    || event.y() < layout.gridTop() || event.y() > layout.gridBottom()) continue;
            CharacterDefinition definition = this.characters.get(index);
            if (!this.occupied.contains(definition.id()) || definition.id().equals(this.selectedCharacter)) {
                this.selectedCharacter = definition.id();
                this.selectedSkin = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
                this.skinScroll = 0.0F;
                this.preview = null;
            }
            return true;
        }
        List<CharacterSkinDefinition> skins = this.selected().skins();
        if (event.x() >= layout.gridX() && event.x() <= layout.gridRight()
                && event.y() >= layout.skinY() && event.y() <= layout.skinBottom()) {
            for (int index = 0; index < skins.size(); index++) {
                int x = layout.gridX() + index * (SKIN_W + GAP) - Math.round(this.skinScroll);
                if (inside(event.x(), event.y(), x, layout.skinY(), SKIN_W, SKIN_H)) {
                    this.selectedSkin = skins.get(index).id();
                    this.preview = null;
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
        if (mouseX >= layout.gridX() && mouseX <= layout.gridRight()
                && mouseY >= layout.skinTitleY() && mouseY <= layout.skinBottom()) {
            this.skinScroll = Mth.clamp(this.skinScroll - delta, 0.0F, this.maximumSkinScroll(layout));
        } else if (mouseX >= layout.gridX() && mouseX <= layout.gridRight()
                && mouseY >= layout.gridTop() && mouseY <= layout.gridBottom()) {
            this.characterScroll = Mth.clamp(this.characterScroll - delta, 0.0F,
                    this.maximumCharacterScroll(layout));
        } else {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        return true;
    }

    private void submit() {
        if (this.submitted) return;
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardCharacterSelectionPayload(this.boardId,
                this.selectedCharacter.toString(), this.selectedSkin));
        this.onClose();
    }

    private CharacterDefinition selected() {
        return this.characters.stream().filter(value -> value.id().equals(this.selectedCharacter))
                .findFirst().orElse(this.characters.getFirst());
    }

    private AstralCharacterEntity preview(CharacterDefinition definition) {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.preview == null && minecraft.level != null) {
            this.preview = new AstralCharacterEntity(AstralEntities.ASTRAL_CHARACTER.get(), minecraft.level);
        }
        if (this.preview != null) {
            this.preview.setCharacterId(definition.id());
            this.preview.setSkinId(this.selectedSkin);
            this.preview.setAnimationAction(definition.previewAction());
            this.preview.tickCount++;
        }
        return this.preview;
    }

    private void ensureSkin() {
        CharacterDefinition definition = this.selected();
        if (definition.skins().stream().noneMatch(value -> value.id().equals(this.selectedSkin))) {
            this.selectedSkin = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
        }
    }

    private float maximumCharacterScroll(Layout layout) {
        int rows = (this.characters.size() + layout.columns() - 1) / layout.columns();
        return Math.max(0, rows * (CHARACTER_H + GAP) - GAP - (layout.gridBottom() - layout.gridTop()));
    }

    private float maximumSkinScroll(Layout layout) {
        int content = this.selected().skins().size() * (SKIN_W + GAP) - GAP;
        return Math.max(0, content - (layout.gridRight() - layout.gridX()));
    }

    private Layout layout() {
        int top = MARGIN;
        int bottom = Math.max(top + 1, this.height - MARGIN);
        int leftWidth = Math.clamp(this.width / 3, 110, 250);
        int leftX = MARGIN;
        int right = Math.max(leftX + 2, this.width - MARGIN);
        int leftRight = Math.min(Math.max(leftX + 1, right - 126), leftX + leftWidth);
        int rightX = Math.min(right - 1, leftRight + GAP);
        int gridX = rightX + 10;
        int gridRight = right - 10;
        int columns = Math.max(1, (gridRight - gridX + GAP) / (CHARACTER_W + GAP));
        int buttonW = Math.min(110, Math.max(1, right - rightX - 14));
        int buttonH = 27;
        int buttonX = right - buttonW - 8;
        int buttonY = bottom - buttonH - 7;
        int skinBottom = buttonY - 8;
        int skinY = skinBottom - SKIN_H;
        int skinTitleY = skinY - 13;
        int gridTop = top + 24;
        int gridBottom = Math.max(gridTop + 1, skinTitleY - 6);
        return new Layout(top, bottom, leftX, leftRight, rightX, right, gridX, gridRight,
                gridTop, gridBottom, skinTitleY, skinY, skinBottom, columns,
                buttonX, buttonY, buttonW, buttonH);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static Identifier parse(String raw, Identifier fallback) {
        try {
            return Identifier.parse(raw);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static Set<Identifier> parseIds(String encoded) {
        Set<Identifier> result = new HashSet<>();
        if (encoded == null || encoded.isBlank()) return result;
        for (String raw : encoded.split(";")) {
            try {
                result.add(Identifier.parse(raw));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    private record CardPosition(int x, int y) {}

    private record Layout(int top, int bottom, int leftX, int leftRight, int rightX, int right,
                          int gridX, int gridRight, int gridTop, int gridBottom,
                          int skinTitleY, int skinY, int skinBottom, int columns,
                          int buttonX, int buttonY, int buttonW, int buttonH) {
        CardPosition characterPosition(int index, float scroll) {
            return new CardPosition(this.gridX + index % this.columns * (CHARACTER_W + GAP),
                    this.gridTop + index / this.columns * (CHARACTER_H + GAP) - Math.round(scroll));
        }
    }
}
