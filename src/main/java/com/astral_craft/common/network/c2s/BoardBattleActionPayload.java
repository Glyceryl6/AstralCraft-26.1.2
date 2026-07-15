package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Selected card indexes are server-side hand indexes. An empty list means no combat card. */
public record BoardBattleActionPayload(String boardId, List<Integer> selectedCardIndexes, String defenseMode)
        implements CustomPacketPayload {

    private static final int MAX_SELECTED_CARDS = 7;
    public static final Type<BoardBattleActionPayload> TYPE = new Type<>(AstralCraft.prefix("board_battle_action"));
    public static final StreamCodec<ByteBuf, BoardBattleActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BoardBattleActionPayload::boardId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_SELECTED_CARDS)), BoardBattleActionPayload::selectedCardIndexes,
            ByteBufCodecs.STRING_UTF8, BoardBattleActionPayload::defenseMode,
            BoardBattleActionPayload::new);

    public BoardBattleActionPayload {
        selectedCardIndexes = List.copyOf(selectedCardIndexes == null ? List.of() : selectedCardIndexes);
        defenseMode = "evade".equals(defenseMode) ? "evade" : "defend";
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
