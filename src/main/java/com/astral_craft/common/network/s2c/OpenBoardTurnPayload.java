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
        Identifier characterId, Identifier skinId, boolean currentTurn,
        boolean counterResponse) implements CustomPacketPayload {

    private static final int MAXIMUM_CARDS = 64;
    private static final StreamCodec<RegistryFriendlyByteBuf, List<BoardCardView>> CARD_LIST_CODEC =
            BoardCardView.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CARDS));
    public static final Type<OpenBoardTurnPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_turn"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardTurnPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenBoardTurnPayload decode(RegistryFriendlyByteBuf buffer) {
            return new OpenBoardTurnPayload(
                    BoardNetworkCodecs.UUID_STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    CARD_LIST_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    Identifier.STREAM_CODEC.decode(buffer),
                    Identifier.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, OpenBoardTurnPayload value) {
            BoardNetworkCodecs.UUID_STREAM_CODEC.encode(buffer, value.boardId());
            ByteBufCodecs.VAR_INT.encode(buffer, value.characterEntityId());
            CARD_LIST_CODEC.encode(buffer, value.cards());
            ByteBufCodecs.VAR_INT.encode(buffer, value.cardPlaysUsed());
            ByteBufCodecs.VAR_INT.encode(buffer, value.maxCardPlays());
            ByteBufCodecs.VAR_INT.encode(buffer, value.skillCooldownTurns());
            ByteBufCodecs.VAR_INT.encode(buffer, value.decisionTicks());
            ByteBufCodecs.VAR_INT.encode(buffer, value.decisionDurationTicks());
            Identifier.STREAM_CODEC.encode(buffer, value.characterId());
            Identifier.STREAM_CODEC.encode(buffer, value.skinId());
            ByteBufCodecs.BOOL.encode(buffer, value.currentTurn());
            ByteBufCodecs.BOOL.encode(buffer, value.counterResponse());
        }
    };

    public OpenBoardTurnPayload {
        cards = List.copyOf(cards);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
