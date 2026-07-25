package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenCardBackSelectionPayload(Identifier selectedId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenCardBackSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_card_back_selection"));
    public static final StreamCodec<ByteBuf, OpenCardBackSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, OpenCardBackSelectionPayload::selectedId, OpenCardBackSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}