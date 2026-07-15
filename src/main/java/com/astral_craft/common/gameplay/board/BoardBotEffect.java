package com.astral_craft.common.gameplay.board;

import java.util.List;
import java.util.UUID;

/** Optional capability implemented by effect-card items that support automated board use. */
public interface BoardBotEffect {

    default boolean canUseByBoardBot(BoardBotEffectContext context) {
        return true;
    }

    default List<UUID> selectBoardBotTargets(BoardBotEffectContext context) {
        return context.definition().needsTarget()
                ? context.randomOpponentSlot(context.definition().range()).stream().toList()
                : List.of();
    }

    /** Returns the number of ticks to wait before the bot may begin its movement roll. */
    int applyByBoardBot(BoardBotEffectContext context);

}