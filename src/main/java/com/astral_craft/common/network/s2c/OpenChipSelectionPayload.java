package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenChipSelectionPayload(String choices) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenChipSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_chip_selection"));

    public static final StreamCodec<ByteBuf, OpenChipSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenChipSelectionPayload::choices, OpenChipSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}