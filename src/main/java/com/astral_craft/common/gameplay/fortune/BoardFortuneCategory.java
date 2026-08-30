package com.astral_craft.common.gameplay.fortune;

import com.astral_craft.common.components.CardType;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum BoardFortuneCategory implements StringRepresentable {

    NEUTRAL("neutral"),
    GOOD_LUCK("good_luck"),
    BAD_LUCK("bad_luck"),
    FORTUNE("fortune"),
    MISFORTUNE("misfortune");

    public static final Codec<BoardFortuneCategory> CODEC = StringRepresentable.fromEnum(BoardFortuneCategory::values);
    private final String serializedName;

    BoardFortuneCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    public String cardFrameType() {
        return this == NEUTRAL ? CardType.EVENT.getSerializedName() : "fortune_" + this.serializedName;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }

}