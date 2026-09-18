package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.gameplay.board.BoardMatchmakingMode;
import com.astral_craft.common.network.c2s.BoardMatchmakingCancelPayload;
import com.astral_craft.common.network.c2s.BoardMatchmakingModeSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload.PlayerEntry;
import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BoardMatchmakingModeSelectionScreen extends Screen {

    private static final int WIDTH = 330;
    private static final int BUTTON_HEIGHT = 30;
    private static final int MATCH_CANCEL_HEIGHT = 24;
    private static final int MATCHED_CARD_HEIGHT = 32;
    private static final int MATCHED_CARD_GAP = 4;
    private final UUID boardId;
    private State state;
    private List<PlayerEntry> players;
    private int elapsedTicks;
    private int countdownTicks;
    private boolean tutorial;
    private boolean cancelSent;

    private BoardMatchmakingModeSelectionScreen(OpenBoardMatchmakingModeSelectionPayload payload) {
        super(Component.translatable("gui.astral_craft.board.matchmaking.title"));
        this.boardId = payload.boardId();
        this.state = payload.state();
        this.players = payload.players();
        this.elapsedTicks = payload.elapsedTicks();
        this.countdownTicks = payload.countdownTicks();
        BoardTutorialGuide.clear(this.boardId);
    }

    public static void open(OpenBoardMatchmakingModeSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof BoardMatchmakingModeSelectionScreen screen
                    && screen.boardId.equals(payload.boardId())) {
                screen.refresh(payload);
                return;
            }
            minecraft.setScreen(new BoardMatchmakingModeSelectionScreen(payload));
        });
    }

    private void refresh(OpenBoardMatchmakingModeSelectionPayload payload) {
        this.state = payload.state();
        this.players = payload.players();
        this.elapsedTicks = payload.elapsedTicks();
        this.countdownTicks = payload.countdownTicks();
        this.cancelSent = false;
        if (this.state != State.IDLE) this.tutorial = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.state == State.IDLE;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.state == State.WAITING && this.elapsedTicks < Integer.MAX_VALUE) this.elapsedTicks++;
        if (this.state == State.MATCHED && this.countdownTicks > 0) this.countdownTicks--;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.state == State.MATCHED) {
            this.renderMatched(graphics);
            return;
        }
        this.renderModeSelection(graphics, mouseX, mouseY);
    }

    private void renderModeSelection(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean waiting = this.state == State.WAITING;
        Layout layout = this.layout(waiting);
        AstralFancyButton.renderOutlinedBox(graphics, layout.x(), layout.y(), WIDTH, layout.height(),
                0xF0181822, 0xFFEEDFFF, 0xFF493D58, 1, 2);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 14, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.single_hint"),
                layout.x() + 16, layout.y() + 42, waiting ? 0xFF777782 : 0xFFD9D9E4, false);
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.multiplayer_hint"),
                layout.x() + 16, layout.y() + 58, waiting || this.tutorial ? 0xFF777782 : 0xFFD9D9E4, false);

        if (waiting) {
            graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.matchmaking.matching"),
                    this.width / 2, layout.y() + 76, 0xFFFFD87A);
            graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.matchmaking.waiting_status",
                            this.formatElapsed(), Math.max(1, this.players.size()), OpenBoardMatchmakingModeSelectionPayload.MAXIMUM_PLAYERS),
                    this.width / 2, layout.y() + 89, 0xFFFFFFFF);
        }

        int checkboxX = layout.x() + 18;
        int checkboxY = layout.tutorialY();
        int checkboxOuter = waiting ? 0xFF777782 : 0xFFEAEAF2;
        int checkboxInner = waiting ? 0xFF24242D : 0xFF161620;
        graphics.fill(checkboxX, checkboxY, checkboxX + 12, checkboxY + 12, checkboxOuter);
        graphics.fill(checkboxX + 1, checkboxY + 1, checkboxX + 11, checkboxY + 11, checkboxInner);
        if (this.tutorial) {
            graphics.fill(checkboxX + 3, checkboxY + 5, checkboxX + 6, checkboxY + 9, 0xFFFFD85A);
            graphics.fill(checkboxX + 5, checkboxY + 7, checkboxX + 10, checkboxY + 9, 0xFFFFD85A);
        }
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.tutorial"),
                checkboxX + 18, checkboxY + 2, waiting ? 0xFF777782 : 0xFFFFFFFF, false);
        if (this.tutorial && !waiting) {
            graphics.text(this.font, Component.translatable("gui.astral_craft.board.matchmaking.tutorial_single_only"),
                    checkboxX + 18, checkboxY + 18, 0xFFFFD87A, false);
        }

        boolean singleHover = !waiting && inside(mouseX, mouseY, layout.x() + 16, layout.singleY(), WIDTH - 32, BUTTON_HEIGHT);
        boolean multiHover = !waiting && !this.tutorial
                && inside(mouseX, mouseY, layout.x() + 16, layout.multiY(), WIDTH - 32, BUTTON_HEIGHT);
        boolean cancelHover = !waiting && inside(mouseX, mouseY, layout.x() + 16, layout.cancelY(), WIDTH - 32, layout.cancelHeight());
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.matchmaking.single"),
                layout.x() + 16, layout.singleY(), WIDTH - 32, BUTTON_HEIGHT, false, singleHover,
                ButtonStyle.button(waiting ? 0xFF4C4652 : 0xFFB83C82));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.matchmaking.multiplayer"),
                layout.x() + 16, layout.multiY(), WIDTH - 32, BUTTON_HEIGHT, false, multiHover,
                ButtonStyle.button(waiting || this.tutorial ? 0xFF4C4652 : 0xFF4B72B6));
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.cancel"),
                layout.x() + 16, layout.cancelY(), WIDTH - 32, layout.cancelHeight(), false, cancelHover,
                ButtonStyle.button(waiting ? 0xFF4C4652 : 0xFF5B5267));
        if (waiting) {
            boolean matchCancelHover = !this.cancelSent
                    && inside(mouseX, mouseY, layout.x() + 16, layout.matchCancelY(), WIDTH - 32, MATCH_CANCEL_HEIGHT);
            AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.matchmaking.cancel_matching"),
                    layout.x() + 16, layout.matchCancelY(), WIDTH - 32, MATCH_CANCEL_HEIGHT, false, matchCancelHover,
                    ButtonStyle.button(this.cancelSent ? 0xFF4C4652 : 0xFFB84463));
        }
    }

    private void renderMatched(GuiGraphicsExtractor graphics) {
        MatchedLayout layout = this.matchedLayout();
        AstralFancyButton.renderOutlinedBox(graphics, layout.x(), layout.y(), WIDTH, layout.height(),
                0xF0181822, 0xFFFFE09A, 0xFF5D465E, 1, 2);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.y() + 12, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.matchmaking.matched"),
                this.width / 2, layout.y() + 34, 0xFFFFD85A);
        int seconds = Math.max(0, (this.countdownTicks + 19) / 20);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.matchmaking.matched_countdown", seconds),
                this.width / 2, layout.y() + 49, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.astral_craft.board.matchmaking.order_hint"),
                this.width / 2, layout.y() + 64, 0xFFBFC8FF);

        Minecraft minecraft = Minecraft.getInstance();
        for (int index = 0; index < this.players.size() && index < OpenBoardMatchmakingModeSelectionPayload.MAXIMUM_PLAYERS; index++) {
            PlayerEntry player = this.players.get(index);
            int x = layout.x() + 16;
            int y = layout.cardY(index);
            int cardWidth = WIDTH - 32;
            AstralFancyButton.renderOutlinedBox(graphics, x, y, cardWidth, MATCHED_CARD_HEIGHT,
                    0xD00B0B13, 0xFF695C75, 0xFF2D2834, 1, 1);
            PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player.playerId());
            PlayerSkin skin = info == null ? DefaultPlayerSkin.get(player.playerId()) : info.getSkin();
            PlayerFaceExtractor.extractRenderState(graphics, skin, x + 5, y + 4, 24);
            String name = this.font.plainSubstrByWidth(player.playerName(), cardWidth - 47);
            graphics.text(this.font, name, x + 36, y + 12, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        if (this.state == State.MATCHED) return true;
        boolean waiting = this.state == State.WAITING;
        Layout layout = this.layout(waiting);
        if (waiting) {
            if (inside(event.x(), event.y(), layout.x() + 16, layout.matchCancelY(), WIDTH - 32, MATCH_CANCEL_HEIGHT)) {
                this.cancelMatchmaking(true);
            }
            return true;
        }
        if (inside(event.x(), event.y(), layout.x() + 16, layout.tutorialY() - 3, WIDTH - 32, 35)) {
            this.tutorial = !this.tutorial;
            return true;
        }
        if (inside(event.x(), event.y(), layout.x() + 16, layout.singleY(), WIDTH - 32, BUTTON_HEIGHT)) {
            this.submit(BoardMatchmakingMode.SINGLE_PLAYER);
            return true;
        }
        if (!this.tutorial && inside(event.x(), event.y(), layout.x() + 16, layout.multiY(), WIDTH - 32, BUTTON_HEIGHT)) {
            this.submit(BoardMatchmakingMode.MULTIPLAYER);
            return true;
        }
        if (inside(event.x(), event.y(), layout.x() + 16, layout.cancelY(), WIDTH - 32, layout.cancelHeight())) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (this.state != State.IDLE) this.cancelMatchmaking(false);
        super.onClose();
    }

    @Override
    public void removed() {
        if (this.state != State.IDLE && !this.cancelSent) this.cancelMatchmaking(false);
        super.removed();
    }

    private void submit(BoardMatchmakingMode mode) {
        if (mode == BoardMatchmakingMode.SINGLE_PLAYER) {
            if (this.tutorial) BoardTutorialGuide.start(this.boardId);
            else BoardTutorialGuide.clear(this.boardId);
            ClientPacketDistributor.sendToServer(new BoardMatchmakingModeSelectionPayload(this.boardId, mode, this.tutorial));
            super.onClose();
            return;
        }

        BoardTutorialGuide.clear(this.boardId);
        this.tutorial = false;
        this.state = State.WAITING;
        this.players = List.of();
        this.elapsedTicks = 0;
        this.countdownTicks = 0;
        this.cancelSent = false;
        ClientPacketDistributor.sendToServer(new BoardMatchmakingModeSelectionPayload(this.boardId, mode, false));
    }

    private void cancelMatchmaking(boolean returnToSelection) {
        if (this.cancelSent || this.state == State.IDLE) return;
        this.cancelSent = true;
        ClientPacketDistributor.sendToServer(new BoardMatchmakingCancelPayload(this.boardId, returnToSelection));
    }

    private String formatElapsed() {
        int seconds = Math.max(0, this.elapsedTicks / 20);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private Layout layout(boolean waiting) {
        int height = 238;
        int x = (this.width - WIDTH) / 2;
        int y = (this.height - height) / 2;
        if (waiting) return new Layout(x, y, height, y + 102, y + 122, y + 156, y + 190, 18, y + 210);
        return new Layout(x, y, height, y + 82, y + 121, y + 158, y + 199, 25, -1);
    }

    private MatchedLayout matchedLayout() {
        int height = 232;
        int x = (this.width - WIDTH) / 2;
        int y = (this.height - height) / 2;
        return new MatchedLayout(x, y, height, y + 79);
    }

    private static boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private record Layout(int x, int y, int height, int tutorialY, int singleY, int multiY,
                          int cancelY, int cancelHeight, int matchCancelY) {}

    private record MatchedLayout(int x, int y, int height, int firstCardY) {
        private int cardY(int index) {
            return this.firstCardY + index * (MATCHED_CARD_HEIGHT + MATCHED_CARD_GAP);
        }
    }
}
