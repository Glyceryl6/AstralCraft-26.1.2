package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventContext;
import com.astral_craft.common.gameplay.board.BoardEventTask;
import com.astral_craft.common.gameplay.board.BoardMonsterService;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Deque;

/** Spawns board monsters on distinct random nodes. */
public record BoardSpawnMonstersEventEffect(int count) implements BoardEventEffect {

    public static final MapCodec<BoardSpawnMonstersEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("count", 2).forGetter(BoardSpawnMonstersEventEffect::count)
    ).apply(instance, BoardSpawnMonstersEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_spawn_monsters").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void enqueue(BoardEventContext context, Deque<BoardEventTask> tasks) {
        tasks.addLast(BoardEventTask.action(() -> BoardMonsterService.spawnRandom(
                context.level(), context.session(), Math.max(0, this.count)), 6));
    }

}