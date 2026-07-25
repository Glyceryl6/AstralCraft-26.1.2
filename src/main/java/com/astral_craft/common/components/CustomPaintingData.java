package com.astral_craft.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public record CustomPaintingData(String resource, int width, int height) {

    public static final int MAX_SIZE = 32;
    public static final CustomPaintingData EMPTY = new CustomPaintingData("", 1, 1);
    public static final Codec<CustomPaintingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("resource", "").forGetter(CustomPaintingData::resource),
            Codec.intRange(1, MAX_SIZE).optionalFieldOf("width", 1).forGetter(CustomPaintingData::width),
            Codec.intRange(1, MAX_SIZE).optionalFieldOf("height", 1).forGetter(CustomPaintingData::height)
    ).apply(instance, CustomPaintingData::new));
    public static final StreamCodec<ByteBuf, CustomPaintingData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CustomPaintingData::resource,
            ByteBufCodecs.VAR_INT, CustomPaintingData::width,
            ByteBufCodecs.VAR_INT, CustomPaintingData::height,
            CustomPaintingData::new);

    public CustomPaintingData {
        resource = resource.trim();
        width = Math.clamp(width, 1, MAX_SIZE);
        height = Math.clamp(height, 1, MAX_SIZE);
    }

    public boolean configured() {
        return !this.resource.isBlank();
    }

    public Identifier resourceId() {
        return Identifier.parse(this.resource);
    }

    public boolean jpg() {
        String path = this.resource.toLowerCase(Locale.ROOT);
        return path.endsWith(".jpg") || path.endsWith(".jpeg");
    }

    public static boolean validResource(String resource) {
        if (resource.isBlank()) return false;
        String value = resource.trim();
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) return false;
        try {
            Identifier id = Identifier.parse(value);
            String path = id.getPath().toLowerCase(Locale.ROOT);
            return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

}