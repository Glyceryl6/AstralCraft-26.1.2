package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.entity.BoardWorldObjectEntity;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrap;
import com.astral_craft.common.gameplay.board.BoardMechanicsState.BoardTrapType;
import com.astral_craft.common.gameplay.SoulLinkManager;
import com.astral_craft.common.gameplay.SoulLinkStyle;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.gameplay.handcard.CardUseService;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
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
            return;
        }

        tickAwards(level, session);
        reconcile(level, session);
        reconcileSoulLinks(level, session);
    }

    public static void clear(ServerLevel level, BoardSession session) {
        if (session == null) return;
        PENDING_AWARDS.removeIf(award -> award.boardId().equals(session.id()));
        for (BoardMechanicsState.BoardSoulLink link : session.mechanics().soulLinks()) {
            BoardParticipant first = session.participant(link.firstSlotId()).orElse(null);
            BoardParticipant second = session.participant(link.secondSlotId()).orElse(null);
            AstralCharacterEntity firstEntity = first == null ? null : BoardEntityService.entity(level, first);
            AstralCharacterEntity secondEntity = second == null ? null : BoardEntityService.entity(level, second);
            if (firstEntity != null && secondEntity != null) {
                SoulLinkManager.removeVisual(level, firstEntity, secondEntity);
            }
        }

        discardStationaryVisuals(level, session.id());
    }

    public static void dropCoins(ServerLevel level, BoardSession session, String nodeId, int amount) {
        if (amount <= 0 || !session.nodes().containsKey(nodeId)) return;
        session.mechanics().addDroppedCoins(nodeId, amount);
        BoardSessionManager.markChanged(level);
        reconcile(level, session);
    }

    public static int pickupAtArrival(ServerLevel level, BoardSession session, BoardParticipant participant) {
        int amount = session.mechanics().removeDroppedCoins(participant.currentNodeKey());
        if (amount <= 0) return 0;
        spawnPickup(level, session, participant.currentNodeKey(), participant, amount);
        awardCoins(level, session, participant.slotUuid(), amount);
        BoardSessionManager.markChanged(level);
        return amount;
    }

    public static boolean hasDroppedCoinsInRange(BoardSession session, BoardParticipant collector, int range) {
        if (session == null || collector == null) return false;
        int maximum = Math.max(0, range);
        return session.mechanics().droppedCoins().entrySet().stream().anyMatch(entry -> entry.getValue() > 0
                && BoardRouteService.graphDistance(session, collector.currentNodeKey(), entry.getKey(), maximum) >= 0);
    }

    public static int collectNearbyCoins(ServerLevel level, BoardSession session, BoardParticipant collector, int range) {
        int total = 0;
        List<String> collectedNodes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : session.mechanics().droppedCoins().entrySet()) {
            int distance = BoardRouteService.graphDistance(session, collector.currentNodeKey(), entry.getKey(), range);
            if (distance < 0 || distance > range || entry.getValue() <= 0) continue;
            total += entry.getValue();
            collectedNodes.add(entry.getKey());
            spawnPickup(level, session, entry.getKey(), collector, entry.getValue());
        }

        for (String nodeId : collectedNodes) session.mechanics().removeDroppedCoins(nodeId);
        if (total > 0) {
            awardCoins(level, session, collector.slotUuid(), total);
            BoardSessionManager.markChanged(level);
        }

        return total;
    }

    public static void awardCoins(ServerLevel level, BoardSession session, UUID slotId, int amount) {
        if (amount <= 0 || session.participant(slotId).isEmpty()) return;
        int bursts = Math.min(MAX_AWARD_BURSTS, amount);
        PENDING_AWARDS.add(new PendingCoinAward(UUID.randomUUID(), session.id(), slotId, amount, bursts, level.getGameTime()));
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
            playBarricadeReveal(level, session, owner, enhancedBarricade);
        }

        if (landing || stopped) {
            for (BoardTrap trap : traps) {
                if (trap.type().barricade()) continue;
                BoardParticipant current = session.participant(participant.slotUuid()).orElse(participant);
                switch (trap.type()) {
                    case ENTRAPMENT -> triggerEntrapment(level, session, trap, current);
                    case DEMOLITION -> triggerDemolition(level, session, current);
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

    private static void triggerEntrapment(ServerLevel level, BoardSession session, BoardTrap trap, BoardParticipant target) {
        BoardParticipant owner = session.participant(trap.ownerSlotId()).orElse(null);
        if (owner == null || owner.slotUuid().equals(target.slotUuid())) return;
        int amount = Math.min(5, target.stats().starCoins());
        if (amount <= 0) return;
        BoardSessionManager.updateParticipant(level, session,
                target.withStats(target.stats().spendCoins(amount)));
        awardCoins(level, session, owner.slotUuid(), amount);
        AstralCharacterEntity targetEntity = BoardEntityService.entity(level, target);
        if (targetEntity != null) {
            level.playSound(null, targetEntity.blockPosition(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.8F, 0.9F);
        }
    }

    private static void triggerDemolition(ServerLevel level, BoardSession session, BoardParticipant target) {
        AstralCharacterEntity entity = BoardEntityService.entity(level, target);
        if (entity != null) playExplosion(level, entity.getX(), entity.getY() + 0.7D, entity.getZ());
        BoardSessionManager.damageFromEffect(level, session, target.slotUuid(), 3);
    }

    public static void playExplosion(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 12, 0.65D, 0.45D, 0.65D, 0.08D);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 28, 0.75D, 0.50D, 0.75D, 0.12D);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 24, 0.65D, 0.45D, 0.65D, 0.06D);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F,
                0.92F + level.getRandom().nextFloat() * 0.16F);
    }

    private static void playBarricadeReveal(ServerLevel level, BoardSession session, BoardParticipant ownerParticipant, boolean enhanced) {
        List<ServerPlayer> viewers = BoardSessionManager.humanPlayers(level, session);
        if (viewers.isEmpty()) return;
        ServerPlayer owner = ownerParticipant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer)
                .orElse(viewers.getFirst());
        BaseHandCard card = (BaseHandCard) (enhanced
                ? AstralItems.HANDCARD_ENHANCED_BARRICADE.get()
                : AstralItems.HANDCARD_BARRICADE.get());
        ItemStack stack = new ItemStack(card);
        for (ServerPlayer viewer : viewers) {
            CardUseService.sendReveal(viewer, stack, owner, card.definition(stack),
                    CardRevealPayload.ANIMATION_FLIP, CardUseService.CARD_REVEAL_DURATION_TICKS);
        }
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
            BoardSessionManager.updateParticipant(level, session,
                    participant.withStats(participant.stats().addCoins(chunk)));
            spawnAward(level, session, participant, chunk);
            int remaining = award.remaining() - chunk;
            int bursts = award.burstsLeft() - 1;
            if (remaining > 0 && bursts > 0) {
                replacements.add(new PendingCoinAward(award.id(), award.boardId(), award.slotId(),
                        remaining, bursts, level.getGameTime() + AWARD_INTERVAL_TICKS));
            }
        }

        PENDING_AWARDS.addAll(replacements);
    }

    private static void spawnAward(ServerLevel level, BoardSession session, BoardParticipant participant, int amount) {
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (entity == null) return;
        BoardWorldObjectEntity coin = new BoardWorldObjectEntity(level);
        coin.setPos(entity.getX(), entity.getY() + entity.getBbHeight() + 1.25D, entity.getZ());
        coin.configureAward(session.id(), UUID.randomUUID(), entity.getId(), amount, 16);
        level.addFreshEntity(coin);
        level.playSound(null, entity.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.45F, 1.5F);
    }

    private static void spawnPickup(ServerLevel level, BoardSession session, String nodeId, BoardParticipant participant, int amount) {
        BlockPos pos = session.positions().get(nodeId);
        AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
        if (pos == null || entity == null) return;
        BoardWorldObjectEntity coin = new BoardWorldObjectEntity(level);
        coin.setPos(pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D);
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
                entity.configure(session.id(), entry.getKey(), visual.kind(), visual.index(), visual.count(), visual.amount());
                entity.setPos(visual.x(), visual.y(), visual.z());
                level.addFreshEntity(entity);
            } else {
                ExpectedVisual visual = entry.getValue();
                entity.configure(session.id(), entry.getKey(), visual.kind(), visual.index(), visual.count(), visual.amount());
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
                result.put(id, new ExpectedVisual(kind(trap.type()), 0, 1, 1,
                        pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D));
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
                result.put(trap.id(), new ExpectedVisual(kind(trap.type()), index, count, 1,
                        pos.getX() + 0.5D + offset[0], pos.getY() + 1.03D,
                        pos.getZ() + 0.5D + offset[1]));
            }
        }

        for (Map.Entry<String, Integer> entry : session.mechanics().droppedCoins().entrySet()) {
            BlockPos pos = session.positions().get(entry.getKey());
            if (pos == null || entry.getValue() <= 0) continue;
            UUID id = stableId(session.id(), "coin:" + entry.getKey());
            result.put(id, new ExpectedVisual(BoardWorldObjectEntity.Kind.COIN_PILE, 0, 1, entry.getValue(),
                    pos.getX() + 0.5D, pos.getY() + 1.02D, pos.getZ() + 0.5D));
        }

        session.mechanics().timeBombSlot().flatMap(session::participant).ifPresent(participant -> {
            AstralCharacterEntity entity = BoardEntityService.entity(level, participant);
            if (entity != null) {
                UUID id = stableId(session.id(), "time_bomb");
                result.put(id, new ExpectedVisual(BoardWorldObjectEntity.Kind.TIME_BOMB, 0, 1, 1,
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.78D, entity.getZ()));
            }
        });

        return result;
    }

    private static void reconcileSoulLinks(ServerLevel level, BoardSession session) {
        for (BoardMechanicsState.BoardSoulLink link : session.mechanics().soulLinks()) {
            BoardParticipant first = session.participant(link.firstSlotId()).orElse(null);
            BoardParticipant second = session.participant(link.secondSlotId()).orElse(null);
            AstralCharacterEntity firstEntity = first == null ? null : BoardEntityService.entity(level, first);
            AstralCharacterEntity secondEntity = second == null ? null : BoardEntityService.entity(level, second);
            if (firstEntity != null && secondEntity != null) {
                SoulLinkManager.ensureVisual(level, firstEntity, secondEntity, SoulLinkStyle.rainbow(2.2F, 0.05F));
            }
        }
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

    public record ArrivalResult(BoardParticipant participant, boolean stopped, boolean triggered) {}

    private record ExpectedVisual(BoardWorldObjectEntity.Kind kind, int index, int count, int amount, double x, double y, double z) {}

    private record PendingCoinAward(UUID id, UUID boardId, UUID slotId, int remaining, int burstsLeft, long nextTick) {}

}