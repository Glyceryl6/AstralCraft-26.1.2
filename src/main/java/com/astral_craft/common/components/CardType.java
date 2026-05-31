package com.astral_craft.common.components;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum CardType implements StringRepresentable {

    ATTACK("attack", 0xfe0000),
    DEFENSE("defense", 0x00fdfe),
    EFFECT("effect", 0xd1fe00),
    COUNTER("counter", 0xff00e9),
    JINX("jinx", 0x00d5a1);

    public static final Codec<CardType> CODEC = StringRepresentable.fromEnum(CardType::values);
    public static final StreamCodec<ByteBuf, CardType> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    private final String name;
    private final int color;

    CardType(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getColor() {
        return this.color;
    }

}