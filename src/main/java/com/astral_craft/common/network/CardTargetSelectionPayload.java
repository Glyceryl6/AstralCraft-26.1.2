package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CardTargetSelectionPayload(String cardId, int handIndex, String selectedEntityIds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CardTargetSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_target_selection"));

    public static final StreamCodec<ByteBuf, CardTargetSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CardTargetSelectionPayload::cardId,
            ByteBufCodecs.VAR_INT,
            CardTargetSelectionPayload::handIndex,
            ByteBufCodecs.STRING_UTF8,
            CardTargetSelectionPayload::selectedEntityIds,
            CardTargetSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}