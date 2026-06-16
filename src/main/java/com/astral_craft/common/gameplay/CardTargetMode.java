package com.astral_craft.common.gameplay;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

@MethodsReturnNonnullByDefault
public enum CardTargetMode implements StringRepresentable {

    NONE("none"),
    SELF("self"),
    ALLY("ally"),
    ENEMY_PLAYER("enemy_player"),
    ANY_PLAYER("any_player"),
    TWO_PLAYERS("two_players"),
    PANEL("panel"),
    MONSTER("monster"),
    CHOICE("choice");

    public static final Codec<CardTargetMode> CODEC = StringRepresentable.fromEnum(CardTargetMode::values);
    public static final StreamCodec<ByteBuf, CardTargetMode> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final String serializedName;

    CardTargetMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

}