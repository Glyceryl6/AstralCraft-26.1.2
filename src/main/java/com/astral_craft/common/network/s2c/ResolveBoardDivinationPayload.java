package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.fortune.DivinationTarget;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ResolveBoardDivinationPayload(UUID boardId, int selectedIndex,
                                             OpenBoardDivinationPayload.Option selectedOption,
                                             DivinationTarget target) implements CustomPacketPayload {

    public static final Type<ResolveBoardDivinationPayload> TYPE = new Type<>(AstralCraft.prefix("resolve_board_divination"));
    private static final StreamCodec<ByteBuf, DivinationTarget> TARGET_CODEC = ByteBufCodecs.idMapper(
            index -> DivinationTarget.values()[Math.clamp(index, 0, DivinationTarget.values().length - 1)],
            DivinationTarget::ordinal);
    public static final StreamCodec<ByteBuf, ResolveBoardDivinationPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, ResolveBoardDivinationPayload::boardId,
            ByteBufCodecs.VAR_INT, ResolveBoardDivinationPayload::selectedIndex,
            OpenBoardDivinationPayload.Option.STREAM_CODEC, ResolveBoardDivinationPayload::selectedOption,
            TARGET_CODEC, ResolveBoardDivinationPayload::target, ResolveBoardDivinationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}