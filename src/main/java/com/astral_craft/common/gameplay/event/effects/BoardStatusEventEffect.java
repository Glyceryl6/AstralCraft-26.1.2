package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record BoardStatusEventEffect(Identifier status, int turns) implements AstralEventEffect {

    public static final MapCodec<BoardStatusEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("status").forGetter(BoardStatusEventEffect::status),
            Codec.INT.optionalFieldOf("turns", 1).forGetter(BoardStatusEventEffect::turns)
    ).apply(instance, BoardStatusEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("board_status").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        BoardEventTargets.resolve(context).ifPresent(target -> {
            BoardBuff buff = AstralBoardBuffs.REGISTRY.getValue(this.status);
            if (buff != null) {
                int duration = this.turns == Integer.MAX_VALUE ? BoardBuffInstance.PERMANENT : Math.max(1, this.turns);
                BoardSessionManager.updateParticipant(target.level(), target.session(),
                        target.participant().withStats(target.participant().stats().addBuff(buff, duration, 0)));
                return;
            }
            BoardSessionManager.updateParticipant(target.level(), target.session(),
                    target.participant().withRoundStatusEffect(this.status, Math.max(1, this.turns)));
        });
    }
}
