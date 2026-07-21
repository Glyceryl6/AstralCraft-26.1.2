package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.common.gameplay.board.BoardEventContext;
import com.astral_craft.common.gameplay.board.BoardEventTask;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;

import java.util.Deque;

/** Data-driven event effect that can enqueue blocking board-game tasks. */
public interface BoardEventEffect extends AstralEventEffect {

    void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks);

    @Override
    default void apply(AstralEventContext context) {}
}
