package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardCardView;
import com.astral_craft.common.network.BoardNetworkCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenBoardDiscardPayload(
        UUID boardId, List<BoardCardView> cards, int requiredCount,
        int timeoutTicks, int timeoutDurationTicks,
        Identifier characterId, Identifier skinId) implements CustomPacketPayload {

    private static final int MAXIMUM_CARDS = 64;
    public static final Type<OpenBoardDiscardPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_discard"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardDiscardPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardDiscardPayload::boardId,
            BoardCardView.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CARDS)), OpenBoardDiscardPayload::cards,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::requiredCount,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardDiscardPayload::timeoutDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardDiscardPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardDiscardPayload::skinId,
            OpenBoardDiscardPayload::new);

    public OpenBoardDiscardPayload {
        cards = List.copyOf(cards);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}