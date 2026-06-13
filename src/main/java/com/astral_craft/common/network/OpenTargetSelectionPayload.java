package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenTargetSelectionPayload(String cardId, int handIndex, int minTargets, int maxTargets, int range, String candidates) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenTargetSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_target_selection"));

    public static final StreamCodec<ByteBuf, OpenTargetSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenTargetSelectionPayload::cardId,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::handIndex,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::minTargets,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::maxTargets,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::range,
            ByteBufCodecs.STRING_UTF8,
            OpenTargetSelectionPayload::candidates,
            OpenTargetSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}