package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.CardRevealOverlay;
import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.BoardPanelPlacementCard;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.BoardCardView;
import com.astral_craft.common.network.c2s.*;
import com.astral_craft.common.network.s2c.OpenBoardTurnPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Board-specific non-stacking hand UI with skill and movement controls. */
public class BoardTurnScreen extends Screen {

    private static final int PANEL_H = 136;
    private static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int GAP = 7;
    private final UUID boardId;
    private final int characterEntityId;
    private List<BoardCard> cards;
    private int maxCardPlays;
    private int cardPlaysUsed;
    private int decisionTicks;
    private int decisionDurationTicks;
    private Identifier characterId;
    private Identifier skinId;
    private boolean currentTurn;
    private boolean counterResponse;
    private float scroll;
    private int draggingIndex = -1;
    private int dragOffsetX;
    private int dragOffsetY;
    private int requestLockTicks;
    private static OpenBoardTurnPayload pendingCounterPayload;

    public BoardTurnScreen(OpenBoardTurnPayload payload) {
        super(Component.translatable("gui.astral_craft.board.turn"));
        this.boardId = payload.boardId();
        this.characterEntityId = payload.characterEntityId();
        this.cards = cards(payload.cards());
        this.refresh(payload);
    }

    public static void open(OpenBoardTurnPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.counterResponse()) pendingCounterPayload = payload;
            else pendingCounterPayload = null;
            openNow(payload);
        });
    }

    public static void restorePendingCounterScreen() {
        OpenBoardTurnPayload payload = pendingCounterPayload;
        if (payload != null) openNow(payload);
    }

    private static void clearPendingCounterScreen() {
        pendingCounterPayload = null;
    }

    private static void openNow(OpenBoardTurnPayload payload) {
        Screen current = Minecraft.getInstance().screen;
        if (current instanceof BoardTurnScreen boardTurnScreen
                && boardTurnScreen.boardId.equals(payload.boardId())
                && boardTurnScreen.characterEntityId == payload.characterEntityId()) {
            boardTurnScreen.refresh(payload);
        } else {
            Minecraft.getInstance().setScreen(new BoardTurnScreen(payload));
        }
    }

    private void refresh(OpenBoardTurnPayload payload) {
        this.cards = cards(payload.cards());
        this.cardPlaysUsed = Math.max(0, payload.cardPlaysUsed());
        this.maxCardPlays = Math.max(0, payload.maxCardPlays());
        this.decisionTicks = Math.max(0, payload.decisionTicks());
        this.decisionDurationTicks = Math.max(1, payload.decisionDurationTicks());
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
        this.currentTurn = payload.currentTurn();
        this.counterResponse = payload.counterResponse();
        this.draggingIndex = -1;
        this.requestLockTicks = 0;
        this.scroll = Math.clamp(this.scroll, 0.0F, this.maximumScroll(this.layout()));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.counterResponse;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.decisionTicks > 0) this.decisionTicks--;
        if (this.requestLockTicks > 0) this.requestLockTicks--;
        if (this.counterResponse && this.decisionTicks <= 0 && Minecraft.getInstance().screen == this) {
            clearPendingCounterScreen();
            this.onClose();
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (CardRevealOverlay.isActive() && !this.counterResponse) return;
        Layout layout = this.layout();
        BoardTutorialGuide.beginFrame(this.boardId);
        graphics.fill(0, layout.top(), this.width, this.height, 0xED090911);
        graphics.fill(0, layout.top(), this.width, layout.top() + 2, 0xB0FFFFFF);
        graphics.text(this.font, this.title, 12, layout.top() + 9, 0xFFFFFFFF, false);
        boolean leaveHover = inside(mouseX, mouseY, layout.leaveX(), layout.leaveY(), layout.leaveW(), 22);
        Component leaveText = this.counterResponse
                ? Component.translatable("gui.astral_craft.board.counter_cancel")
                : Component.translatable("gui.astral_craft.board.leave");
        AstralFancyButton.renderButton(graphics, this.font, leaveText,
                layout.leaveX(), layout.leaveY(), layout.leaveW(), 22, false, leaveHover,
                ButtonStyle.button(this.counterResponse ? 0xFF5E566F : 0xFF8E3542));
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
        if (this.counterResponse) {
            graphics.text(this.font, Component.translatable("gui.astral_craft.board.counter_prompt"),
                    layout.infoX(), layout.infoY(), 0xFFFFD87A, false);
        } else {
            boolean moveEnabled = this.currentTurn && !busy;
            boolean moveHover = moveEnabled && inside(mouseX, mouseY, layout.moveX(), layout.moveY(),
                    layout.moveSize(), layout.moveSize());
            Component move = Component.translatable("gui.astral_craft.board.move");
            AstralFancyButton.renderButton(graphics, this.font, move, layout.moveX(), layout.moveY(),
                    layout.moveSize(), layout.moveSize(), false, moveHover,
                    ButtonStyle.button(moveEnabled ? 0xFFD84484 : 0xFF555560));
            Component play = Component.translatable("gui.astral_craft.board.play_card");
            Component count = Component.translatable("gui.astral_craft.board.card_count", this.cardPlaysUsed, this.maxCardPlays);
            graphics.text(this.font, play, layout.infoX(), layout.infoY(), 0xFFFFFFFF, false);
            graphics.text(this.font, count, layout.infoX(), layout.infoY() + 17, 0xFFFFC75C, false);
            Component skill = Component.translatable("gui.astral_craft.board.skill_unimplemented");
            AstralFancyButton.renderButton(graphics, this.font, skill, layout.skillX(),
                    layout.skillY(), layout.skillW(), 25, false, false, AstralFancyButton.disabledButtonStyle());
        }
        this.renderTutorial(graphics, mouseX, mouseY, layout);
        BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId,
                this.decisionTicks, this.decisionDurationTicks, this.width / 2,
                this.height - 17, Math.min(270, this.width - 44));
    }

    private void renderTutorial(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Layout layout) {
        if (!BoardTutorialGuide.active(this.boardId)) return;
        int width = Math.clamp(this.width - 24, 180, 420);
        int x = 12;
        int bottom = layout.top() - 6;
        BoardCard hovered = this.hoveredCard(mouseX, mouseY, layout);
        if (this.counterResponse) {
            int height = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                    BoardTutorialGuide.Hint.COUNTER, x, bottom, width);
            bottom -= height > 0 ? height + 5 : 0;
        } else if (hovered != null) {
            BoardTutorialGuide.Hint hint = this.cardTutorialHint(hovered);
            if (hint != null) {
                Component message = Component.translatable(hint.translationKey());
                graphics.setTooltipForNextFrame(this.font, this.font.split(message, Math.min(280, Math.max(120, this.width - 40))),
                        mouseX, mouseY);
            }
        }
        int handHeight = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                BoardTutorialGuide.Hint.HAND_DRAG, x, bottom, width);
        if (handHeight > 0) bottom -= handHeight + 5;
        int decisionTimeHeight = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                BoardTutorialGuide.Hint.DECISION_TIME, x, bottom, width);
        if (decisionTimeHeight > 0) bottom -= decisionTimeHeight + 5;
        int protectionHeight = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                BoardTutorialGuide.Hint.PROTECTION, x, bottom, width);
        if (protectionHeight > 0) bottom -= protectionHeight + 5;
        if (handHeight == 0) {
            BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                    BoardTutorialGuide.Hint.BRANCH, x, bottom, width);
        }
    }

    private BoardCard hoveredCard(int mouseX, int mouseY, Layout layout) {
        if (this.draggingIndex >= 0 || mouseX < layout.cardLeft() || mouseX > layout.cardRight()
                || mouseY < layout.cardTop() || mouseY > layout.cardBottom()) return null;
        int x = 12 - Math.round(this.scroll);
        for (BoardCard card : this.cards) {
            if (inside(mouseX, mouseY, x, layout.cardY(), CARD_W, CARD_H)) return card;
            x += CARD_W + GAP;
        }
        return null;
    }

    private BoardTutorialGuide.Hint cardTutorialHint(BoardCard card) {
        if (card.definition().type() == CardType.COUNTER) return BoardTutorialGuide.Hint.COUNTER;
        if (card.definition().type() != CardType.EFFECT) return null;
        if (card.stack().getItem() instanceof BoardPanelPlacementCard) return BoardTutorialGuide.Hint.TARGET_PLATFORM;
        return card.definition().needsTarget() ? BoardTutorialGuide.Hint.TARGET_CHARACTER : BoardTutorialGuide.Hint.TARGET_SELF;
    }

    private void renderCard(GuiGraphicsExtractor graphics, BoardCard card, int x, int y, int mouseX, int mouseY, boolean dragging) {
        boolean playable = this.canPlayCard(card);
        HandCardRenderHelper.renderFramedCard(graphics, this.font, card.definition().type(),
                card.definition().largeFrontTexture(card.stack()), card.definition().displayName(card.stack()),
                x, y, mouseX, mouseY, dragging);
        if (!playable) graphics.fill(x, y, x + CARD_W, y + CARD_H, 0x77000000);
    }

    private boolean canPlayCard(BoardCard card) {
        if (!card.playable()) return false;
        if (this.counterResponse) {
            return this.requestLockTicks <= 0 && card.definition().type() == CardType.COUNTER;
        }
        return this.currentTurn && this.requestLockTicks <= 0 && !CardRevealOverlay.isActive()
                && this.cardPlaysUsed < this.maxCardPlays && card.definition().type() == CardType.EFFECT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (BoardTutorialGuide.mouseClicked(this.boardId, event.x(), event.y())) return true;
        Layout layout = this.layout();
        if (inside(event.x(), event.y(), layout.leaveX(), layout.leaveY(), layout.leaveW(), 22)) {
            if (this.counterResponse) {
                clearPendingCounterScreen();
                ClientPacketDistributor.sendToServer(new BoardCounterResponsePayload(this.boardId, -1));
            } else {
                ClientPacketDistributor.sendToServer(new BoardLeavePayload(this.boardId));
            }
            this.onClose();
            return true;
        }

        boolean busy = this.requestLockTicks > 0 || CardRevealOverlay.isActive();
        if (!this.counterResponse && inside(event.x(), event.y(), layout.moveX(), layout.moveY(),
                layout.moveSize(), layout.moveSize()) && this.currentTurn && !busy) {
            this.requestLockTicks = 8;
            ClientPacketDistributor.sendToServer(new BoardMoveRequestPayload(this.boardId));
            this.onClose();
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
                if (this.counterResponse) {
                    clearPendingCounterScreen();
                    ClientPacketDistributor.sendToServer(new BoardCounterResponsePayload(
                            this.boardId, this.cards.get(index).originalIndex()));
                } else {
                    ClientPacketDistributor.sendToServer(new UseBoardCardPayload(
                            this.boardId, this.cards.get(index).originalIndex()));
                }
                this.onClose();
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
        int controlsWidth = Math.clamp(this.width / 4, 112, 142);
        int cardLeft = 10;
        int cardRight = Math.max(cardLeft + 1, this.width - controlsWidth - 8);
        int cardTop = Math.min(this.height, top + 31);
        int cardBottom = Math.max(cardTop + 1, this.height - 7);
        int cardY = top + 34;
        int moveSize = Math.clamp(controlsWidth - 64, 40, 52);
        int moveX = this.width - moveSize - 10;
        int moveY = this.height - moveSize - 9;
        int skillX = cardRight + 7;
        int skillW = Math.max(1, this.width - skillX - 10);
        int skillY = top + 31;
        int infoX = skillX;
        int infoY = skillY + 31;
        int leaveW = Math.clamp(controlsWidth - 24, 54, 78);
        int leaveX = this.width - leaveW - 10;
        int leaveY = top + 5;
        return new Layout(top, cardLeft, cardTop, cardRight, cardBottom, cardY,
                moveX, moveY, moveSize, skillX, skillY, skillW, infoX, infoY,
                leaveX, leaveY, leaveW);
    }

    private static List<BoardCard> cards(List<BoardCardView> views) {
        List<BoardCard> result = new ArrayList<>();
        for (BoardCardView view : views) {
            ItemStack stack = view.stack().copy();
            CardDefinition definition = stack.get(AstralDataComponents.CARD_DEFINITION);
            if (definition == null && stack.getItem() instanceof BaseHandCard card) {
                definition = card.definition(stack);
            }
            if (definition != null) result.add(new BoardCard(view.handIndex(), stack, definition, view.playable()));
        }
        return List.copyOf(result);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private record BoardCard(int originalIndex, ItemStack stack, CardDefinition definition, boolean playable) {}

    private record Layout(int top, int cardLeft, int cardTop, int cardRight, int cardBottom, int cardY,
                          int moveX, int moveY, int moveSize, int skillX, int skillY, int skillW,
                          int infoX, int infoY, int leaveX, int leaveY, int leaveW) {}

}