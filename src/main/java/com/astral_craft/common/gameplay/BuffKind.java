package com.astral_craft.common.gameplay;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.Objects;

/**
 * Extensible identifier object for Astral Party style buffs.
 *
 * <p>This deliberately is not an enum. Other code can create and register additional kinds through
 * {@link BuffKinds#register(String)} without changing this mod's source.</p>
 */
public final class BuffKind implements StringRepresentable {
    public static final Codec<BuffKind> CODEC = Codec.STRING.xmap(BuffKinds::getOrCreate, BuffKind::getSerializedName);
    public static final StreamCodec<ByteBuf, BuffKind> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final String serializedName;

    BuffKind(String serializedName) {
        this.serializedName = normalize(serializedName);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public static String normalize(String name) {
        return Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof BuffKind other && this.serializedName.equals(other.serializedName);
    }

    @Override
    public int hashCode() {
        return this.serializedName.hashCode();
    }

    @Override
    public String toString() {
        return this.serializedName;
    }
}
