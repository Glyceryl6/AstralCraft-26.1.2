package com.astral_craft.common.gameplay.board;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * World-space board sessions. A session protects the physical board while logical movement
 * runs on scanned BoardNodes. Rendering a true hologram can be layered on top of this state later.
 */
public class BoardSessionManager {

    private static final List<BoardSession> SESSIONS = new CopyOnWriteArrayList<>();

    public static boolean startFromPanel(ServerPlayer player, BlockPos origin) {
        ServerLevel level = player.level();
        ScannedBoard scanned = BoardScanner.scan(level, origin);
        if (!scanned.isValid()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.invalid", String.join(", ", scanned.errors())).withStyle(ChatFormatting.RED), false);
            return false;
        }

        BoardSession session = new BoardSession(UUID.randomUUID(), level.dimension(), scanned);
        SESSIONS.add(session);
        player.sendSystemMessage(Component.translatable("message.astral_craft.board.started", scanned.nodes().size(), scanned.startNodes().size(), session.hologramCenter().getX() + ", " + session.hologramCenter().getY() + ", " + session.hologramCenter().getZ()).withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        for (BoardSession session : SESSIONS) {
            if (session.protects(level.dimension(), pos)) {
                return true;
            }
        }

        return false;
    }

    public static void end(UUID id) {
        SESSIONS.removeIf(session -> session.id().equals(id));
    }

    public static void serverTick() {
        for (BoardSession session : SESSIONS) {
            session.tick();
        }
    }

    public static List<BoardSession> sessions() {
        return new ArrayList<>(SESSIONS);
    }

}