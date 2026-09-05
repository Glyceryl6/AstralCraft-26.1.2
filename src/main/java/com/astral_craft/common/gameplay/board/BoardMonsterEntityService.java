package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.BoardMonsterZombieEntity;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/** Board-monster presentation backed by a zombie-shaped entity with board-controlled facing. */
public class BoardMonsterEntityService {

    public static void ensureEntity(ServerLevel level, BoardSession session, BoardParticipant monster) {
        if (level == null || session == null || monster == null || !monster.monster()) return;
        if (entity(level, monster) != null) {
            sync(level, session, monster);
            return;
        }
        spawn(level, session, monster);
    }

    public static void spawn(ServerLevel level, BoardSession session, BoardParticipant monster) {
        BlockPos pos = session.positions().get(monster.currentNodeKey());
        if (pos == null || !level.hasChunkAt(pos) || entity(level, monster) != null) return;
        monster.entityUuid().map(level::getEntity).ifPresent(Entity::discard);
        BoardMonsterZombieEntity entity = AstralEntities.BOARD_MONSTER_ZOMBIE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) return;
        entity.setBoardDirection(BoardRouteService.facingDirection(session, monster));
        entity.setCustomName(Component.translatable("gui.astral_craft.board.monster"));
        entity.setCustomNameVisible(false);
        entity.setPos(pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D);
        level.addFreshEntity(entity);
        BoardParticipant spawned = monster.withEntity(entity.getUUID());
        session.putParticipant(spawned);
        BoardSessionManager.markChanged(level);
    }

    public static void sync(ServerLevel level, BoardSession session, BoardParticipant monster) {
        sync(level, monster);
        BoardMonsterZombieEntity entity = entity(level, monster);
        if (entity != null) entity.setBoardDirection(BoardRouteService.facingDirection(session, monster));
    }

    public static void sync(ServerLevel level, BoardParticipant monster) {
        BoardMonsterZombieEntity entity = entity(level, monster);
        if (entity == null) return;
        entity.setNoAi(true);
        entity.setCanPickUpLoot(false);
        entity.setInvulnerable(true);
        entity.setNoGravity(true);
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.setCustomName(Component.translatable("gui.astral_craft.board.monster"));
    }

    public static void setPosition(ServerLevel level, BoardParticipant monster, double x, double y, double z) {
        BoardMonsterZombieEntity entity = entity(level, monster);
        if (entity != null) entity.setPos(x, y, z);
    }

    public static void setDirection(ServerLevel level, BoardParticipant monster, Direction direction) {
        BoardMonsterZombieEntity entity = entity(level, monster);
        if (entity != null) entity.setBoardDirection(direction);
    }

    public static void playAttack(ServerLevel level, BoardParticipant monster) {
        BoardMonsterZombieEntity entity = entity(level, monster);
        if (entity != null) entity.swing(InteractionHand.MAIN_HAND);
    }

    public static void discard(ServerLevel level, BoardParticipant monster) {
        BoardMonsterZombieEntity entity = entity(level, monster);
        if (entity != null) entity.discard();
    }

    public static int entityId(ServerLevel level, BoardParticipant monster) {
        BoardMonsterZombieEntity entity = entity(level, monster);
        return entity == null ? -1 : entity.getId();
    }

    public static @Nullable BoardMonsterZombieEntity entity(ServerLevel level, BoardParticipant monster) {
        if (level == null || monster == null || !monster.monster()) return null;
        Entity entity = monster.entityUuid().map(level::getEntity).orElse(null);
        return entity instanceof BoardMonsterZombieEntity zombie ? zombie : null;
    }
}
