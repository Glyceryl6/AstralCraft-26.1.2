package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChipSelectionPayload(Identifier chipId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChipSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("chip_selection"));
    public static final StreamCodec<ByteBuf, ChipSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ChipSelectionPayload::chipId, ChipSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}