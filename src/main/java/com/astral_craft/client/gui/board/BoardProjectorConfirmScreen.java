package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.components.AstralConfirmationScreen;
import com.astral_craft.common.gameplay.board.BoardMode;
import com.astral_craft.common.network.c2s.BoardProjectorConfirmPayload;
import com.astral_craft.common.network.s2c.OpenBoardProjectorConfirmPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class BoardProjectorConfirmScreen {

    public static void open(OpenBoardProjectorConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            List<Component> lines = List.of(Component.translatable("gui.astral_craft.board_projector.confirm.warning"),
                    Component.translatable("gui.astral_craft.board_projector.confirm.details",
                            payload.panelCount(), payload.width(), payload.depth()));
            if (payload.mode().decided()) {
                Minecraft.getInstance().setScreen(new AstralConfirmationScreen(
                        Component.translatable("gui.astral_craft.board_projector.confirm.title"), lines,
                        Component.translatable("gui.astral_craft.board_projector.confirm.create"),
                        Component.translatable("gui.astral_craft.board_projector.confirm.cancel"),
                        () -> submit(payload, payload.mode())));
                return;
            }
            Minecraft.getInstance().setScreen(new AstralConfirmationScreen(
                    Component.translatable("gui.astral_craft.board.mode.title"), lines,
                    Component.translatable("gui.astral_craft.board.mode.pvp"),
                    Component.translatable("gui.astral_craft.board.mode.pve"),
                    Component.translatable("gui.astral_craft.board_projector.confirm.cancel"),
                    () -> submit(payload, BoardMode.PVP), () -> submit(payload, BoardMode.PVE)));
        });
    }

    private static void submit(OpenBoardProjectorConfirmPayload payload, BoardMode mode) {
        ClientPacketDistributor.sendToServer(new BoardProjectorConfirmPayload(
                payload.groundPos(), payload.facing(), payload.offhand(), mode));
    }
}
