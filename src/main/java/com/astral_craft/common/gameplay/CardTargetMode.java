package com.astral_craft.common.gameplay;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

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

    public static CardTargetMode byName(String name) {
        String normalized = name == null ? "none" : name.toLowerCase(Locale.ROOT);
        for (CardTargetMode mode : values()) {
            if (mode.serializedName.equals(normalized)) {
                return mode;
            }
        }
        return NONE;
    }
}
