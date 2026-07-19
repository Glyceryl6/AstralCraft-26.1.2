package com.astral_craft.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Data-driven random bonus range for cards used by the board battle system. */
public record CombatBonusDefinition(int minimum, int maximum, boolean standardPvp) {

    public static final Codec<CombatBonusDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("minimum").forGetter(CombatBonusDefinition::minimum),
            Codec.INT.fieldOf("maximum").forGetter(CombatBonusDefinition::maximum),
            Codec.BOOL.optionalFieldOf("standard_pvp", false).forGetter(CombatBonusDefinition::standardPvp)
    ).apply(instance, CombatBonusDefinition::new));
    public static final StreamCodec<ByteBuf, CombatBonusDefinition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CombatBonusDefinition::minimum,
            ByteBufCodecs.VAR_INT, CombatBonusDefinition::maximum,
            ByteBufCodecs.BOOL, CombatBonusDefinition::standardPvp,
            CombatBonusDefinition::new);

    public CombatBonusDefinition {
        minimum = Math.max(0, minimum);
        maximum = Math.max(minimum, maximum);
    }

    public int random(RandomSource random) {
        return Mth.nextInt(random, this.minimum, this.maximum);
    }

}