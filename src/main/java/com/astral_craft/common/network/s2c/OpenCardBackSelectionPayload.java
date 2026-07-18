package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenCardBackSelectionPayload(List<CardBackDefinition> options, Identifier selectedId) implements CustomPacketPayload {

    public static final int MAXIMUM_OPTIONS = 128;
    public static final CustomPacketPayload.Type<OpenCardBackSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_card_back_selection"));
    public static final StreamCodec<ByteBuf, OpenCardBackSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            CardBackDefinition.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_OPTIONS)),
            OpenCardBackSelectionPayload::options,
            Identifier.STREAM_CODEC,
            OpenCardBackSelectionPayload::selectedId,
            OpenCardBackSelectionPayload::new);

    public OpenCardBackSelectionPayload {
        options = List.copyOf(options);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}