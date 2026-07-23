package com.astral_craft.common.gameplay.battle;

import com.astral_craft.common.util.AstralServerTickClock;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.components.CombatBonusDefinition;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.DamagePresentation;
import com.astral_craft.common.gameplay.board.*;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.BoardDecisionProgress;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload.*;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Server-authoritative board battle selection, dice presentation and result coordinator. */
public class BoardBattleService {

    public static final int DECISION_TICKS = 20 * 20;
    public static final int DEFENSE_CHOICE_TICKS = 20 * 8;
    public static final int ATTACKER_ROLL_TICKS = 36;
    public static final int DEFENDER_ROLL_TICKS = 84;
    public static final int RESULT_TICKS = 20 * 3;
    public static final int KNOCKOUT_RESULT_TICKS = 20 * 6;
    private static final int BOT_INITIAL_CARD_TICKS = 1;
    private static final int BOT_CARD_INTERVAL_TICKS = 6;
    private static final int CARD_READY_HOLD_TICKS = 12;
    private static final int BOT_FINAL_SCORE_HOLD_TICKS = 32;
    public static final int MAXIMUM_PVP_COST = 3;
    private static final int MAXIMUM_SELECTED_CARDS = 7;
    private static final Map<UUID, BattleState> ACTIVE = new HashMap<>();
    private static final Set<UUID> DELAYED_ATTACKER_KNOCKOUT = new HashSet<>();

    public static void start(ServerLevel level, BoardSession session, BoardParticipant attacker, BoardParticipant defender) {
        if (ACTIVE.containsKey(session.id()) || BoardSessionManager.isHospitalProtected(session, defender)) return;
        boolean attackerAutomated = BoardSessionManager.isAutomated(level, attacker);
        boolean defenderAutomated = BoardSessionManager.isAutomated(level, defender);
        long now = AstralServerTickClock.now(level);
        int attackerDurationTicks = attackerAutomated ? BOT_INITIAL_CARD_TICKS : attacker.decisionDurationTicks(DECISION_TICKS);
        int defenderDurationTicks = defenderAutomated ? BOT_INITIAL_CARD_TICKS : defender.decisionDurationTicks(DECISION_TICKS);
        long attackerDeadlineTick = now + attackerDurationTicks;
        long defenderDeadlineTick = now + defenderDurationTicks;
        BattleState state = new BattleState(session.id(), attacker.slotUuid(), defender.slotUuid(),
                BattlePhase.SELECT, Math.max(attackerDeadlineTick, defenderDeadlineTick),
                Math.max(attackerDurationTicks, defenderDurationTicks), attackerDeadlineTick, attackerDurationTicks,
                defenderDeadlineTick, defenderDurationTicks, List.of(), List.of(), DefenseMode.DEFEND, false, false, null);
        ACTIVE.put(session.id(), state);
        send(level, session, state);
    }

    public static boolean active(UUID boardId) {
        return ACTIVE.containsKey(boardId);
    }

    public static void submit(ServerPlayer player, UUID boardId, List<Integer> selectedCardIndexes, DefenseMode defenseMode) {
        if (boardId == null) return;
        BattleState state = ACTIVE.get(boardId);
        BoardSession session = BoardSessionManager.session(player.level(), boardId).orElse(null);
        if (state == null || session == null) return;
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (attacker == null || defender == null) return;
        if (state.phase() == BattlePhase.SELECT) {
            if (attacker.controlledBy(player.getUUID())) {
                if (state.attackerReady()) return;
                List<Integer> indexes = validateSelection(attacker, selectedCardIndexes, CardType.ATTACK);
                if (indexes == null) return;
                BoardSessionManager.updateParticipant(player.level(), session, attacker.recordManualDecision());
                state = state.withAttacker(indexes, true);
            } else if (defender.controlledBy(player.getUUID())) {
                if (state.defenderReady()) return;
                List<Integer> indexes = validateSelection(defender, selectedCardIndexes, CardType.DEFENSE);
                if (indexes == null) return;
                BoardSessionManager.updateParticipant(player.level(), session, defender.recordManualDecision());
                state = state.withDefenderCards(indexes, true);
            } else {
                return;
            }

            ACTIVE.put(boardId, state);
            if (state.cardsReady()) beginReadyHold(player.level(), session, state);
            else send(player.level(), session, state);
            return;
        }

        if (state.phase() == BattlePhase.DEFENSE_CHOICE && defender.controlledBy(player.getUUID())) {
            BoardSessionManager.updateParticipant(player.level(), session, defender.recordManualDecision());
            boolean evadeAllowed = !BoardSessionManager.hasAllOrNothing(attacker);
            DefenseMode selectedMode = evadeAllowed && defenseMode == DefenseMode.EVADE ? DefenseMode.EVADE : DefenseMode.DEFEND;
            beginDefenderRoll(player.level(), session, state.withDefenseMode(selectedMode));
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

            if (state.phase() == BattlePhase.SELECT) {
                long now = AstralServerTickClock.now(level);
                BattleState next = state;
                BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
                BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
                if (!next.attackerReady() && now >= next.attackerDeadlineTick()) {
                    boolean automated = attacker != null && BoardSessionManager.isAutomated(level, attacker);
                    if (attacker != null && automated) {
                        next = advanceBotAttacker(next, attacker, now);
                    } else {
                        if (attacker != null) BoardSessionManager.updateParticipant(level, session, attacker.recordTimedOutDecision());
                        next = next.withAttackerSelection(List.of(), true, now, 1);
                    }
                }
                if (!next.defenderReady() && now >= next.defenderDeadlineTick()) {
                    boolean automated = defender != null && BoardSessionManager.isAutomated(level, defender);
                    if (defender != null && automated) {
                        next = advanceBotDefender(next, defender, now);
                    } else {
                        if (defender != null) BoardSessionManager.updateParticipant(level, session, defender.recordTimedOutDecision());
                        next = next.withDefenderSelection(List.of(), true, now, 1);
                    }
                }
                if (next.cardsReady()) {
                    beginReadyHold(level, session, next);
                } else if (next != state) {
                    ACTIVE.put(next.boardId(), next);
                    send(level, session, next);
                }
                continue;
            }

            if (AstralServerTickClock.now(level) < state.deadlineTick()) continue;
            switch (state.phase()) {
                case READY -> beginAttackerRoll(level, session, state);
                case ATTACKER_ROLL -> beginDefenseChoice(level, session, state);
                case DEFENSE_CHOICE -> {
                    BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
                    if (defender != null && !BoardSessionManager.isAutomated(level, defender)) {
                        BoardSessionManager.updateParticipant(level, session, defender.recordTimedOutDecision());
                    }
                    beginDefenderRoll(level, session, state.withDefenseMode(DefenseMode.DEFEND));
                }
                case DEFENDER_ROLL -> applyRoll(level, session, state);
                default -> {
                    ACTIVE.remove(state.boardId());
                    if (DELAYED_ATTACKER_KNOCKOUT.remove(state.boardId())) {
                        BoardSessionManager.knockDownFromEffect(level, session, state.attackerSlot());
                    }
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
        if (state.phase() == BattlePhase.SELECT) {
            long nextTick = AstralServerTickClock.now(level) + BOT_INITIAL_CARD_TICKS;
            if (state.attackerSlot().equals(slotId) && !state.attackerReady()) {
                state = state.withAttackerSelection(state.attackerCards(), false, nextTick, BOT_INITIAL_CARD_TICKS);
            }
            if (state.defenderSlot().equals(slotId) && !state.defenderReady()) {
                state = state.withDefenderSelection(state.defenderCards(), false, nextTick, BOT_INITIAL_CARD_TICKS);
            }
            ACTIVE.put(session.id(), state);
            send(level, session, state);
            return;
        }
        if (state.phase() == BattlePhase.DEFENSE_CHOICE && state.defenderSlot().equals(slotId)) {
            beginDefenderRoll(level, session, state.withDefenseMode(DefenseMode.DEFEND));
        }
    }

    public static void cancel(UUID boardId) {
        ACTIVE.remove(boardId);
        DELAYED_ATTACKER_KNOCKOUT.remove(boardId);
    }

    private static BattleState advanceBotAttacker(BattleState state, BoardParticipant participant, long now) {
        List<Integer> target = chooseBotCards(participant, CardType.ATTACK);
        int nextSize = Math.min(target.size(), state.attackerCards().size() + 1);
        List<Integer> cards = List.copyOf(target.subList(0, nextSize));
        boolean ready = cards.size() >= target.size();
        long deadline = ready ? now : now + BOT_CARD_INTERVAL_TICKS;
        return state.withAttackerSelection(cards, ready, deadline, ready ? 1 : BOT_CARD_INTERVAL_TICKS);
    }

    private static BattleState advanceBotDefender(BattleState state, BoardParticipant participant, long now) {
        List<Integer> target = chooseBotCards(participant, CardType.DEFENSE);
        int nextSize = Math.min(target.size(), state.defenderCards().size() + 1);
        List<Integer> cards = List.copyOf(target.subList(0, nextSize));
        boolean ready = cards.size() >= target.size();
        long deadline = ready ? now : now + BOT_CARD_INTERVAL_TICKS;
        return state.withDefenderSelection(cards, ready, deadline, ready ? 1 : BOT_CARD_INTERVAL_TICKS);
    }

    private static void beginReadyHold(ServerLevel level, BoardSession session, BattleState state) {
        BoardParticipant attacker = session.participant(state.attackerSlot()).orElse(null);
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        boolean automatedBattle = attacker != null && defender != null
                && BoardSessionManager.isAutomated(level, attacker)
                && BoardSessionManager.isAutomated(level, defender);
        int holdTicks = automatedBattle ? BOT_FINAL_SCORE_HOLD_TICKS : CARD_READY_HOLD_TICKS;
        BattleState ready = state.withPhase(BattlePhase.READY, AstralServerTickClock.now(level) + holdTicks, holdTicks);
        ACTIVE.put(session.id(), ready);
        send(level, session, ready);
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
        int attackBase = Math.max(0, attacker.stats().attack());
        int defenseBase = Math.max(0, defender.stats().defense() - defender.stats().incomingDamageBonus());
        int attackerDie = Mth.nextInt(level.getRandom(), 1, 6);
        int attackBonus = randomCardBonus(level, attacker, attackerCards, CardType.ATTACK);
        BattleRoll roll = new BattleRoll(attackerCards, defenderCards, attackBase, defenseBase,
                attackRange.minimum(), attackRange.maximum(), defenseRange.minimum(), defenseRange.maximum(),
                attackerDie, 0, attackBonus, 0,
                attackBase + attackerDie + attackBonus, 0,
                0, false, false);
        BattleState rolling = state.withRoll(BattlePhase.ATTACKER_ROLL,
                AstralServerTickClock.now(level) + ATTACKER_ROLL_TICKS, ATTACKER_ROLL_TICKS, roll);
        ACTIVE.put(session.id(), rolling);
        send(level, session, rolling);
    }

    private static void beginDefenseChoice(ServerLevel level, BoardSession session, BattleState state) {
        BoardParticipant defender = session.participant(state.defenderSlot()).orElse(null);
        if (defender == null) {
            cancelAndResume(level, session);
            return;
        }
        if (BoardSessionManager.isAutomated(level, defender)) {
            beginDefenderRoll(level, session, state.withDefenseMode(DefenseMode.DEFEND));
            return;
        }
        int durationTicks = defender.decisionDurationTicks(DEFENSE_CHOICE_TICKS);
        BattleState choosing = state.withPhase(BattlePhase.DEFENSE_CHOICE, AstralServerTickClock.now(level) + durationTicks, durationTicks);
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
        boolean evading = state.defenseMode() == DefenseMode.EVADE;
        int defenseTotal = evading ? defenderDie : preliminary.defenseBase() + defenderDie + defenseBonus;
        boolean evaded = evading && (defenderDie > preliminary.attackerDie() || defenderDie == 6);
        int rawDamage = state.defenseMode() == DefenseMode.EVADE
                ? (evaded ? 0 : Math.max(0, attackTotal + defender.stats().incomingDamageBonus()))
                : Math.max(1, attackTotal - defenseTotal);
        int damage = BoardSessionManager.resolveIncomingDamage(level, session, defender, rawDamage);
        defender = session.participant(defender.slotUuid()).orElse(defender);
        int remainingHealth = Math.max(0, defender.stats().health() - damage);
        BattleRoll roll = new BattleRoll(preliminary.attackerCards(), preliminary.defenderCards(),
                preliminary.attackBase(), preliminary.defenseBase(),
                preliminary.attackCardMinimum(), preliminary.attackCardMaximum(),
                preliminary.defenseCardMinimum(), preliminary.defenseCardMaximum(),
                preliminary.attackerDie(), defenderDie, preliminary.attackBonus(), defenseBonus,
                attackTotal, defenseTotal, damage, evaded, remainingHealth == 0);
        BattleState rolling = state.withRoll(BattlePhase.DEFENDER_ROLL,
                AstralServerTickClock.now(level) + DEFENDER_ROLL_TICKS, DEFENDER_ROLL_TICKS, roll);
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
        boolean allOrNothing = BoardSessionManager.hasAllOrNothing(attacker);
        AstralPlayerStats nextStats = defender.stats().damage(roll.damage());
        BoardParticipant nextAttacker = removeCards(attacker, roll.attackerCards())
                .withoutRoundStatusEffect(BoardSessionManager.ALL_OR_NOTHING_STATUS);
        BoardParticipant nextDefender = removeCards(defender.withStats(nextStats), roll.defenderCards());
        int knockoutCoins = 0;
        if (nextStats.health() <= 0) {
            knockoutCoins = Math.max(0, (nextDefender.stats().starCoins() + 1) / 2);
            nextDefender = nextDefender.knockDown();
            DELAYED_ATTACKER_KNOCKOUT.remove(session.id());
        } else if (allOrNothing) {
            DELAYED_ATTACKER_KNOCKOUT.add(session.id());
        }
        playAttackAnimation(level, nextAttacker);
        playHurtAnimation(level, nextDefender, roll.damage());
        if (roll.damage() >= DamagePresentation.CRITICAL_DAMAGE_THRESHOLD) {
            Entity damagedEntity = nextDefender.entityUuid().map(level::getEntity).orElse(null);
            if (damagedEntity instanceof LivingEntity living) DamagePresentation.playCriticalImpact(level, living);
        }
        BoardSessionManager.updateParticipant(level, session, nextAttacker);
        BoardSessionManager.updateParticipant(level, session, nextDefender);
        if (knockoutCoins > 0) {
            BoardWorldObjectService.awardCoinsNow(level, session, nextAttacker.slotUuid(), knockoutCoins);
        }
        int resultTicks = roll.knockout() ? KNOCKOUT_RESULT_TICKS : RESULT_TICKS;
        BattleState result = state.withPhase(BattlePhase.RESULT, AstralServerTickClock.now(level) + resultTicks, resultTicks);
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

    private static void playHurtAnimation(ServerLevel level, BoardParticipant participant, int damage) {
        if (damage <= 0) return;
        Entity entity = participant.entityUuid().map(level::getEntity).orElse(null);
        if (entity instanceof AstralCharacterEntity character) character.playBoardHurtAnimation(16);
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
            CombatBonusDefinition bonus = combatBonus(participant, index, expected);
            if (bonus == null) continue;
            minimum += bonus.minimum();
            maximum += bonus.maximum();
        }
        return new CardRange(minimum, maximum);
    }

    private static int randomCardBonus(ServerLevel level, BoardParticipant participant, List<Integer> indexes, CardType expected) {
        int result = 0;
        for (int index : validatedOrEmpty(participant, indexes, expected)) {
            CombatBonusDefinition bonus = combatBonus(participant, index, expected);
            if (bonus != null) result += bonus.random(level.getRandom());
        }
        return result;
    }

    private static List<Integer> validateSelection(BoardParticipant participant, List<Integer> indexes, CardType expected) {
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

    private static List<Integer> validatedOrEmpty(BoardParticipant participant, List<Integer> indexes, CardType expected) {
        List<Integer> valid = validateSelection(participant, indexes, expected);
        return valid == null ? List.of() : valid;
    }

    private static CardDefinition combatDefinition(BoardParticipant participant, int index, CardType expected) {
        ItemStack stack = combatStack(participant, index, expected);
        return stack == null ? null : stack.get(AstralDataComponents.CARD_DEFINITION);
    }

    private static CombatBonusDefinition combatBonus(BoardParticipant participant, int index, CardType expected) {
        ItemStack stack = combatStack(participant, index, expected);
        return stack == null ? null : stack.get(AstralDataComponents.COMBAT_BONUS);
    }

    private static ItemStack combatStack(BoardParticipant participant, int index, CardType expected) {
        if (index < 0 || index >= participant.hand().size()) return null;
        Item item = BuiltInRegistries.ITEM.getValue(participant.hand().get(index));
        if (!(item instanceof BaseHandCard)) return null;
        ItemStack stack = new ItemStack(item);
        CardDefinition definition = stack.get(AstralDataComponents.CARD_DEFINITION);
        CombatBonusDefinition bonus = stack.get(AstralDataComponents.COMBAT_BONUS);
        if (definition == null || bonus == null || !bonus.standardPvp() || definition.type() != expected
                || definition.combatCost() <= 0 || definition.combatCost() > MAXIMUM_PVP_COST) return null;
        return stack;
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
        int attackerEntity = BoardEntityService.entityId(level, attacker);
        int defenderEntity = BoardEntityService.entityId(level, defender);
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            BattleRole role = attacker.controlledBy(viewer.getUUID()) ? BattleRole.ATTACKER
                    : defender.controlledBy(viewer.getUUID()) ? BattleRole.DEFENDER : BattleRole.SPECTATOR;
            BoardParticipant own = role == BattleRole.ATTACKER ? attacker
                    : role == BattleRole.DEFENDER ? defender : null;
            List<CombatCardView> cards = own == null || state.phase() != BattlePhase.SELECT ? List.of()
                    : combatCards(own, role == BattleRole.ATTACKER ? CardType.ATTACK : CardType.DEFENSE);
            long deadlineTick = state.deadlineFor(role);
            int durationTicks = state.durationFor(role);
            int remaining = (int) Math.max(0L, deadlineTick - AstralServerTickClock.now(level));
            BoardParticipant progressParticipant = state.phase() == BattlePhase.DEFENSE_CHOICE
                    || role == BattleRole.DEFENDER ? defender : attacker;
            PacketDistributor.sendToPlayer(viewer, new OpenBoardBattlePayload(session.id(),
                    attackerEntity, defenderEntity, BoardSessionManager.displayName(level, attacker),
                    BoardSessionManager.displayName(level, defender), cards, role,
                    new BoardDecisionProgress(remaining, durationTicks,
                            progressParticipant.characterId(), progressParticipant.skinId()),
                    MAXIMUM_PVP_COST, battleView(state, attacker, defender)));
        }
    }

    private static BattleView battleView(BattleState state, BoardParticipant attacker, BoardParticipant defender) {
        BattleRoll roll = state.roll();
        int attackBase = Math.max(0, attacker.stats().attack());
        int defenseBase = Math.max(0, defender.stats().defense() - defender.stats().incomingDamageBonus());
        if (roll == null) {
            CardRange attackRange = cardRange(attacker, state.attackerCards(), CardType.ATTACK);
            CardRange defenseRange = cardRange(defender, state.defenderCards(), CardType.DEFENSE);
            return new BattleView(state.phase(), attacker.stats().health(), defender.stats().health(),
                    attackBase, defenseBase, attackBase + attackRange.minimum(),
                    attackBase + attackRange.maximum(), defenseBase + defenseRange.minimum(),
                    defenseBase + defenseRange.maximum(), 0, 0, 0, 0, 0, 0, 0,
                    false, false, !BoardSessionManager.hasAllOrNothing(attacker),
                    state.attackerReady(), state.defenderReady(), state.defenseMode());
        }
        return new BattleView(state.phase(), attacker.stats().health(), defender.stats().health(),
                roll.attackBase(), roll.defenseBase(),
                roll.attackBase() + roll.attackCardMinimum(), roll.attackBase() + roll.attackCardMaximum(),
                roll.defenseBase() + roll.defenseCardMinimum(), roll.defenseBase() + roll.defenseCardMaximum(),
                roll.attackerDie(), roll.defenderDie(), roll.attackBonus(), roll.defenseBonus(),
                roll.attackTotal(), roll.defenseTotal(), roll.damage(), roll.evaded(), roll.knockout(),
                !BoardSessionManager.hasAllOrNothing(attacker), state.attackerReady(), state.defenderReady(), state.defenseMode());
    }

    private static List<CombatCardView> combatCards(BoardParticipant participant, CardType expected) {
        List<CombatCardView> result = new ArrayList<>();
        for (int index = 0; index < participant.hand().size(); index++) {
            CardDefinition definition = combatDefinition(participant, index, expected);
            CombatBonusDefinition bonus = combatBonus(participant, index, expected);
            ItemStack stack = combatStack(participant, index, expected);
            if (definition != null && bonus != null && stack != null) {
                result.add(new CombatCardView(index, stack, definition.combatCost(),
                        bonus.minimum(), bonus.maximum()));
            }
        }
        return List.copyOf(result);
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

    private record BattleState(UUID boardId, UUID attackerSlot, UUID defenderSlot, BattlePhase phase,
                               long deadlineTick, int decisionDurationTicks,
                               long attackerDeadlineTick, int attackerDurationTicks,
                               long defenderDeadlineTick, int defenderDurationTicks,
                               List<Integer> attackerCards, List<Integer> defenderCards,
                               DefenseMode defenseMode, boolean attackerReady, boolean defenderReady, BattleRoll roll) {
        private BattleState {
            attackerCards = List.copyOf(attackerCards);
            defenderCards = List.copyOf(defenderCards);
            phase = phase == null ? BattlePhase.SELECT : phase;
            defenseMode = defenseMode == DefenseMode.EVADE ? DefenseMode.EVADE : DefenseMode.DEFEND;
            decisionDurationTicks = Math.max(1, decisionDurationTicks);
            attackerDurationTicks = Math.max(1, attackerDurationTicks);
            defenderDurationTicks = Math.max(1, defenderDurationTicks);
        }

        private boolean cardsReady() {
            return this.attackerReady && this.defenderReady;
        }

        private long deadlineFor(BattleRole role) {
            if (this.phase != BattlePhase.SELECT) return this.deadlineTick;
            if (role == BattleRole.DEFENDER) return this.defenderReady ? 0L : this.defenderDeadlineTick;
            if (role == BattleRole.ATTACKER) return this.attackerReady ? 0L : this.attackerDeadlineTick;
            return Math.max(this.attackerReady ? 0L : this.attackerDeadlineTick,
                    this.defenderReady ? 0L : this.defenderDeadlineTick);
        }

        private int durationFor(BattleRole role) {
            if (this.phase != BattlePhase.SELECT) return this.decisionDurationTicks;
            if (role == BattleRole.DEFENDER) return this.defenderDurationTicks;
            if (role == BattleRole.ATTACKER) return this.attackerDurationTicks;
            return Math.max(this.attackerDurationTicks, this.defenderDurationTicks);
        }

        private BattleState withAttacker(List<Integer> cards, boolean ready) {
            return this.withAttackerSelection(cards, ready, this.attackerDeadlineTick, this.attackerDurationTicks);
        }

        private BattleState withAttackerSelection(List<Integer> cards, boolean ready, long deadlineTick, int durationTicks) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    this.phase, this.deadlineTick, this.decisionDurationTicks,
                    deadlineTick, durationTicks, this.defenderDeadlineTick, this.defenderDurationTicks,
                    cards, this.defenderCards, this.defenseMode, ready, this.defenderReady, this.roll);
        }

        private BattleState withDefenderCards(List<Integer> cards, boolean ready) {
            return this.withDefenderSelection(cards, ready, this.defenderDeadlineTick, this.defenderDurationTicks);
        }

        private BattleState withDefenderSelection(List<Integer> cards, boolean ready, long deadlineTick, int durationTicks) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    this.phase, this.deadlineTick, this.decisionDurationTicks,
                    this.attackerDeadlineTick, this.attackerDurationTicks, deadlineTick, durationTicks,
                    this.attackerCards, cards, this.defenseMode, this.attackerReady, ready, this.roll);
        }

        private BattleState withDefenseMode(DefenseMode mode) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    this.phase, this.deadlineTick, this.decisionDurationTicks,
                    this.attackerDeadlineTick, this.attackerDurationTicks,
                    this.defenderDeadlineTick, this.defenderDurationTicks,
                    this.attackerCards, this.defenderCards,
                    mode, this.attackerReady, this.defenderReady, this.roll);
        }

        private BattleState withPhase(BattlePhase phase, long deadlineTick, int durationTicks) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    phase, deadlineTick, durationTicks,
                    deadlineTick, durationTicks, deadlineTick, durationTicks,
                    this.attackerCards, this.defenderCards,
                    this.defenseMode, this.attackerReady, this.defenderReady, this.roll);
        }

        private BattleState withRoll(BattlePhase phase, long deadlineTick, int durationTicks, BattleRoll roll) {
            return new BattleState(this.boardId, this.attackerSlot, this.defenderSlot,
                    phase, deadlineTick, durationTicks,
                    deadlineTick, durationTicks, deadlineTick, durationTicks,
                    roll.attackerCards(), roll.defenderCards(),
                    this.defenseMode, true, true, roll);
        }
    }
}