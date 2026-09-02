package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.AstralDiceEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.gameplay.character.skill.AstralCharacterSkillEffects;
import com.astral_craft.common.registry.AstralEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Board pawn spawning, state synchronization and node arrangement. */
public class BoardEntityService {

    public static void ensureEntities(ServerLevel level, BoardSession session) {
        Set<String> occupiedNodes = new LinkedHashSet<>();
        for (BoardParticipant participant : session.participants()) {
            BlockPos pos = session.positions().get(participant.currentNodeKey());
            if (pos == null || !level.hasChunkAt(pos)) continue;
            if (entity(level, participant) == null) spawnCharacter(level, session, participant);
            occupiedNodes.add(participant.currentNodeKey());
        }
        if (session.movement() == null) occupiedNodes.forEach(nodeId -> arrangeNode(level, session, nodeId));
    }

    public static void spawnCharacter(ServerLevel level, BoardSession session, BoardParticipant participant) {
        BlockPos pos = session.positions().get(participant.currentNodeKey());
        if (pos == null || !level.hasChunkAt(pos) || entity(level, participant) != null) return;
        AstralCharacterEntity entity = AstralEntities.ASTRAL_CHARACTER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) return;
        entity.setCharacterId(participant.characterId());
        entity.setSkinId(participant.skinName());
        participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer).ifPresent(controller -> {
            var progress = CharacterProgressManager.progress(controller).entry(participant.characterId());
            entity.setCharacterLevel(progress.level());
            entity.setFriendship(progress.friendship());
            entity.setPotentialActive(progress.potentialActivated());
        });
        entity.setStarCoins(participant.stats().starCoins());
        entity.setBoardSessionId(session.id());
        entity.setBoardParticipantId(participant.slotUuid());
        entity.setCustomName(participant.monster()
                ? Component.translatable("gui.astral_craft.board.monster")
                : Component.translatable(CharacterManager.INSTANCE.get(participant.characterId()).getDescriptionId()));
        entity.setCustomNameVisible(false);
        AttributeInstance instance = entity.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) instance.setBaseValue(participant.stats().maxHealth());
        entity.setHealth(Math.max(1.0F, participant.stats().health()));
        entity.setPos(pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D);
        entity.setBoardDirection(BoardRouteService.facingDirection(session, participant));
        entity.setPersistenceRequired();
        level.addFreshEntity(entity);
        BoardParticipant spawned = participant.withEntity(entity.getUUID());
        session.putParticipant(spawned);
        syncState(level, spawned);
        arrangeNode(level, session, participant.currentNodeKey());
        BoardSessionManager.markChanged(level);
    }

    public static void syncState(ServerLevel level, BoardParticipant participant) {
        AstralCharacterEntity entity = entity(level, participant);
        if (entity == null) return;
        entity.setStarCoins(participant.stats().starCoins());
        AttributeInstance instance = entity.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) instance.setBaseValue(participant.stats().maxHealth());
        entity.setHealth(Math.max(participant.stats().health() <= 0 ? 1.0F : participant.stats().health(), 1.0F));
        if (participant.knockedDownTurns() > 0 || participant.stats().health() <= 0) {
            entity.setAnimationAction("knockdown");
        } else if ("knockdown".equals(entity.animationAction())) {
            entity.setAnimationAction("idle");
        }
        AstralCharacterSkillEffects.synchronizeRoundEffects(entity, participant.roundStatusEffects());
        entity.setBoardDirectionMask(directionMask(level, entity, participant));
        BoardSession session = entity.boardSessionUuid().flatMap(id -> BoardSessionManager.session(level, id)).orElse(null);
        if (session != null && !"walk".equals(entity.animationAction())) {
            entity.setBoardDirection(BoardRouteService.facingDirection(session, participant));
        }
    }

    public static void arrangeNode(ServerLevel level, BoardSession session, String nodeId) {
        BlockPos pos = session.positions().get(nodeId);
        if (pos == null) return;
        List<BoardParticipant> occupants = session.participants().stream()
                .filter(participant -> participant.currentNodeKey().equals(nodeId))
                .sorted(Comparator.comparingInt(BoardParticipant::arrivalOrder)).toList();
        BoardSession.MovementState movement = session.movement();
        for (int index = 0; index < occupants.size(); index++) {
            BoardParticipant participant = occupants.get(index);
            AstralCharacterEntity entity = entity(level, participant);
            if (entity == null || movement != null && movement.slotId().equals(participant.slotUuid())
                    && movement.stepping()) continue;
            double angle = occupants.size() == 1 ? 0.0D : Math.PI * 2.0D * index / occupants.size();
            double radius = occupants.size() == 1 ? 0.0D : Math.min(0.34D, 0.12D + occupants.size() * 0.035D);
            entity.setPos(pos.getX() + 0.5D + Math.cos(angle) * radius, pos.getY() + 0.12D,
                    pos.getZ() + 0.5D + Math.sin(angle) * radius);
        }
    }

    public static void clearBoardDice(ServerLevel level, BoardSession session) {
        BoardArea area = session.protectedArea().inflate(8, 12);
        AABB bounds = new AABB(area.min().getX(), area.min().getY(), area.min().getZ(),
                area.max().getX() + 1.0D, area.max().getY() + 1.0D, area.max().getZ() + 1.0D);
        for (AstralDiceEntity dice : level.getEntitiesOfClass(AstralDiceEntity.class, bounds,
                candidate -> candidate.boardSessionId().filter(session.id()::equals).isPresent())) {
            dice.discard();
        }
    }

    public static void clearRuntimeEntities(ServerLevel level, BoardSession session) {
        for (BoardParticipant participant : session.participants()) {
            AstralCharacterEntity entity = entity(level, participant);
            if (entity != null) entity.discard();
        }
    }

    public static int entityId(ServerLevel level, BoardParticipant participant) {
        AstralCharacterEntity entity = entity(level, participant);
        return entity == null ? -1 : entity.getId();
    }

    public static int revealSourceEntityId(ServerPlayer player) {
        BoardSession session = BoardSessionManager.findByController(player).orElse(null);
        if (session == null) return player.getId();
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        int entityId = participant == null ? -1 : entityId(player.level(), participant);
        return entityId < 0 ? player.getId() : entityId;
    }

    public static LivingEntity effectSourceEntity(ServerPlayer player) {
        BoardSession session = BoardSessionManager.findByController(player).orElse(null);
        if (session == null) return player;
        BoardParticipant participant = session.participantByController(player.getUUID()).orElse(null);
        AstralCharacterEntity pawn = participant == null ? null : entity(player.level(), participant);
        return pawn == null ? player : pawn;
    }

    public static @Nullable AstralCharacterEntity entity(ServerLevel level, BoardParticipant participant) {
        return participant.entityUuid().map(level::getEntity).filter(AstralCharacterEntity.class::isInstance)
                .map(AstralCharacterEntity.class::cast).orElse(null);
    }

    private static int directionMask(ServerLevel level, AstralCharacterEntity entity, BoardParticipant participant) {
        BoardSession session = entity.boardSessionUuid().flatMap(id -> BoardSessionManager.session(level, id)).orElse(null);
        BlockPos current = session == null ? null : session.positions().get(participant.currentNodeKey());
        if (session == null || current == null) return 0;
        List<String> choices = BoardRouteService.nextChoices(session, participant);
        if (choices.size() <= 1) return 0;
        int mask = 0;
        for (String nextNode : choices) {
            BlockPos target = session.positions().get(nextNode);
            if (target != null) mask |= 1 << directionBetween(current, target).get2DDataValue();
        }
        return mask;
    }

    public static Direction directionBetween(BlockPos from, BlockPos to) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dz = Integer.compare(to.getZ(), from.getZ());
        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (dz != 0) return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        return Direction.NORTH;
    }

}