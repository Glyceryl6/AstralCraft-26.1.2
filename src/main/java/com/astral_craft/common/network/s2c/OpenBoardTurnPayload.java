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

public record OpenBoardTurnPayload(
        UUID boardId, int characterEntityId, List<BoardCardView> cards,
        int cardPlaysUsed, int maxCardPlays, int skillCooldownTurns,
        int decisionTicks, int decisionDurationTicks,
        Identifier characterId, Identifier skinId, boolean currentTurn) implements CustomPacketPayload {

    private static final int MAXIMUM_CARDS = 64;
    public static final Type<OpenBoardTurnPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_turn"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardTurnPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardTurnPayload::boardId,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::characterEntityId,
            BoardCardView.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CARDS)), OpenBoardTurnPayload::cards,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::cardPlaysUsed,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::maxCardPlays,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::skillCooldownTurns,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::decisionTicks,
            ByteBufCodecs.VAR_INT, OpenBoardTurnPayload::decisionDurationTicks,
            Identifier.STREAM_CODEC, OpenBoardTurnPayload::characterId,
            Identifier.STREAM_CODEC, OpenBoardTurnPayload::skinId,
            ByteBufCodecs.BOOL, OpenBoardTurnPayload::currentTurn,
            OpenBoardTurnPayload::new);

    public OpenBoardTurnPayload {
        cards = List.copyOf(cards);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}