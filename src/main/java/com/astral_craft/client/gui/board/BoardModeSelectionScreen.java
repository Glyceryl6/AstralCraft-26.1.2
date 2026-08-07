package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralConfirmationScreen;
import com.astral_craft.common.gameplay.board.BoardMode;
import com.astral_craft.common.network.c2s.BoardModeSelectionPayload;
import com.astral_craft.common.network.s2c.OpenBoardModeSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class BoardModeSelectionScreen {

    public static void open(OpenBoardModeSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new AstralConfirmationScreen(
                Component.translatable("gui.astral_craft.board.mode.title"),
                List.of(Component.translatable("gui.astral_craft.board.mode.common_only"),
                        Component.translatable("gui.astral_craft.board.mode.warning")),
                Component.translatable("gui.astral_craft.board.mode.pvp"),
                Component.translatable("gui.astral_craft.board.mode.pve"),
                Component.translatable("gui.cancel"),
                () -> submit(payload, BoardMode.PVP), () -> submit(payload, BoardMode.PVE))));
    }

    private static void submit(OpenBoardModeSelectionPayload payload, BoardMode mode) {
        ClientPacketDistributor.sendToServer(new BoardModeSelectionPayload(payload.origin(), mode));
    }
}
