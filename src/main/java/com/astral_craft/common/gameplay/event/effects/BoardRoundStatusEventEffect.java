package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** Applies a non-buff board round status such as hospitalization. */
public record BoardRoundStatusEventEffect(Identifier status, int turns) implements AstralEventEffect {

    public static final MapCodec<BoardRoundStatusEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("status").forGetter(BoardRoundStatusEventEffect::status),
            Codec.INT.optionalFieldOf("turns", 1).forGetter(BoardRoundStatusEventEffect::turns)
    ).apply(instance, BoardRoundStatusEventEffect::new));

    public BoardRoundStatusEventEffect {
        turns = Math.max(1, turns);
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_round_status").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context, BoardEventTargets.Impact.STATUS).ifPresent(target -> BoardSessionManager.updateParticipant(
                target.level(), target.session(), target.participant().withRoundStatusEffect(this.status, this.turns)));
    }

}