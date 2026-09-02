package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.components.CardType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Development-only battle roll overrides. No value in this service is part of normal board progression. */
public class BoardDeveloperBattleOverrideService {

    public static final int RANDOM_DIE = 0;
    public static final int RANDOM_CARD_BONUS = -1;
    public static final int MAX_CARD_BONUS = 999;
    private static final Map<UUID, Map<UUID, BattleOverride>> OVERRIDES = new HashMap<>();

    public static BattleOverride get(UUID boardId, UUID slotId) {
        Map<UUID, BattleOverride> board = boardId == null ? null : OVERRIDES.get(boardId);
        return board == null || slotId == null ? BattleOverride.DEFAULT : board.getOrDefault(slotId, BattleOverride.DEFAULT);
    }

    public static void set(UUID boardId, UUID slotId, BattleOverride value) {
        if (boardId == null || slotId == null) return;
        BattleOverride safe = value == null ? BattleOverride.DEFAULT : value.validated();
        if (safe.equals(BattleOverride.DEFAULT)) {
            Map<UUID, BattleOverride> board = OVERRIDES.get(boardId);
            if (board == null) return;
            board.remove(slotId);
            if (board.isEmpty()) OVERRIDES.remove(boardId);
            return;
        }
        OVERRIDES.computeIfAbsent(boardId, ignored -> new HashMap<>()).put(slotId, safe);
    }

    public static int resolveDie(UUID boardId, UUID slotId, boolean attacker, int fallback) {
        BattleOverride value = get(boardId, slotId);
        int forced = attacker ? value.attackerDie() : value.defenderDie();
        return forced >= 1 && forced <= 6 ? forced : fallback;
    }

    public static int cardBonus(UUID boardId, UUID slotId, CardType type) {
        BattleOverride value = get(boardId, slotId);
        return type == CardType.ATTACK ? value.attackCardBonus() : type == CardType.DEFENSE
                ? value.defenseCardBonus() : RANDOM_CARD_BONUS;
    }

    public static void clear(UUID boardId) {
        if (boardId != null) OVERRIDES.remove(boardId);
    }

    public record BattleOverride(int attackerDie, int defenderDie, int attackCardBonus, int defenseCardBonus) {
        public static final BattleOverride DEFAULT = new BattleOverride(RANDOM_DIE, RANDOM_DIE,
                RANDOM_CARD_BONUS, RANDOM_CARD_BONUS);

        public BattleOverride validated() {
            return new BattleOverride(validDie(this.attackerDie), validDie(this.defenderDie),
                    validCardBonus(this.attackCardBonus), validCardBonus(this.defenseCardBonus));
        }

        private static int validDie(int value) {
            return value >= 1 && value <= 6 ? value : RANDOM_DIE;
        }

        private static int validCardBonus(int value) {
            return value < 0 ? RANDOM_CARD_BONUS : Math.min(value, MAX_CARD_BONUS);
        }
    }
}
