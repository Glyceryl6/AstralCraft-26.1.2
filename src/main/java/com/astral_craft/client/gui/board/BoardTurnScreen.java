package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.CardRevealOverlay;
import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.c2s.BoardLeavePayload;
import com.astral_craft.common.network.c2s.BoardMoveRequestPayload;
import com.astral_craft.common.network.c2s.BoardSkillRequestPayload;
import com.astral_craft.common.network.s2c.OpenBoardTurnPayload;
import com.astral_craft.common.network.c2s.UseBoardCardPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import java.util.List;

/** Board-specific non-stacking hand UI with skill and movement controls. */
public class BoardTurnScreen extends Screen {

    private static final int PANEL_H = 178;
    private static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int GAP = 7;
    private final String boardId;
    private final int characterEntityId;
    private List<BoardCard> cards;
    private int maxCardPlays;
    private int cardPlaysUsed;
    private int skillCooldown;
    private int decisionTicks;
    private int decisionDurationTicks;
    private Identifier characterId;
    private Identifier skinId;
    private boolean currentTurn;
    private float scroll;
    private int draggingIndex = -1;
    private int dragOffsetX;
    private int dragOffsetY;
    private int requestLockTicks;

    public BoardTurnScreen(OpenBoardTurnPayload payload) {
        super(Component.translatable("gui.astral_craft.board.turn"));
        this.boardId = payload.boardId();
        this.characterEntityId = payload.characterEntityId();
        this.cards = decode(payload.encodedCards());
        this.refresh(payload);
    }

    public static void open(OpenBoardTurnPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof BoardTurnScreen boardTurnScreen
                    && boardTurnScreen.boardId.equals(payload.boardId())
                    && boardTurnScreen.characterEntityId == payload.characterEntityId()) {
                boardTurnScreen.refresh(payload);
            } else {
                Minecraft.getInstance().setScreen(new BoardTurnScreen(payload));
            }
        });
    }

    private void refresh(OpenBoardTurnPayload payload) {
        this.cards = decode(payload.encodedCards());
        this.cardPlaysUsed = Math.max(0, payload.cardPlaysUsed());
        this.maxCardPlays = Math.max(0, payload.maxCardPlays());
        this.skillCooldown = Math.max(0, payload.skillCooldownTurns());
        this.decisionTicks = Math.max(0, payload.decisionTicks());
        this.decisionDurationTicks = Math.max(1, payload.decisionDurationTicks());
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
        this.currentTurn = payload.currentTurn();
        this.draggingIndex = -1;
        this.requestLockTicks = 0;
        this.scroll = Math.clamp(this.scroll, 0.0F, this.maximumScroll(this.layout()));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (this.decisionTicks > 0) this.decisionTicks--;
        if (this.requestLockTicks > 0) this.requestLockTicks--;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (CardRevealOverlay.isActive()) return;
        Layout layout = this.layout();
        graphics.fill(0, layout.top(), this.width, this.height, 0xED090911);
        graphics.fill(0, layout.top(), this.width, layout.top() + 2, 0xB0FFFFFF);
        graphics.text(this.font, this.title, 12, layout.top() + 9, 0xFFFFFFFF, false);
        boolean leaveHover = inside(mouseX, mouseY, layout.leaveX(), layout.leaveY(), layout.leaveW(), 22);
        AstralFancyButton.renderButton(graphics, this.font,
                Component.translatable("gui.astral_craft.board.leave"),
                layout.leaveX(), layout.leaveY(), layout.leaveW(), 22, false, leaveHover,
                ButtonStyle.button(0xFF8E3542));

        graphics.enableScissor(layout.cardLeft(), layout.cardTop(), layout.cardRight(), layout.cardBottom());
        int x = 12 - Math.round(this.scroll);
        int y = layout.cardY();
        for (int index = 0; index < this.cards.size(); index++) {
            BoardCard card = this.cards.get(index);
            if (index != this.draggingIndex && x + CARD_W >= layout.cardLeft() && x <= layout.cardRight()) {
                this.renderCard(graphics, card, x, y, mouseX, mouseY, false);
            }
            x += CARD_W + GAP;
        }
        graphics.disableScissor();
        if (this.draggingIndex >= 0 && this.draggingIndex < this.cards.size()) {
            this.renderCard(graphics, this.cards.get(this.draggingIndex), mouseX - this.dragOffsetX,
                    mouseY - this.dragOffsetY, mouseX, mouseY, true);
        }

        boolean busy = this.requestLockTicks > 0 || CardRevealOverlay.isActive();
        boolean moveEnabled = this.currentTurn && !busy;
        boolean moveHover = moveEnabled && inside(mouseX, mouseY, layout.moveX(), layout.moveY(),
                layout.moveSize(), layout.moveSize());
        Component move = Component.translatable("gui.astral_craft.board.move");
        AstralFancyButton.renderButton(graphics, this.font, move, layout.moveX(), layout.moveY(),
                layout.moveSize(), layout.moveSize(), false, moveHover,
                ButtonStyle.button(moveEnabled ? 0xFFD84484 : 0xFF555560));

        Component play = Component.translatable("gui.astral_craft.board.play_card");
        Component count = Component.translatable("gui.astral_craft.board.card_count",
                this.cardPlaysUsed, this.maxCardPlays);
        graphics.text(this.font, play, layout.infoX(), layout.infoY(), 0xFFFFFFFF, false);
        graphics.text(this.font, count, layout.infoX(), layout.infoY() + 17, 0xFFFFC75C, false);

        int skillH = 25;
        boolean skillEnabled = this.currentTurn && this.skillCooldown <= 0 && !busy;
        boolean skillHover = skillEnabled && inside(mouseX, mouseY, layout.skillX(), layout.skillY(),
                layout.skillW(), skillH);
        Component skill = this.skillCooldown <= 0
                ? Component.translatable("gui.astral_craft.board.skill")
                : Component.translatable("gui.astral_craft.board.skill_cooldown", this.skillCooldown);
        AstralFancyButton.renderButton(graphics, this.font, skill, layout.skillX(), layout.skillY(),
                layout.skillW(), skillH, false, skillHover,
                ButtonStyle.button(skillEnabled ? 0xFF4D7AC7 : 0xFF555560));
        BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId,
                this.decisionTicks, this.decisionDurationTicks, this.width / 2, this.height - 17,
                Math.min(270, this.width - 44));
    }

    private void renderCard(GuiGraphicsExtractor graphics, BoardCard card, int x, int y,
                            int mouseX, int mouseY, boolean dragging) {
        boolean playable = this.canPlayCard(card);
        HandCardRenderHelper.renderFramedCard(graphics, this.font, card.definition().type(),
                card.definition().largeFrontTexture(card.stack()), card.definition().displayName(card.stack()),
                x, y, mouseX, mouseY, dragging);
        if (!playable) graphics.fill(x, y, x + CARD_W, y + CARD_H, 0x77000000);
    }

    private boolean canPlayCard(BoardCard card) {
        return this.currentTurn && this.requestLockTicks <= 0 && !CardRevealOverlay.isActive()
                && this.cardPlaysUsed < this.maxCardPlays && card.definition().type() == CardType.EFFECT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.leaveX(), layout.leaveY(), layout.leaveW(), 22)) {
            ClientPacketDistributor.sendToServer(new BoardLeavePayload(this.boardId));
            this.onClose();
            return true;
        }
        boolean busy = this.requestLockTicks > 0 || CardRevealOverlay.isActive();
        if (inside(event.x(), event.y(), layout.moveX(), layout.moveY(),
                layout.moveSize(), layout.moveSize()) && this.currentTurn && !busy) {
            this.requestLockTicks = 8;
            ClientPacketDistributor.sendToServer(new BoardMoveRequestPayload(this.boardId));
            this.onClose();
            return true;
        }
        if (inside(event.x(), event.y(), layout.skillX(), layout.skillY(), layout.skillW(), 25)
                && this.currentTurn && this.skillCooldown <= 0 && !busy) {
            this.requestLockTicks = 8;
            ClientPacketDistributor.sendToServer(new BoardSkillRequestPayload(this.boardId));
            return true;
        }
        int x = 12 - Math.round(this.scroll);
        int y = layout.cardY();
        for (int index = 0; index < this.cards.size(); index++) {
            if (event.x() >= layout.cardLeft() && event.x() <= layout.cardRight()
                    && event.y() >= layout.cardTop() && event.y() <= layout.cardBottom()
                    && inside(event.x(), event.y(), x, y, CARD_W, CARD_H)) {
                BoardCard card = this.cards.get(index);
                if (this.canPlayCard(card)) {
                    this.draggingIndex = index;
                    this.dragOffsetX = Math.clamp((int) event.x() - x, 0, CARD_W);
                    this.dragOffsetY = Math.clamp((int) event.y() - y, 0, CARD_H);
                }
                return true;
            }
            x += CARD_W + GAP;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingIndex >= 0) {
            int index = this.draggingIndex;
            this.draggingIndex = -1;
            if (event.y() < this.layout().top() - 6 && this.canPlayCard(this.cards.get(index))) {
                this.requestLockTicks = 8;
                ClientPacketDistributor.sendToServer(new UseBoardCardPayload(
                        this.boardId, this.cards.get(index).originalIndex()));
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Layout layout = this.layout();
        if (mouseX < layout.cardLeft() || mouseX > layout.cardRight()
                || mouseY < layout.cardTop() || mouseY > layout.cardBottom()) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        this.scroll = Mth.clamp(this.scroll - (float) (deltaY + deltaX) * 38.0F,
                0.0F, this.maximumScroll(layout));
        return true;
    }

    private float maximumScroll(Layout layout) {
        int content = Math.max(0, this.cards.size() * (CARD_W + GAP) - GAP);
        int visible = Math.max(1, layout.cardRight() - layout.cardLeft());
        return Math.max(0, content - visible);
    }

    private Layout layout() {
        int panelHeight = Math.clamp(this.height, 1, PANEL_H);
        int top = Math.max(0, this.height - panelHeight);
        int controlsWidth = Math.clamp(this.width - 22, 1, Math.clamp(this.width / 3, 118, 154));
        int cardLeft = 10;
        int cardRight = Math.max(cardLeft + 1, this.width - controlsWidth - 8);
        int cardTop = Math.min(this.height, top + 32);
        int cardBottom = Math.max(cardTop + 1, this.height - 10);
        int cardY = top + 40;
        int moveSize = Math.clamp(controlsWidth - 28, 36, 72);
        int moveX = Math.max(cardRight + 6, this.width - moveSize - 14);
        int moveY = Math.max(top + 48, this.height - moveSize - 16);
        int skillX = cardRight + 7;
        int skillW = Math.max(1, this.width - skillX - 10);
        int skillY = top + 28;
        int infoX = skillX;
        int infoY = skillY + 36;
        int leaveW = Math.min(88, Math.max(58, controlsWidth - 20));
        int leaveX = this.width - leaveW - 10;
        int leaveY = top + 5;
        return new Layout(top, cardLeft, cardTop, cardRight, cardBottom, cardY,
                moveX, moveY, moveSize, skillX, skillY, skillW, infoX, infoY,
                leaveX, leaveY, leaveW);
    }

    private static List<BoardCard> decode(String encoded) {
        List<BoardCard> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) return result;
        String[] ids = encoded.split(";");
        for (int index = 0; index < ids.length; index++) {
            try {
                Identifier id = Identifier.parse(ids[index]);
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (!(item instanceof BaseHandCard card)) continue;
                ItemStack stack = new ItemStack(item);
                result.add(new BoardCard(index, stack, card.definition(stack)));
            } catch (Exception ignored) {}
        }
        return List.copyOf(result);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private record BoardCard(int originalIndex, ItemStack stack, CardDefinition definition) {}

    private record Layout(int top, int cardLeft, int cardTop, int cardRight, int cardBottom, int cardY,
                          int moveX, int moveY, int moveSize, int skillX, int skillY, int skillW,
                          int infoX, int infoY, int leaveX, int leaveY, int leaveW) {}

}