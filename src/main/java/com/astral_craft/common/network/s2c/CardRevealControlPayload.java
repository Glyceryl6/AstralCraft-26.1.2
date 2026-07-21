package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

/** Keeps a reveal on screen while a counter decision is pending, or releases it afterwards. */
public record CardRevealControlPayload(UUID revealId, Action action) implements CustomPacketPayload {

    public static final Type<CardRevealControlPayload> TYPE = new Type<>(AstralCraft.prefix("card_reveal_control"));
    public static final StreamCodec<ByteBuf, CardRevealControlPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, CardRevealControlPayload::revealId,
            Action.STREAM_CODEC, CardRevealControlPayload::action,
            CardRevealControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        HOLD,
        RELEASE;

        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(
                index -> index >= 0 && index < values().length ? values()[index] : RELEASE, Action::ordinal);
    }

}
