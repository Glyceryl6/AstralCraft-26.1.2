package com.astral_craft.common.gameplay.board;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/** Shared travel orientation selected once when a board game starts. */
public enum BoardTravelDirection implements StringRepresentable {

    CLOCKWISE("clockwise", 1),
    COUNTERCLOCKWISE("counterclockwise", -1);

    public static final Codec<BoardTravelDirection> CODEC = StringRepresentable.fromEnum(BoardTravelDirection::values);

    private final String name;
    private final int angularSign;

    BoardTravelDirection(String name, int angularSign) {
        this.name = name;
        this.angularSign = angularSign;
    }

    public int angularSign() {
        return this.angularSign;
    }

    public BoardTravelDirection opposite() {
        return this == CLOCKWISE ? COUNTERCLOCKWISE : CLOCKWISE;
    }

    public static BoardTravelDirection random(RandomSource random) {
        return random.nextBoolean() ? CLOCKWISE : COUNTERCLOCKWISE;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

}