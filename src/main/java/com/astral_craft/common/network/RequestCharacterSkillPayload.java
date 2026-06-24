package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestCharacterSkillPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestCharacterSkillPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("request_character_skill"));
    public static final StreamCodec<ByteBuf, RequestCharacterSkillPayload> STREAM_CODEC = StreamCodec.unit(new RequestCharacterSkillPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}