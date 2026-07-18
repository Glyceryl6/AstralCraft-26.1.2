package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenChipSelectionPayload(List<Choice> choices) implements CustomPacketPayload {

    public static final int MAXIMUM_CHOICES = 3;
    public static final CustomPacketPayload.Type<OpenChipSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_chip_selection"));
    public static final StreamCodec<ByteBuf, OpenChipSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            Choice.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CHOICES)),
            OpenChipSelectionPayload::choices,
            OpenChipSelectionPayload::new);

    public OpenChipSelectionPayload {
        choices = List.copyOf(choices);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Choice(Identifier id, String nameKey, String effectKey, Identifier icon) {

        public static final StreamCodec<ByteBuf, Choice> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC,
                Choice::id,
                ByteBufCodecs.STRING_UTF8,
                Choice::nameKey,
                ByteBufCodecs.STRING_UTF8,
                Choice::effectKey,
                Identifier.STREAM_CODEC,
                Choice::icon,
                Choice::new);
    }

}