package com.astral_craft.common.gameplay.board;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

/** Protected board-area index and gravity-block restoration. */
public class BoardProtectionService {

    private static final int GRAVITY_GUARD_SCAN_INTERVAL = 4;
    private static final int GRAVITY_GUARD_HEIGHT = 24;
    private static final Map<ResourceKey<Level>, List<BoardArea>> ACTIVE_AREAS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, PendingGravityRestore>> PENDING_RESTORES = new HashMap<>();

    public static boolean toggleProtection(ServerPlayer player, BlockPos pos) {
        Optional<BoardSession> maybeSession = BoardSessionManager.findAt(player.level(), pos);
        if (maybeSession.isEmpty()) return false;
        BoardSession session = maybeSession.get();
        session.setProtectionEnabled(!session.protectionEnabled());
        BoardSessionManager.markChanged(player.level());
        refreshProtectedAreas(player.level(), BoardSavedData.get(player.level()));
        BoardSessionManager.syncBoardSnapshot(player.level(), session);
        player.sendSystemMessage(Component.translatable(session.protectionEnabled()
                ? "message.astral_craft.board.protection_enabled"
                : "message.astral_craft.board.protection_disabled"), true);
        return true;
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        return BoardSavedData.get(level).sessions().stream().anyMatch(session -> session.protects(level.dimension(), pos));
    }

    public static boolean protectFallingBlock(ServerLevel level, FallingBlockEntity fallingBlock) {
        BlockPos source = fallingBlock.blockPosition();
        boolean protectedSource = ACTIVE_AREAS.getOrDefault(level.dimension(), List.of()).stream()
                .anyMatch(area -> area.contains(source));
        if (!protectedSource) return false;
        PENDING_RESTORES.computeIfAbsent(level.dimension(), ignored -> new LinkedHashMap<>())
                .putIfAbsent(source.immutable(), new PendingGravityRestore(fallingBlock.getBlockState(),
                        level.getGameTime() + 1L));
        return true;
    }

    public static void refreshProtectedAreas(ServerLevel level, BoardSavedData savedData) {
        List<BoardArea> areas = savedData.sessions().stream().filter(BoardSession::protectionEnabled)
                .map(BoardSession::protectedArea).toList();
        if (areas.isEmpty()) ACTIVE_AREAS.remove(level.dimension());
        else ACTIVE_AREAS.put(level.dimension(), areas);
    }

    public static void tickLevel(ServerLevel level, BoardSavedData savedData) {
        refreshProtectedAreas(level, savedData);
        restoreGravityBlocks(level);
        if (level.getGameTime() % GRAVITY_GUARD_SCAN_INTERVAL == 0L) guardGravityBlocks(level, savedData);
    }

    public static void retainDimensions(Set<ResourceKey<Level>> activeDimensions) {
        ACTIVE_AREAS.keySet().retainAll(activeDimensions);
        PENDING_RESTORES.keySet().retainAll(activeDimensions);
    }

    private static void restoreGravityBlocks(ServerLevel level) {
        Map<BlockPos, PendingGravityRestore> pending = PENDING_RESTORES.get(level.dimension());
        if (pending == null || pending.isEmpty()) return;
        pending.entrySet().removeIf(entry -> {
            PendingGravityRestore restore = entry.getValue();
            if (level.getGameTime() < restore.restoreAfterTick()) return false;
            BlockPos pos = entry.getKey();
            if (isProtected(level, pos) && level.getBlockState(pos).isAir()) level.setBlock(pos, restore.state(), 3);
            return true;
        });
        if (pending.isEmpty()) PENDING_RESTORES.remove(level.dimension());
    }

    private static void guardGravityBlocks(ServerLevel level, BoardSavedData data) {
        Set<UUID> handled = new HashSet<>();
        for (BoardSession session : data.sessions()) {
            if (!session.protectionEnabled()) continue;
            BoardArea area = session.protectedArea();
            AABB bounds = new AABB(area.min().getX(), area.min().getY(), area.min().getZ(), area.max().getX() + 1.0D,
                    area.max().getY() + GRAVITY_GUARD_HEIGHT, area.max().getZ() + 1.0D);
            for (FallingBlockEntity fallingBlock : level.getEntitiesOfClass(FallingBlockEntity.class, bounds)) {
                if (!handled.add(fallingBlock.getUUID())) continue;
                BlockPos current = fallingBlock.blockPosition();
                if (insideProtectedColumn(area, current)
                        && current.getY() <= area.max().getY() + GRAVITY_GUARD_HEIGHT - 1) fallingBlock.discard();
            }
        }
    }

    private static boolean insideProtectedColumn(BoardArea area, BlockPos pos) {
        return pos.getX() >= area.min().getX() && pos.getX() <= area.max().getX()
                && pos.getZ() >= area.min().getZ() && pos.getZ() <= area.max().getZ()
                && pos.getY() >= area.min().getY();
    }

    private record PendingGravityRestore(BlockState state, long restoreAfterTick) {}

}