package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.BoardWorldObjectEntity;
import com.astral_craft.common.entity.StarCoinEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrap;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import com.astral_craft.common.items.cards.HandcardBarricade;
import com.astral_craft.common.items.cards.pvp.HandcardDemolition;
import com.astral_craft.common.items.cards.pvp.HandcardEntrapment;
import com.astral_craft.common.items.cards.pvp.HandcardSoulLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reconciles disposable world visuals with persistent board mechanics and processes coin animations.
 * Killing one of the visual entities does not remove its logical trap/coin: it is recreated on the next tick.
 */
public class BoardWorldObjectService {

    private static final int MAX_AWARD_BURSTS = 6;
    private static final int AWARD_INTERVAL_TICKS = 4;
    private static final List<PendingCoinAward> PENDING_AWARDS = new ArrayList<>();

    public static void tick(ServerLevel level, BoardSession session) {
        if (session.phase() != BoardPhase.PLAYING) {
            discardStationaryVisuals(level, session.id());
            discardCoinVisuals(level, session.id());
            return;
        }

        tickAwards(level, session);
        reconcile(level, session);
        reconcileCoinPiles(level, session);
        HandcardSoulLink.reconcileBoardVisuals(level, session);
    }

    public static void clear(ServerLevel level, BoardSession session) {
        if (session == null) return;
        PENDING_AWARDS.removeIf(award -> award.boardId().equals(session.id()));
        HandcardSoulLink.clearBoardVisuals(level, session);
        discardStationaryVisuals(level, session.id());
        discardCoinVisuals(level, session.id());
    }

    public static void dropCoins(ServerLevel level, BoardSession session, String nodeId, int amount) {
        if (amount <= 0 || !session.nodes().containsKey(nodeId)) return;
        session.mechanics().addDroppedCoins(nodeId, amount);
        BoardSessionManager.markChanged(level);
        reconcile(level, session);
        reconcileCoinPiles(level, session);
    }

    public static void pickupAtArrival(ServerLevel level, BoardSession session, BoardParticipant participant) {
        int amount = session.mechanics().removeDroppedCoins(participant.currentNodeKey());
        if (amount <= 0) return;
        spawnPickup(level, session, participant.currentNodeKey(), participant, amount);
        awardCoinsNow(level, session, participant.slotUuid(), amount);
        BoardSessionManager.markChanged(level);
    }

    public static void awardCoins(ServerLevel level, BoardSession session, UUID slotId, int amount) {
        scheduleCoinAward(level, session, slotId, amount, true);
    }

    public static void awardCoinsNow(ServerLevel level, BoardSession session, UUID slotId, int amount) {
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (amount <= 0 || participant == null) return;
        BoardSessionManager.updateParticipant(level, session, participant.withStats(participant.stats().addCoins(amount)));
        scheduleCoinAward(level, session, slotId, amount, false);
    }

    private static void scheduleCoinAward(ServerLevel level, BoardSession session, UUID slotId, int amount, boolean creditCoins) {
        if (amount <= 0 || session.participant(slotId).isEmpty()) return;
        int bursts = Math.min(MAX_AWARD_BURSTS, amount);
        PENDING_AWARDS.add(new PendingCoinAward(
                UUID.randomUUID(), session.id(), slotId, amount, bursts, level.getGameTime(), creditCoins));
    }

    public static ArrivalResult triggerArrival(ServerLevel level, BoardSession session, BoardParticipant participant, boolean landing) {
        List<BoardTrap> traps = session.mechanics().trapsAt(participant.currentNodeKey());
        if (traps.isEmpty()) return new ArrivalResult(participant, false, false);
        boolean stopped = false;
        boolean triggered = false;
        boolean enhancedBarricade = false;
        List<UUID> remove = new ArrayList<>();
        List<BoardTrap> barricades = traps.stream().filter(trap -> trap.type().barricade()).toList();
        if (!barricades.isEmpty()) {
            stopped = true;
            triggered = true;
            enhancedBarricade = barricades.stream().anyMatch(trap -> trap.type() == BoardTrapType.ENHANCED_BARRICADE);
            for (BoardTrap trap : barricades) remove.add(trap.id());
            BoardParticipant owner = session.participant(barricades.getFirst().ownerSlotId()).orElse(participant);
            HandcardBarricade.playTriggeredReveal(level, session, owner, enhancedBarricade);
        }

        if (landing || stopped) {
            for (BoardTrap trap : traps) {
                if (trap.type().barricade()) continue;
                BoardParticipant current = session.participant(participant.slotUuid()).orElse(participant);
                switch (trap.type()) {
                    case ENTRAPMENT -> HandcardEntrapment.trigger(level, session, trap, current);
                    case DEMOLITION -> HandcardDemolition.trigger(level, session, current);
                    default -> {}
                }
                triggered = true;
                remove.add(trap.id());
            }
        }

        for (UUID trapId : remove) session.mechanics().removeTrap(trapId);
        BoardParticipant updated = session.participant(participant.slotUuid()).orElse(participant);
        if (enhancedBarricade && !updated.knockedDown()) {
            updated = updated.withStats(updated.stats().addTemporary("speed", 2, 1));
            BoardSessionManager.updateParticipant(level, session, updated);
        }

        if (stopped && session.movement() != null) session.setMovement(session.movement().stop());
        if (triggered) {
            BoardSessionManager.markChanged(level);
            reconcile(level, session);
        }

        return new ArrivalResult(updated, stopped, triggered);
    }

    public static void placeTrap(ServerLevel level, BoardSession session, BoardTrapType type, UUID ownerSlotId, String nodeId) {
        if (!session.nodes().containsKey(nodeId)) return;
        session.mechanics().addTrap(type, ownerSlotId, nodeId);
        BoardSessionManager.markChanged(level);
        reconcile(level, session);
    }

    public static void playExplosion(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 12, 0.65D, 0.45D, 0.65D, 0.08D);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 28, 0.75D, 0.50D, 0.75D, 0.12D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 24, 0.65D, 0.45D, 0.65D, 0.06D);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.92F + level.getRandom().nextFloat() * 0.16F);
    }

    private static void tickAwards(ServerLevel level, BoardSession session) {
        List<PendingCoinAward> replacements = new ArrayList<>();
        for (int index = PENDING_AWARDS.size() - 1; index >= 0; index--) {
            PendingCoinAward award = PENDING_AWARDS.get(index);
            if (!award.boardId().equals(session.id()) || level.getGameTime() < award.nextTick()) continue;
            PENDING_AWARDS.remove(index);
            BoardParticipant participant = session.participant(award.slotId()).orElse(null);
            if (participant == null) continue;
            int chunk = Math.max(1, Mth.ceil(award.remaining() / (float) award.burstsLeft()));
            chunk = Math.min(chunk, award.remaining());
            if (award.creditCoins()) {
                BoardSessionManager.updateParticipant(level, session,
                        participant.withStats(participant.stats().addCoins(chunk)));
            }
            spawnAward(level, session, participant, chunk);
            int remaining = award.remaining() - chunk;
            int bursts = award.burstsLeft() - 1;
            if (remaining > 0 && bursts > 0) {
                replacements.add(new PendingCoinAward(award.id(), award.boardId(), award.slotId(),
                        remaining, bursts, level.getGameTime() + AWARD_INTERVAL_TICKS, award.creditCoins()));
            }
        }

        PENDING_AWARDS.addAll(replacements);
    }

    private static void spawnAward(ServerLevel level, BoardSession session, BoardParticipant participant, int amount) {
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity == null) return;
        StarCoinEntity coin = new StarCoinEntity(level);
        coin.setPos(entity.getX(), entity.getY() + entity.getBbHeight() + 1.25D, entity.getZ());
        coin.configureAward(session.id(), UUID.randomUUID(), entity.getId(), amount, 16);
        level.addFreshEntity(coin);
        level.playSound(null, entity.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.45F, 1.5F);
    }

    public static void spawnPickup(ServerLevel level, BoardSession session, String nodeId, BoardParticipant participant, int amount) {
        BlockPos pos = session.positions().get(nodeId);
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (pos == null || entity == null) return;
        StarCoinEntity coin = new StarCoinEntity(level);
        coin.setPos(pos.getX() + 0.5D, surfaceY(level, pos) + 0.22D, pos.getZ() + 0.5D);
        coin.configurePickup(session.id(), UUID.randomUUID(), entity.getId(), amount, 14);
        level.addFreshEntity(coin);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.15F);
    }

    private static void reconcile(ServerLevel level, BoardSession session) {
        AABB bounds = bounds(session).inflate(2.0D);
        Map<UUID, BoardWorldObjectEntity> existing = new HashMap<>();
        for (BoardWorldObjectEntity entity : level.getEntitiesOfClass(BoardWorldObjectEntity.class, bounds,
                entity -> !entity.transientVisual() && entity.boardId().filter(session.id()::equals).isPresent())) {
            entity.objectId().ifPresent(objectId -> existing.put(objectId, entity));
        }

        Map<UUID, ExpectedVisual> expected = expectedVisuals(level, session);
        for (Map.Entry<UUID, ExpectedVisual> entry : expected.entrySet()) {
            BoardWorldObjectEntity entity = existing.remove(entry.getKey());
            if (entity == null || entity.isRemoved()) {
                entity = new BoardWorldObjectEntity(level);
                ExpectedVisual visual = entry.getValue();
                entity.configure(session.id(), entry.getKey(), visual.kind(), visual.block(), visual.index(), visual.count(), visual.amount());
                entity.setPos(visual.x(), visual.y(), visual.z());
                level.addFreshEntity(entity);
            } else {
                ExpectedVisual visual = entry.getValue();
                entity.configure(session.id(), entry.getKey(), visual.kind(), visual.block(), visual.index(), visual.count(), visual.amount());
                entity.setPos(visual.x(), visual.y(), visual.z());
            }
        }

        existing.values().forEach(Entity::discard);
    }

    private static Map<UUID, ExpectedVisual> expectedVisuals(ServerLevel level, BoardSession session) {
        Map<UUID, ExpectedVisual> result = new LinkedHashMap<>();
        Map<String, List<BoardTrap>> stackableByNode = new LinkedHashMap<>();
        for (BoardTrap trap : session.mechanics().traps()) {
            if (trap.type().barricade()) {
                BlockPos pos = session.positions().get(trap.nodeId());
                if (pos == null) continue;
                UUID id = stableId(session.id(), "barricade:" + trap.nodeId());
                BoardWorldObjectEntity.Kind kind = kind(trap.type());
                result.put(id, new ExpectedVisual(kind, kind.defaultBlock(), 0, 1, 1,
                        pos.getX() + 0.5D, surfaceY(level, pos) + 0.28D, pos.getZ() + 0.5D));
            } else {
                stackableByNode.computeIfAbsent(trap.nodeId(), ignored -> new ArrayList<>()).add(trap);
            }
        }

        for (Map.Entry<String, List<BoardTrap>> entry : stackableByNode.entrySet()) {
            BlockPos pos = session.positions().get(entry.getKey());
            if (pos == null) continue;
            List<BoardTrap> traps = entry.getValue().stream().sorted(Comparator.comparing(BoardTrap::id)).toList();
            int count = traps.size();
            for (int index = 0; index < count; index++) {
                double[] offset = stackOffset(index, count);
                BoardTrap trap = traps.get(index);
                float halfSize = count > 4 ? 0.12F : 0.15F;
                BoardWorldObjectEntity.Kind kind = kind(trap.type());
                result.put(trap.id(), new ExpectedVisual(kind, kind.defaultBlock(), index, count, 1,
                        pos.getX() + 0.5D + offset[0], surfaceY(level, pos) + halfSize,
                        pos.getZ() + 0.5D + offset[1]));
            }
        }

        session.mechanics().timeBombSlot().flatMap(session::participant).ifPresent(participant -> {
            AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
            if (entity != null) {
                UUID id = stableId(session.id(), "time_bomb");
                BoardWorldObjectEntity.Kind kind = BoardWorldObjectEntity.Kind.TIME_BOMB;
                result.put(id, new ExpectedVisual(kind, kind.defaultBlock(), 0, 1, 1,
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.78D, entity.getZ()));
            }
        });

        return result;
    }

    private static void reconcileCoinPiles(ServerLevel level, BoardSession session) {
        AABB bounds = bounds(session).inflate(3.0D);
        Map<UUID, StarCoinEntity> existing = new HashMap<>();
        for (StarCoinEntity entity : level.getEntitiesOfClass(StarCoinEntity.class, bounds,
                entity -> !entity.transientVisual() && entity.boardId().filter(session.id()::equals).isPresent())) {
            entity.objectId().ifPresent(objectId -> existing.put(objectId, entity));
        }

        for (Map.Entry<String, Integer> entry : session.mechanics().droppedCoins().entrySet()) {
            BlockPos pos = session.positions().get(entry.getKey());
            if (pos == null || entry.getValue() <= 0) continue;
            UUID id = stableId(session.id(), "coin:" + entry.getKey());
            StarCoinEntity entity = existing.remove(id);
            double x = pos.getX() + 0.5D;
            double y = surfaceY(level, pos) + 0.28D;
            double z = pos.getZ() + 0.5D;
            if (entity == null || entity.isRemoved()) {
                entity = new StarCoinEntity(level);
                entity.setPos(x, y, z);
                entity.configurePile(session.id(), id, entry.getValue());
                entity.setDeltaMovement((level.getRandom().nextDouble() - 0.5D) * 0.06D, 0.08D,
                        (level.getRandom().nextDouble() - 0.5D) * 0.06D);
                level.addFreshEntity(entity);
            } else {
                entity.configurePile(session.id(), id, entry.getValue());
                if (entity.distanceToSqr(x, y, z) > 6.25D) entity.setPos(x, y, z);
            }
        }

        existing.values().forEach(Entity::discard);
    }

    private static BoardWorldObjectEntity.Kind kind(BoardTrapType type) {
        return switch (type) {
            case ENTRAPMENT -> BoardWorldObjectEntity.Kind.ENTRAPMENT;
            case DEMOLITION -> BoardWorldObjectEntity.Kind.DEMOLITION;
            case BARRICADE -> BoardWorldObjectEntity.Kind.BARRICADE;
            case ENHANCED_BARRICADE -> BoardWorldObjectEntity.Kind.ENHANCED_BARRICADE;
        };
    }

    private static double[] stackOffset(int index, int count) {
        if (count <= 1) return new double[]{0.0D, 0.0D};
        if (count == 2) return new double[]{index == 0 ? -0.17D : 0.17D, 0.0D};
        if (count == 3) {
            return switch (index) {
                case 0 -> new double[]{-0.17D, 0.12D};
                case 1 -> new double[]{0.17D, 0.12D};
                default -> new double[]{0.0D, -0.17D};
            };
        }

        int columns = Mth.ceil(Math.sqrt(count));
        int row = index / columns;
        int column = index % columns;
        double spacing = 0.25D;
        return new double[]{(column - (columns - 1) * 0.5D) * spacing,
                (row - (Mth.ceil(count / (float) columns) - 1) * 0.5D) * spacing};
    }

    private static double surfaceY(ServerLevel level, BlockPos pos) {
        double height = level.getBlockState(pos).getShape(level, pos).max(Direction.Axis.Y);
        if (!Double.isFinite(height) || height <= 0.0D) height = 1.0D;
        return pos.getY() + height;
    }

    private static UUID stableId(UUID boardId, String suffix) {
        return UUID.nameUUIDFromBytes((boardId + ":" + suffix).getBytes(StandardCharsets.UTF_8));
    }

    private static AABB bounds(BoardSession session) {
        BoardArea area = session.protectedArea();
        return new AABB(area.min().getX(), area.min().getY(), area.min().getZ(),
                area.max().getX() + 1.0D, area.max().getY() + 1.0D, area.max().getZ() + 1.0D);
    }

    private static void discardStationaryVisuals(ServerLevel level, UUID boardId) {
        AABB all = new AABB(-3.0E7D, level.getMinY(), -3.0E7D, 3.0E7D, level.getMaxY(), 3.0E7D);
        for (BoardWorldObjectEntity entity : level.getEntitiesOfClass(BoardWorldObjectEntity.class, all,
                entity -> !entity.transientVisual() && entity.boardId().filter(boardId::equals).isPresent())) {
            entity.discard();
        }
    }

    private static void discardCoinVisuals(ServerLevel level, UUID boardId) {
        AABB all = new AABB(-3.0E7D, level.getMinY(), -3.0E7D, 3.0E7D, level.getMaxY(), 3.0E7D);
        for (StarCoinEntity entity : level.getEntitiesOfClass(StarCoinEntity.class, all,
                entity -> entity.boardId().filter(boardId::equals).isPresent())) {
            entity.discard();
        }
    }

    public record ArrivalResult(BoardParticipant participant, boolean stopped, boolean triggered) {}

    private record ExpectedVisual(BoardWorldObjectEntity.Kind kind, Block block, int index, int count, int amount, double x, double y, double z) {}

    private record PendingCoinAward(UUID id, UUID boardId, UUID slotId, int remaining, int burstsLeft, long nextTick, boolean creditCoins) {}

}