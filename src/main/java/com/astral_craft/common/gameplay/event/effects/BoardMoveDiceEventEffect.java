package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BoardMoveDiceEventEffect(int extraDice) implements AstralEventEffect {

    public static final MapCodec<BoardMoveDiceEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("extra_dice", 1).forGetter(BoardMoveDiceEventEffect::extraDice)
    ).apply(instance, BoardMoveDiceEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_move_dice").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        int dice = Math.max(0, this.extraDice);
        if (dice == 0) return;
        BoardEventTargets.resolve(context, BoardEventTargets.Impact.SAFE).ifPresent(target -> BoardSessionManager.updateParticipant(target.level(),
                target.session(), target.participant().withStats(target.participant().stats()
                        .addBuff(AstralBoardBuffs.HASTE.get(), BoardBuffInstance.PERMANENT, dice - 1))));
    }
}
