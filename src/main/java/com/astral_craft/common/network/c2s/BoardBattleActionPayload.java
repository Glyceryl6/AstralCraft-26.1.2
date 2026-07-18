package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload.DefenseMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

/** Selected card indexes are server-side hand indexes. An empty list means no combat card. */
public record BoardBattleActionPayload(UUID boardId, List<Integer> selectedCardIndexes, DefenseMode defenseMode)
        implements CustomPacketPayload {

    private static final int MAX_SELECTED_CARDS = 7;

    public static final Type<BoardBattleActionPayload> TYPE = new Type<>(AstralCraft.prefix("board_battle_action"));
    public static final StreamCodec<ByteBuf, BoardBattleActionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardBattleActionPayload::boardId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_SELECTED_CARDS)), BoardBattleActionPayload::selectedCardIndexes,
            DefenseMode.STREAM_CODEC, BoardBattleActionPayload::defenseMode,
            BoardBattleActionPayload::new);

    public BoardBattleActionPayload {
        selectedCardIndexes = List.copyOf(selectedCardIndexes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}