package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralConfirmationScreen;
import com.astral_craft.common.gameplay.board.BoardMatchmakingMode;
import com.astral_craft.common.network.c2s.BoardMatchmakingModeSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardMatchmakingModeSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class BoardMatchmakingModeSelectionScreen {

    public static void open(OpenBoardMatchmakingModeSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new AstralConfirmationScreen(
                Component.translatable("gui.astral_craft.board.matchmaking.title"),
                List.of(Component.translatable("gui.astral_craft.board.matchmaking.single_hint"),
                        Component.translatable("gui.astral_craft.board.matchmaking.multiplayer_hint")),
                Component.translatable("gui.astral_craft.board.matchmaking.single"),
                Component.translatable("gui.astral_craft.board.matchmaking.multiplayer"),
                Component.translatable("gui.cancel"),
                () -> submit(payload, BoardMatchmakingMode.SINGLE_PLAYER),
                () -> submit(payload, BoardMatchmakingMode.MULTIPLAYER))));
    }

    private static void submit(OpenBoardMatchmakingModeSelectionPayload payload, BoardMatchmakingMode mode) {
        ClientPacketDistributor.sendToServer(new BoardMatchmakingModeSelectionPayload(payload.boardId(), mode));
    }
}
