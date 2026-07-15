package com.astral_craft.common.gameplay.battle;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.OpenBoardBattlePayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/** Server-authoritative board battle selection, dice presentation and result coordinator. */
public class BoardBattleService {

    public static final int DECISION_TICKS = 20 * 20;
    public static final int DEFENSE_CHOICE_TICKS = 20 * 8;
    public static final int ATTACKER_ROLL_TICKS = 36;
    public static final int DEFENDER_ROLL_TICKS = 76;
    public static final int RESULT_TICKS = 20 * 3;
    public static final int MAXIMUM_PVP_COST = 3;
    private static final int MAXIMUM_SELECTED_CARDS = 7;
    private static final String PHASE_SELECT = "select";
    private static final String PHASE_ATTACKER_ROLL = "attacker_roll";
    private static final String PHASE_DEFENSE_CHOICE = "defense_choice";
    private static final String PHASE_DEFENDER_ROLL = "defender_roll";
    private static final String PHASE_RESULT = "result";
    private static final Map<UUID, BattleState> ACTIVE = new HashMap<>();

    public static void start(ServerLevel level, BoardSession session,
                             BoardParticipant attacker, BoardParticipant defender) {
        if (ACTIVE.containsKey(session.id())) return;
        List<Integer> attackerCards = attacker.bot() ? chooseBotCards(attacker, CardType.ATTACK) : List.of();
        List<Integer> defenderCards = defender.bot() ? chooseBotCards(defender, CardType.DEFENSE) : List.of();
        BattleState state = new BattleState(session.id(), attacker.slotUuid(), defender.slotUuid(),
                PHASE_SELECT, level.getGameTime() + DECISION_TICKS,
                attackerCards, defenderCards, "defend", attacker.bot(), defender.bot(), null);
        ACTIVE.put(session.id(), state);
        if (state.cardsReady()) beginAttackerRoll(level, session, state);
        else send(level, session, state);
    }

    public static boolean active(UUID boardId) {
        return ACTIVE.containsKey(boardId);
    }

    public static void submit(ServerPlayer player, String rawBoardId,
                              List<Integer> selectedCardIndexes, String defenseMode) {
        UUID boardId;
        try {
            boardId = UUID.fromString(rawBoardId);
        } catch (IllegalArgumentException exception) {
            return;
        }
        BattleState state = ACTIVE.get(boardId);
        BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
        if (state == null || session == null) return;
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (attacker == null || defender == null) return;

        if (PHASE_SELECT.equals(state.phase())) {
            if (attacker.controlledBy(player.getUUID())) {
                if (state.attackerReady()) return;
                List<Integer> indexes = validateSelection(attacker, selectedCardIndexes, CardType.ATTACK);
                if (indexes == null) return;
                state = state.withAttacker(indexes, true);
            } else if (defender.controlledBy(player.getUUID())) {
                if (state.defenderReady()) return;
                List<Integer> indexes = validateSelection(defender, selectedCardIndexes, CardType.DEFENSE);
                if (indexes == null) return;
                state = state.withDefenderCards(indexes, true);
            } else {
                return;
            }
            ACTIVE.put(boardId, state);
            if (state.cardsReady()) beginAttackerRoll(player.level(), session, state);
            else send(player.level(), session, state);
            return;
        }

        if (PHASE_DEFENSE_CHOICE.equals(state.phase()) && defender.controlledBy(player.getUUID())) {
            beginDefenderRoll(player.level(), session,
                    state.withDefenseMode("evade".equals(defenseMode) ? "evade" : "defend"));
        }
    }

    public static void serverTick(MinecraftServer server) {
        for (BattleState state : new ArrayList<>(ACTIVE.values())) {
            var dimension = BoardSessionManager.dimension(state.boardId()).orElse(null);
            ServerLevel level = dimension == null ? null : server.getLevel(dimension);
            if (level == null) {
                ACTIVE.remove(state.boardId());
                continue;
            }
            BoardSession session = BoardSessionManager.session(level, state.boardId()).orElse(null);
            if (session == null) {
                ACTIVE.remove(state.boardId());
                continue;
            }
            if (level.getGameTime() < state.deadlineTick()) continue;

            switch (state.phase()) {
                case PHASE_SELECT -> beginAttackerRoll(level, session, state.withTimedOutSelections());
                case PHASE_ATTACKER_ROLL -> beginDefenseChoice(level, session, state);
                case PHASE_DEFENSE_CHOICE -> beginDefenderRoll(level, session, state.withDefenseMode("defend"));
                case PHASE_DEFENDER_ROLL -> applyRoll(level, session, state);
                default -> {
                    ACTIVE.remove(state.boardId());
                    BoardSessionManager.resumeAfterBattle(level, session);
                }
            }
        }
    }


    public static void participantBecameBot(ServerLevel level, BoardSession session, UUID slotId) {
        BattleState state = ACTIVE.get(session.id());
        if (state == null) return;
        BoardParticipant participant = session.participant(slotId).orElse(null);
        if (participant == null) return;
        if (PHASE_SELECT.equals(state.phase())) {
            if (state.attackerSlot().equals(slotId) && !state.attackerReady()) {
                state = state.withAttacker(chooseBotCards(participant, CardType.ATTACK), true);
            }
            if (state.defenderSlot().equals(slotId) && !state.defenderReady()) {
                state = state.withDefenderCards(chooseBotCards(participant, CardType.DEFENSE), true);
            }
            ACTIVE.put(session.id(), state);
            if (state.cardsReady()) beginAttackerRoll(level, session, state);
            else send(level, session, state);
            return;
        }
        if (PHASE_DEFENSE_CHOICE.equals(state.phase()) && state.defenderSlot().equals(slotId)) {
            beginDefenderRoll(level, session, state.withDefenseMode("defend"));
        }
    }

    public static void cancel(UUID boardId) {
        ACTIVE.remove(boardId);
    }

    private static void beginAttackerRoll(ServerLevel level, BoardSession session, BattleState state) {
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (attacker == null || defender == null) {
            cancelAndResume(level, session);
            return;
        }
        List<Integer> attackerCards = validatedOrEmpty(attacker, state.attackerCards(), CardType.ATTACK);
        List<Integer> defenderCards = validatedOrEmpty(defender, state.defenderCards(), CardType.DEFENSE);
        CardRange attackRange = cardRange(attacker, attackerCards, CardType.ATTACK);
        CardRange defenseRange = cardRange(defender, defenderCards, CardType.DEFENSE);
        int attackerDie = Mth.nextInt(level.getRandom(), 1, 6);
        int attackBonus = randomCardBonus(level, attacker, attackerCards, CardType.ATTACK);
        BattleRoll roll = new BattleRoll(attackerCards, defenderCards,
                attacker.stats().attack(), defender.stats().defense(),
                attackRange.minimum(), attackRange.maximum(), defenseRange.minimum(), defenseRange.maximum(),
                attackerDie, 0, attackBonus, 0,
                attacker.stats().attack() + attackerDie + attackBonus, 0,
                0, false, false);
        BattleState rolling = state.withRoll(PHASE_ATTACKER_ROLL,
                level.getGameTime() + ATTACKER_ROLL_TICKS, roll);
        ACTIVE.put(session.id(), rolling);
        send(level, session, rolling);
    }

    private static void beginDefenseChoice(ServerLevel level, BoardSession session, BattleState state) {
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (defender == null) {
            cancelAndResume(level, session);
            return;
        }
        if (defender.bot() || defender.controllerUuid()
                .map(level.getServer().getPlayerList()::getPlayer).orElse(null) == null) {
            beginDefenderRoll(level, session, state.withDefenseMode("defend"));
            return;
        }
        BattleState choosing = state.withPhase(PHASE_DEFENSE_CHOICE,
                level.getGameTime() + DEFENSE_CHOICE_TICKS);
        ACTIVE.put(session.id(), choosing);
        send(level, session, choosing);
    }

    private static void beginDefenderRoll(ServerLevel level, BoardSession session, BattleState state) {
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        BattleRoll preliminary = state.roll();
        if (attacker == null || defender == null || preliminary == null) {
            cancelAndResume(level, session);
            return;
        }
        int defenderDie = Mth.nextInt(level.getRandom(), 1, 6);
        int defenseBonus = randomCardBonus(level, defender, preliminary.defenderCards(), CardType.DEFENSE);
        int attackTotal = preliminary.attackBase() + preliminary.attackerDie() + preliminary.attackBonus();
        int defenseTotal = preliminary.defenseBase() + defenderDie + defenseBonus;
        boolean evaded = "evade".equals(state.defenseMode())
                && (defenderDie > preliminary.attackerDie() || defenderDie == 6);
        int damage = "evade".equals(state.defenseMode())
                ? (evaded ? 0 : Math.max(0, attackTotal))
                : Math.max(1, attackTotal - defenseTotal);
        if (damage > 0) damage = Math.max(0, damage + defender.stats().incomingDamageBonus());
        int remainingHealth = Math.max(0, defender.stats().health() - damage);
        BattleRoll roll = new BattleRoll(preliminary.attackerCards(), preliminary.defenderCards(),
                preliminary.attackBase(), preliminary.defenseBase(),
                preliminary.attackCardMinimum(), preliminary.attackCardMaximum(),
                preliminary.defenseCardMinimum(), preliminary.defenseCardMaximum(),
                preliminary.attackerDie(), defenderDie, preliminary.attackBonus(), defenseBonus,
                attackTotal, defenseTotal, damage, evaded, remainingHealth == 0);
        BattleState rolling = state.withRoll(PHASE_DEFENDER_ROLL,
                level.getGameTime() + DEFENDER_ROLL_TICKS, roll);
        ACTIVE.put(session.id(), rolling);
        send(level, session, rolling);
    }

    private static void applyRoll(ServerLevel level, BoardSession session, BattleState state) {
        BattleRoll roll = state.roll();
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (roll == null || attacker == null || defender == null) {
            cancelAndResume(level, session);
            return;
        }
        AstralPlayerStats nextStats = defender.stats().damage(roll.damage());
        BoardParticipant nextAttacker = removeCards(attacker, roll.attackerCards());
        BoardParticipant nextDefender = removeCards(defender.withStats(nextStats), roll.defenderCards());
        if (nextStats.health() <= 0) {
            int lostCoins = Math.max(0, (nextDefender.stats().starCoins() + 1) / 2);
            nextDefender = nextDefender.knockDown();
            nextAttacker = nextAttacker.withStats(nextAttacker.stats().addCoins(lostCoins));
        }
        playAttackAnimation(level, nextAttacker);
        BoardSessionManager.updateParticipant(level, session, nextAttacker);
        BoardSessionManager.updateParticipant(level, session, nextDefender);
        BattleState result = state.withPhase(PHASE_RESULT, level.getGameTime() + RESULT_TICKS);
        ACTIVE.put(session.id(), result);
        send(level, session, result);
    }

    private static void cancelAndResume(ServerLevel level, BoardSession session) {
        ACTIVE.remove(session.id());
        BoardSessionManager.resumeAfterBattle(level, session);
    }

    private static void playAttackAnimation(ServerLevel level, BoardParticipant participant) {
        Entity entity = participant.entityUuid().map(level::getEntity).orElse(null);
        if (entity instanceof AstralCharacterEntity character) character.playBoardAttackAnimation(16);
    }

    private static BoardParticipant removeCards(BoardParticipant participant, List<Integer> indexes) {
        BoardParticipant result = participant;
        List<Integer> descending = new ArrayList<>(indexes);
        descending.sort(Comparator.reverseOrder());
        for (int index : descending) result = result.removeCard(index);
        return result;
    }

    private static CardRange cardRange(BoardParticipant participant, List<Integer> indexes, CardType expected) {
        int minimum = 0;
        int maximum = 0;
        for (int index : validatedOrEmpty(participant, indexes, expected)) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition == null) continue;
            minimum += 1;
            maximum += maximumBonus(definition.combatCost());
        }
        return new CardRange(minimum, maximum);
    }

    private static int randomCardBonus(ServerLevel level, BoardParticipant participant,
                                       List<Integer> indexes, CardType expected) {
        int bonus = 0;
        for (int index : validatedOrEmpty(participant, indexes, expected)) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition != null) bonus += Mth.nextInt(level.getRandom(), 1,
                    maximumBonus(definition.combatCost()));
        }
        return bonus;
    }

    private static int maximumBonus(int cost) {
        return switch (cost) {
            case 3 -> 10;
            case 2 -> 6;
            default -> 3;
        };
    }

    private static List<Integer> validateSelection(BoardParticipant participant, List<Integer> indexes,
                                                   CardType expected) {
        if (indexes == null || indexes.size() > MAXIMUM_SELECTED_CARDS) return null;
        Set<Integer> unique = new HashSet<>(indexes);
        if (unique.size() != indexes.size()) return null;
        int cost = 0;
        List<Integer> result = new ArrayList<>(indexes.size());
        for (int index : indexes) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition == null) return null;
            cost += definition.combatCost();
            if (cost > MAXIMUM_PVP_COST) return null;
            result.add(index);
        }
        result.sort(Integer::compareTo);
        return List.copyOf(result);
    }

    private static List<Integer> validatedOrEmpty(BoardParticipant participant, List<Integer> indexes,
                                                  CardType expected) {
        List<Integer> valid = validateSelection(participant, indexes, expected);
        return valid == null ? List.of() : valid;
    }

    private static CardDefinition combatDefinition(BoardParticipant participant, int index, CardType expected) {
        if (index < 0 || index >= participant.hand().size()) return null;
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
        if (!(item instanceof BaseHandCard) || !isStandardCombatItem(item, expected)) return null;
        ItemStack stack = new ItemStack(item);
        CardDefinition definition = stack.get(AstralDataComponents.CARD_DEFINITION);
        if (definition == null || definition.type() != expected
                || definition.combatCost() <= 0 || definition.combatCost() > MAXIMUM_PVP_COST) return null;
        return definition;
    }

    private static boolean isStandardCombatItem(Item item, CardType expected) {
        if (expected == CardType.ATTACK) {
            return item == AstralItems.HANDCARD_ATTACK_M.get()
                    || item == AstralItems.HANDCARD_ATTACK_L.get()
                    || item == AstralItems.HANDCARD_ATTACK_G.get();
        }
        return item == AstralItems.HANDCARD_DEFENSE_M.get()
                || item == AstralItems.HANDCARD_DEFENSE_L.get()
                || item == AstralItems.HANDCARD_DEFENSE_G.get();
    }

    private static List<Integer> chooseBotCards(BoardParticipant participant, CardType expected) {
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            if (combatDefinition(participant, index, expected) != null) candidates.add(index);
        }
        List<Integer> best = new ArrayList<>();
        chooseBotCards(participant, expected, candidates, 0, 0, new ArrayList<>(), best);
        best.sort(Integer::compareTo);
        return List.copyOf(best);
    }

    private static void chooseBotCards(BoardParticipant participant, CardType expected, List<Integer> candidates,
                                       int cursor, int spent, List<Integer> chosen, List<Integer> best) {
        if (spent > MAXIMUM_PVP_COST) return;
        if (chosen.size() > best.size() || chosen.size() == best.size()
                && selectionCost(participant, expected, chosen) > selectionCost(participant, expected, best)) {
            best.clear();
            best.addAll(chosen);
        }
        if (cursor >= candidates.size() || chosen.size() >= MAXIMUM_SELECTED_CARDS) return;
        for (int index = cursor; index < candidates.size(); index++) {
            CardDefinition definition = combatDefinition(participant, candidates.get(index), expected);
            if (definition == null || spent + definition.combatCost() > MAXIMUM_PVP_COST) continue;
            chosen.add(candidates.get(index));
            chooseBotCards(participant, expected, candidates, index + 1,
                    spent + definition.combatCost(), chosen, best);
            chosen.removeLast();
        }
    }

    private static int selectionCost(BoardParticipant participant, CardType expected, List<Integer> indexes) {
        int result = 0;
        for (int index : indexes) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition != null) result += definition.combatCost();
        }
        return result;
    }

    private static void send(ServerLevel level, BoardSession session, BattleState state) {
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (attacker == null || defender == null) return;
        int attackerEntity = BoardSessionManager.entityId(level, attacker);
        int defenderEntity = BoardSessionManager.entityId(level, defender);
        for (ServerPlayer viewer : BoardSessionManager.humanPlayers(level, session)) {
            String role = attacker.controlledBy(viewer.getUUID()) ? "attacker"
                    : defender.controlledBy(viewer.getUUID()) ? "defender" : "spectator";
            BoardParticipant own = "attacker".equals(role) ? attacker
                    : "defender".equals(role) ? defender : null;
            String cards = own == null || !PHASE_SELECT.equals(state.phase()) ? ""
                    : encodeCombatCards(own, "attacker".equals(role) ? CardType.ATTACK : CardType.DEFENSE);
            int remaining = (int) Math.max(0L, state.deadlineTick() - level.getGameTime());
            PacketDistributor.sendToPlayer(viewer, new OpenBoardBattlePayload(session.id().toString(),
                    attackerEntity, defenderEntity, BoardSessionManager.displayName(level, attacker),
                    BoardSessionManager.displayName(level, defender), cards, role, remaining,
                    MAXIMUM_PVP_COST, !PHASE_SELECT.equals(state.phase()), encodeView(state, attacker, defender)));
        }
    }

    private static String encodeView(BattleState state, BoardParticipant attacker, BoardParticipant defender) {
        BattleRoll roll = state.roll();
        int attackBase = attacker.stats().attack();
        int defenseBase = defender.stats().defense();
        String s = Integer.toString(attacker.stats().health());
        String s1 = Integer.toString(defender.stats().health());
        if (roll == null) {
            return String.join("|", state.phase(), s, s1,
                    Integer.toString(attackBase), Integer.toString(defenseBase),
                    Integer.toString(attackBase), Integer.toString(attackBase),
                    Integer.toString(defenseBase), Integer.toString(defenseBase),
                    "0", "0", "0", "0", "0", "0", "0", "false", "false",
                    Boolean.toString(state.attackerReady()), Boolean.toString(state.defenderReady()),
                    state.defenseMode());
        }
        return String.join("|", state.phase(), s, s1,
                Integer.toString(roll.attackBase()), Integer.toString(roll.defenseBase()),
                Integer.toString(roll.attackBase() + roll.attackCardMinimum()),
                Integer.toString(roll.attackBase() + roll.attackCardMaximum()),
                Integer.toString(roll.defenseBase() + roll.defenseCardMinimum()),
                Integer.toString(roll.defenseBase() + roll.defenseCardMaximum()),
                Integer.toString(roll.attackerDie()), Integer.toString(roll.defenderDie()),
                Integer.toString(roll.attackBonus()), Integer.toString(roll.defenseBonus()),
                Integer.toString(roll.attackTotal()), Integer.toString(roll.defenseTotal()),
                Integer.toString(roll.damage()), Boolean.toString(roll.evaded()), Boolean.toString(roll.knockout()),
                Boolean.toString(state.attackerReady()), Boolean.toString(state.defenderReady()),
                state.defenseMode());
    }

    private static String encodeCombatCards(BoardParticipant participant, CardType expected) {
        StringJoiner output = new StringJoiner(";");
        for (int index = 0; index < participant.hand().size(); index++) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition != null) {
                output.add(index + "," + participant.hand().get(index) + "," + definition.combatCost());
            }
        }
        return output.toString();
    }

    private record CardRange(int minimum, int maximum) {}

    private record BattleRoll(List<Integer> attackerCards, List<Integer> defenderCards,
                              int attackBase, int defenseBase,
                              int attackCardMinimum, int attackCardMaximum,
                              int defenseCardMinimum, int defenseCardMaximum,
                              int attackerDie, int defenderDie, int attackBonus, int defenseBonus,
                              int attackTotal, int defenseTotal, int damage,
                              boolean evaded, boolean knockout) {
        private BattleRoll {
            attackerCards = List.copyOf(attackerCards);
            defenderCards = List.copyOf(defenderCards);
        }
    }

    private record BattleState(UUID boardId, UUID attackerSlot, UUID defenderSlot,
                               String phase, long deadlineTick,
                               List<Integer> attackerCards, List<Integer> defenderCards,
                               String defenseMode, boolean attackerReady, boolean defenderReady,
                               BattleRoll roll) {
        private BattleState {
            attackerCards = List.copyOf(attackerCards);
            defenderCards = List.copyOf(defenderCards);
            phase = phase == null || phase.isBlank() ? PHASE_SELECT : phase;
            defenseMode = "evade".equals(defenseMode) ? "evade" : "defend";
        }

        private boolean cardsReady() {
            return this.attackerReady && this.defenderReady;
        }

        private BattleState withAttacker(List<Integer> cards, boolean ready) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    this.phase, this.deadlineTick, cards, this.defenderCards,
                    this.defenseMode, ready, this.defenderReady, this.roll);
        }

        private BattleState withDefenderCards(List<Integer> cards, boolean ready) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    this.phase, this.deadlineTick, this.attackerCards, cards,
                    this.defenseMode, this.attackerReady, ready, this.roll);
        }

        private BattleState withDefenseMode(String mode) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    this.phase, this.deadlineTick, this.attackerCards, this.defenderCards,
                    mode, this.attackerReady, this.defenderReady, this.roll);
        }

        private BattleState withPhase(String phase, long deadlineTick) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    phase, deadlineTick, this.attackerCards, this.defenderCards,
                    this.defenseMode, this.attackerReady, this.defenderReady, this.roll);
        }

        private BattleState withRoll(String phase, long deadlineTick, BattleRoll roll) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    phase, deadlineTick, roll.attackerCards(), roll.defenderCards(),
                    this.defenseMode, true, true, roll);
        }

        private BattleState withTimedOutSelections() {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    this.phase, this.deadlineTick,
                    this.attackerReady ? this.attackerCards : List.of(),
                    this.defenderReady ? this.defenderCards : List.of(),
                    this.defenseMode, true, true, this.roll);
        }
    }

}