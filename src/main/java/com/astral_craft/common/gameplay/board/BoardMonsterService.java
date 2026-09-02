package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.battle.BoardBattleService;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.stats.AstralPlayerStats;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runtime movement for board monsters. Monsters are persisted as participants but never enter the player turn order. */
public class BoardMonsterService {

    private static final Identifier DEFAULT_MONSTER_CHARACTER_ID = AstralCraft.prefix("mimi");
    private static final Map<UUID, MonsterPhase> PHASES = new HashMap<>();

    public static boolean active(UUID boardId) {
        return PHASES.containsKey(boardId);
    }

    public static @Nullable BoardParticipant spawnDefault(ServerLevel level, BoardSession session, String nodeId) {
        if (level == null || session == null || session.phase() != BoardPhase.PLAYING || !session.nodes().containsKey(nodeId)) return null;
        CharacterDefinition definition = CharacterManager.INSTANCE.contains(DEFAULT_MONSTER_CHARACTER_ID)
                ? CharacterManager.INSTANCE.get(DEFAULT_MONSTER_CHARACTER_ID)
                : CharacterManager.INSTANCE.values().stream().findFirst().orElse(null);
        if (definition == null) return null;
        String skinName = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
        BoardParticipant monster = BoardParticipant.monster(definition.id(),
                BoardParticipant.skinIdentifier(definition.id(), skinName), BoardParticipant.nodeIdentifier(nodeId),
                AstralPlayerStats.DEFAULT, List.of(), session.nextArrivalOrder());
        session.putParticipant(monster);
        BoardMonsterEntityService.spawn(level, session, monster);
        BoardSessionManager.markChanged(level);
        return session.participant(monster.slotUuid()).orElse(monster);
    }

    public static List<BoardParticipant> spawnRandom(ServerLevel level, BoardSession session, int count) {
        if (level == null || session == null || count <= 0) return List.of();
        List<String> nodes = new ArrayList<>(session.nodes().keySet());
        List<BoardParticipant> result = new ArrayList<>();
        int amount = Math.min(count, nodes.size());
        for (int index = 0; index < amount; index++) {
            int selected = level.getRandom().nextInt(nodes.size());
            String nodeId = nodes.remove(selected);
            BoardParticipant monster = spawnDefault(level, session, nodeId);
            if (monster != null) result.add(monster);
        }
        return List.copyOf(result);
    }

    public static boolean beginPhase(ServerLevel level, BoardSession session) {
        if (level == null || session == null || session.phase() != BoardPhase.PLAYING) return false;
        if (PHASES.containsKey(session.id())) return true;
        cleanupKnockedDown(level, session);
        List<UUID> monsters = session.participants().stream()
                .filter(BoardParticipant::monster).filter(participant -> !participant.knockedDown())
                .map(BoardParticipant::slotUuid).toList();
        if (monsters.isEmpty()) return false;
        PHASES.put(session.id(), new MonsterPhase(monsters));
        return true;
    }

    public static boolean tick(ServerLevel level, BoardSession session) {
        MonsterPhase phase = PHASES.get(session.id());
        if (phase == null) return false;
        if (session.phase() != BoardPhase.PLAYING) {
            PHASES.remove(session.id());
            return false;
        }
        if (phase.waitingBattle || BoardBattleService.active(session.id())) return true;
        if (phase.monsterIndex >= phase.monsterSlots.size()) {
            PHASES.remove(session.id());
            cleanupKnockedDown(level, session);
            BoardSessionManager.resumeAfterMonsterPhase(level, session);
            return true;
        }

        UUID slotId = phase.monsterSlots.get(phase.monsterIndex);
        BoardParticipant monster = session.participant(slotId).orElse(null);
        if (monster == null || !monster.monster() || monster.knockedDown()) {
            phase.nextMonster();
            return true;
        }
        var entity = BoardMonsterEntityService.entity(level, monster);
        if (entity == null) {
            BoardMonsterEntityService.spawn(level, session, monster);
            entity = BoardMonsterEntityService.entity(level, session.participant(slotId).orElse(monster));
            if (entity == null) {
                phase.nextMonster();
                return true;
            }
        }

        if (phase.remainingSteps < 0) {
            phase.remainingSteps = Mth.nextInt(level.getRandom(), 1, 10);
            phase.checkEncounter = true;
            phase.attackedSlots.clear();
        }
        if (phase.checkEncounter) {
            BoardParticipant target = encounterTarget(session, monster, phase.attackedSlots);
            if (target != null) {
                phase.attackedSlots.add(target.slotUuid());
                phase.waitingBattle = true;
                BoardBattleService.start(level, session, monster, target);
                return true;
            }
            phase.checkEncounter = false;
        }
        if (phase.remainingSteps <= 0) {
            phase.nextMonster();
            return true;
        }
        if (phase.targetNodeId == null) {
            List<String> choices = BoardRouteService.nextChoices(session, monster);
            if (choices.isEmpty()) {
                phase.remainingSteps = 0;
                return true;
            }
            phase.targetNodeId = choices.get(level.getRandom().nextInt(choices.size()));
            phase.stepStartedTick = AstralServerTickClock.now(level);

        }

        BlockPos from = session.positions().get(monster.currentNodeKey());
        BlockPos to = session.positions().get(phase.targetNodeId);
        if (from == null || to == null) {
            phase.remainingSteps = 0;
            phase.targetNodeId = null;
            return true;
        }
        long elapsed = Math.max(0L, AstralServerTickClock.now(level) - phase.stepStartedTick);
        double progress = Math.min(1.0D, elapsed / (double) BoardSessionManager.MOVEMENT_STEP_TICKS);
        entity.setPos(Mth.lerp(progress, from.getX() + 0.5D, to.getX() + 0.5D),
                Mth.lerp(progress, from.getY() + 0.12D, to.getY() + 0.12D),
                Mth.lerp(progress, from.getZ() + 0.5D, to.getZ() + 0.5D));
        if (progress < 1.0D) return true;

        String previousNode = monster.currentNodeKey();
        BoardParticipant arrived = monster.withNode(BoardParticipant.nodeIdentifier(previousNode),
                BoardParticipant.nodeIdentifier(phase.targetNodeId), session.nextArrivalOrder());
        session.putParticipant(arrived);
        phase.remainingSteps--;
        phase.targetNodeId = null;
        BoardEntityService.arrangeNode(level, session, previousNode);
        BoardEntityService.arrangeNode(level, session, arrived.currentNodeKey());
        BoardEntityService.syncState(level, arrived);
        BoardSessionManager.markChanged(level);

        phase.checkEncounter = true;
        return true;
    }

    public static boolean resumeAfterBattle(ServerLevel level, BoardSession session) {
        cleanupKnockedDown(level, session);
        MonsterPhase phase = PHASES.get(session.id());
        if (phase == null || !phase.waitingBattle) return false;
        phase.waitingBattle = false;
        phase.checkEncounter = true;
        if (phase.monsterIndex < phase.monsterSlots.size()) {
            UUID current = phase.monsterSlots.get(phase.monsterIndex);
            if (session.participant(current).isEmpty()) phase.nextMonster();
        }
        return true;
    }

    public static void clear(UUID boardId) {
        if (boardId != null) PHASES.remove(boardId);
    }

    private static @Nullable BoardParticipant encounterTarget(BoardSession session, BoardParticipant monster,
                                                               java.util.Set<UUID> attackedSlots) {
        return session.partyParticipants().stream()
                .filter(participant -> !participant.knockedDown())
                .filter(participant -> participant.currentNodeId().equals(monster.currentNodeId()))
                .filter(participant -> !attackedSlots.contains(participant.slotUuid()))
                .filter(participant -> !BoardSessionManager.isHospitalProtected(session, participant))
                .min(java.util.Comparator.comparingInt(BoardParticipant::arrivalOrder)).orElse(null);
    }

    private static void cleanupKnockedDown(ServerLevel level, BoardSession session) {
        List<BoardParticipant> defeated = session.participants().stream()
                .filter(BoardParticipant::monster).filter(BoardParticipant::knockedDown).toList();
        for (BoardParticipant monster : defeated) {
            BoardMonsterEntityService.discard(level, monster);
            String nodeId = monster.currentNodeKey();
            session.removeParticipant(monster.slotUuid());
            BoardEntityService.arrangeNode(level, session, nodeId);
        }
        if (!defeated.isEmpty()) BoardSessionManager.markChanged(level);
    }

    private static class MonsterPhase {
        private final List<UUID> monsterSlots;
        private int monsterIndex;
        private int remainingSteps = -1;
        private @Nullable String targetNodeId;
        private long stepStartedTick;
        private boolean waitingBattle;
        private boolean checkEncounter;
        private final java.util.Set<UUID> attackedSlots = new java.util.HashSet<>();

        private MonsterPhase(List<UUID> monsterSlots) {
            this.monsterSlots = List.copyOf(monsterSlots);
        }

        private void nextMonster() {
            this.monsterIndex++;
            this.remainingSteps = -1;
            this.targetNodeId = null;
            this.stepStartedTick = 0L;
            this.waitingBattle = false;
            this.checkEncounter = false;
            this.attackedSlots.clear();
        }
    }
}
