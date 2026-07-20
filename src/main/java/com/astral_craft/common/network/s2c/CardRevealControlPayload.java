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
    public static final StreamCodec<ByteBuf, CardRevealControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CardRevealControlPayload decode(ByteBuf buffer) {
            UUID revealId = BoardNetworkCodecs.UUID_STREAM_CODEC.decode(buffer);
            int ordinal = ByteBufCodecs.VAR_INT.decode(buffer);
            return new CardRevealControlPayload(revealId, Action.fromOrdinal(ordinal));
        }

        @Override
        public void encode(ByteBuf buffer, CardRevealControlPayload value) {
            BoardNetworkCodecs.UUID_STREAM_CODEC.encode(buffer, value.revealId());
            ByteBufCodecs.VAR_INT.encode(buffer, value.action().ordinal());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        HOLD,
        RELEASE;

        private static Action fromOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : RELEASE;
        }
    }

}