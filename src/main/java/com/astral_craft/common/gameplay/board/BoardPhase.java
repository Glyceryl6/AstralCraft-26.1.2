package com.astral_craft.common.gameplay.board;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum BoardPhase implements StringRepresentable {
    READY("ready"),
    CHARACTER_SELECTION("character_selection"),
    PLAYING("playing"),
    FINISHED("finished");

    public static final Codec<BoardPhase> CODEC = StringRepresentable.fromEnum(BoardPhase::values);

    private final String serializedName;

    BoardPhase(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.serializedName;
    }
}
