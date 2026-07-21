package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventContext;
import com.astral_craft.common.gameplay.board.BoardEventTask;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Deque;

public record BoardActivateRoundEventEffect(int turns) implements BoardEventEffect {

    public static final MapCodec<BoardActivateRoundEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("turns", 1).forGetter(BoardActivateRoundEventEffect::turns)
    ).apply(instance, BoardActivateRoundEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_activate_round_event").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks) {
        tasks.addLast(BoardEventTask.action(() -> {
            context.session().mechanics().setTimedEvent(context.definition().id(), Math.max(1, this.turns));
            BoardSessionManager.markChanged(context.level());
        }, 0));
    }
}
