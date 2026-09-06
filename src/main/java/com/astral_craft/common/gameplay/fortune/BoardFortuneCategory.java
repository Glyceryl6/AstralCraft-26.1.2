package com.astral_craft.common.gameplay.fortune;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.IntFunction;

public enum BoardFortuneCategory implements StringRepresentable {

    NEUTRAL("neutral"),
    GOOD_LUCK("good_luck"),
    BAD_LUCK("bad_luck"),
    FORTUNE("fortune"),
    MISFORTUNE("misfortune");

    public static final Codec<BoardFortuneCategory> CODEC = StringRepresentable.fromEnum(BoardFortuneCategory::values);
    private static final IntFunction<BoardFortuneCategory> BY_ID = ByIdMap.continuous(
            BoardFortuneCategory::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, BoardFortuneCategory> STREAM_CODEC = ByteBufCodecs.idMapper(
            BY_ID, BoardFortuneCategory::ordinal);
    private final String serializedName;

    BoardFortuneCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    public String cardFrameType() {
        return this == NEUTRAL ? CardType.EVENT.getSerializedName() : this.serializedName;
    }

    public Identifier cardFrameTexture() {
        return AstralCraft.prefix("textures/item/template_handcard_" + this.cardFrameType() + ".png");
    }

    public String translationKey() {
        return "fortune.astral_craft." + this.serializedName + ".name";
    }

    public static Optional<BoardFortuneCategory> fromSerializedName(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return Arrays.stream(values()).filter(category -> category.serializedName.equalsIgnoreCase(value)).findFirst();
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }

}