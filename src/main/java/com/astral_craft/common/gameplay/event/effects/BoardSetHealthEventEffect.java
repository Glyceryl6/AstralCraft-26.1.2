package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoardSetHealthEventEffect(int health) implements AstralEventEffect {

    public static final MapCodec<BoardSetHealthEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("health").forGetter(BoardSetHealthEventEffect::health)
    ).apply(instance, BoardSetHealthEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_set_health").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context).ifPresent(target -> BoardSessionManager.updateParticipant(target.level(),
                target.session(), target.participant().withStats(target.participant().stats()
                        .withHealth(Math.max(0, this.health))).withKnockedDownTurns(0)));
    }
}
