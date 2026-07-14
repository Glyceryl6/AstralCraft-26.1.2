package com.astral_craft.common.gameplay.battle;

import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.OpenBoardBattlePayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Turn-scoped combat coordinator for board pawns. Combat choices are authoritative on the server:
 * PVP grants three cost points, allows several standard combat cards, and consumes the selected
 * cards only after both sides are ready.
 */
public class BoardBattleService {

    public static final int DECISION_TICKS = 20 * 20;
    public static final int RESULT_TICKS = 20 * 3;
    public static final int MAXIMUM_PVP_COST = 3;
    private static final int MAXIMUM_SELECTED_CARDS = 7;
    private static final Map<UUID, BattleState> ACTIVE = new HashMap<>();

    public static void start(ServerLevel level, BoardSession session, BoardParticipant attacker, BoardParticipant defender) {
        if (ACTIVE.containsKey(session.id())) return;
        List<Integer> attackerCards = attacker.bot() ? chooseBotCards(level, attacker, CardType.ATTACK) : List.of();
        List<Integer> defenderCards = defender.bot() ? chooseBotCards(level, defender, CardType.DEFENSE) : List.of();
        BattleState state = new BattleState(session.id(), attacker.slotUuid(), defender.slotUuid(),
                level.getGameTime() + DECISION_TICKS, attackerCards, defenderCards, "defend",
                attacker.bot(), defender.bot(), 0L);
        ACTIVE.put(session.id(), state);
        if (ready(session, state)) {
            resolve(level, session, state);
        } else {
            send(level, session, state, false, "");
        }
    }

    public static boolean active(UUID boardId) {
        return ACTIVE.containsKey(boardId);
    }

    public static void submit(ServerPlayer player, String rawBoardId, List<Integer> selectedCardIndexes,
                              String defenseMode) {
        UUID boardId;
        try {
            boardId = UUID.fromString(rawBoardId);
        } catch (IllegalArgumentException exception) {
            return;
        }
        BattleState state = ACTIVE.get(boardId);
        BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
        if (state == null || session == null || state.resultUntilTick() > 0L) return;
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (attacker == null || defender == null) return;
        if (attacker.controlledBy(player.getUUID())) {
            if (state.attackerReady()) return;
            List<Integer> indexes = validateSelection(attacker, selectedCardIndexes, CardType.ATTACK);
            if (indexes == null) return;
            state = state.withAttacker(indexes, true);
        } else if (defender.controlledBy(player.getUUID())) {
            if (state.defenderReady()) return;
            List<Integer> indexes = validateSelection(defender, selectedCardIndexes, CardType.DEFENSE);
            if (indexes == null) return;
            String mode = "evade".equals(defenseMode) ? "evade" : "defend";
            state = state.withDefender(indexes, mode, true);
        } else {
            return;
        }
        ACTIVE.put(boardId, state);
        if (ready(session, state)) {
            resolve(player.level(), session, state);
        } else {
            send(player.level(), session, state, false, "");
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
            if (state.resultUntilTick() > 0L) {
                if (level.getGameTime() >= state.resultUntilTick()) {
                    ACTIVE.remove(state.boardId());
                    BoardSessionManager.resumeAfterBattle(level, session);
                }
                continue;
            }
            if (level.getGameTime() >= state.deadlineTick()) {
                resolve(level, session, state);
            }
        }
    }

    public static void cancel(UUID boardId) {
        ACTIVE.remove(boardId);
    }

    private static boolean ready(BoardSession session, BattleState state) {
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        return attacker != null && defender != null
                && (attacker.bot() || state.attackerReady())
                && (defender.bot() || state.defenderReady());
    }

    private static void resolve(ServerLevel level, BoardSession session, BattleState state) {
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (attacker == null || defender == null) {
            ACTIVE.remove(session.id());
            BoardSessionManager.resumeAfterBattle(level, session);
            return;
        }

        List<Integer> attackerCards = validatedOrEmpty(attacker, state.attackerCards(), CardType.ATTACK);
        List<Integer> defenderCards = validatedOrEmpty(defender, state.defenderCards(), CardType.DEFENSE);
        int attackerDie = Mth.nextInt(level.getRandom(), 1, 6);
        int defenderDie = Mth.nextInt(level.getRandom(), 1, 6);
        int attackBonus = cardBonus(attacker, attackerCards, CardType.ATTACK, level);
        int defenseBonus = cardBonus(defender, defenderCards, CardType.DEFENSE, level);
        int attackTotal = attacker.stats().attack() + attackerDie + attackBonus;
        int damage;
        boolean evaded = false;
        if ("evade".equals(state.defenseMode())) {
            evaded = defenderDie > attackerDie || defenderDie == 6;
            damage = evaded ? 0 : Math.max(0, attackTotal);
        } else {
            int defenseTotal = defender.stats().defense() + defenderDie + defenseBonus;
            damage = Math.max(1, attackTotal - defenseTotal);
        }

        if (damage > 0) damage += defender.stats().incomingDamageBonus();
        AstralPlayerStats nextStats = defender.stats().damage(damage);
        BoardParticipant nextAttacker = removeCards(attacker, attackerCards);
        BoardParticipant nextDefender = removeCards(defender.withStats(nextStats), defenderCards);
        if (nextStats.health() <= 0) {
            int lostCoins = Math.max(0, (nextDefender.stats().starCoins() + 1) / 2);
            nextDefender = nextDefender.knockDown();
            nextAttacker = nextAttacker.withStats(nextAttacker.stats().addCoins(lostCoins));
        }

        BoardSessionManager.updateParticipant(level, session, nextAttacker);
        BoardSessionManager.updateParticipant(level, session, nextDefender);
        String result = (evaded
                ? Component.translatable("message.astral_craft.board.battle_evaded", attackerDie, defenderDie)
                : Component.translatable("message.astral_craft.board.battle_damage", damage, attackerDie, defenderDie))
                .getString();
        BattleState finished = state.withSelections(attackerCards, defenderCards)
                .withResult(level.getGameTime() + RESULT_TICKS);
        ACTIVE.put(session.id(), finished);
        send(level, session, finished, true, result);
    }

    private static BoardParticipant removeCards(BoardParticipant participant, List<Integer> indexes) {
        if (indexes.isEmpty()) return participant;
        BoardParticipant result = participant;
        List<Integer> descending = new ArrayList<>(indexes);
        descending.sort(Comparator.reverseOrder());
        for (int index : descending) {
            result = result.removeCard(index);
        }
        return result;
    }

    private static int cardBonus(BoardParticipant participant, List<Integer> indexes, CardType expected,
                                 ServerLevel level) {
        List<Integer> valid = validatedOrEmpty(participant, indexes, expected);
        int bonus = 0;
        for (int index : valid) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition == null) continue;
            int maximum = switch (definition.combatCost()) {
                case 3 -> 10;
                case 2 -> 6;
                default -> 3;
            };
            bonus += Mth.nextInt(level.getRandom(), 1, maximum);
        }
        return bonus;
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
        if (definition == null || definition.type() != expected || definition.combatCost() <= 0) return null;
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

    private static List<Integer> chooseBotCards(ServerLevel level, BoardParticipant participant, CardType expected) {
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            if (combatDefinition(participant, index, expected) != null) candidates.add(index);
        }
        if (candidates.isEmpty()) return List.of();
        Collections.shuffle(candidates, new java.util.Random(level.getRandom().nextLong()));
        List<Integer> chosen = new ArrayList<>();
        int spent = 0;
        for (int index : candidates) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition == null || spent + definition.combatCost() > MAXIMUM_PVP_COST) continue;
            chosen.add(index);
            spent += definition.combatCost();
            if (spent >= MAXIMUM_PVP_COST) break;
        }
        chosen.sort(Integer::compareTo);
        return List.copyOf(chosen);
    }

    private static void send(ServerLevel level, BoardSession session, BattleState state, boolean resolved,
                             String result) {
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
            String cards = own == null ? ""
                    : encodeCombatCards(own, "attacker".equals(role) ? CardType.ATTACK : CardType.DEFENSE);
            int remaining = resolved
                    ? (int) Math.max(0L, state.resultUntilTick() - level.getGameTime())
                    : (int) Math.max(0L, state.deadlineTick() - level.getGameTime());
            PacketDistributor.sendToPlayer(viewer, new OpenBoardBattlePayload(session.id().toString(),
                    attackerEntity, defenderEntity, BoardSessionManager.displayName(level, attacker),
                    BoardSessionManager.displayName(level, defender), cards, role, remaining,
                    MAXIMUM_PVP_COST, resolved, result));
        }
    }

    private static String encodeCombatCards(BoardParticipant participant, CardType expected) {
        StringJoiner output = new StringJoiner(";");
        for (int index = 0; index < participant.hand().size(); index++) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            if (definition == null) continue;
            output.add(index + "," + participant.hand().get(index) + "," + definition.combatCost());
        }
        return output.toString();
    }

    private record BattleState(UUID boardId, UUID attackerSlot, UUID defenderSlot, long deadlineTick,
                               List<Integer> attackerCards, List<Integer> defenderCards, String defenseMode,
                               boolean attackerReady, boolean defenderReady, long resultUntilTick) {
        BattleState {
            attackerCards = List.copyOf(attackerCards);
            defenderCards = List.copyOf(defenderCards);
        }

        BattleState withAttacker(List<Integer> cards, boolean ready) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot, this.deadlineTick,
                    cards, this.defenderCards, this.defenseMode, ready, this.defenderReady, this.resultUntilTick);
        }

        BattleState withDefender(List<Integer> cards, String mode, boolean ready) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot, this.deadlineTick,
                    this.attackerCards, cards, mode, this.attackerReady, ready, this.resultUntilTick);
        }

        BattleState withSelections(List<Integer> attackerCards, List<Integer> defenderCards) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot, this.deadlineTick,
                    attackerCards, defenderCards, this.defenseMode, this.attackerReady, this.defenderReady,
                    this.resultUntilTick);
        }

        BattleState withResult(long until) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot, this.deadlineTick,
                    this.attackerCards, this.defenderCards, this.defenseMode, true, true, until);
        }
    }
}
