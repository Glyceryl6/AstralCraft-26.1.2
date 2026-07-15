package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UnlockAllCharactersPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UnlockAllCharactersPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("unlock_all_characters"));
    public static final StreamCodec<ByteBuf, UnlockAllCharactersPayload> STREAM_CODEC = StreamCodec.unit(new UnlockAllCharactersPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
