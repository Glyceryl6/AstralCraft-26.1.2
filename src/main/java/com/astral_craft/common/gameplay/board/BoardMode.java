package com.astral_craft.common.gameplay.board;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum BoardMode implements StringRepresentable {

    PVP("pvp"),
    PVE("pve"),
    UNDECIDED("undecided");

    public static final Codec<BoardMode> CODEC = StringRepresentable.fromEnum(BoardMode::values);
    private final String name;

    BoardMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean decided() {
        return this != UNDECIDED;
    }
}
