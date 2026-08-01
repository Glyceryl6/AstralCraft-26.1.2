package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenChipSelectionPayload(UUID boardId, List<Choice> choices, int timeoutTicks,
                                       int timeoutDurationTicks) implements CustomPacketPayload {

    public static final UUID NO_BOARD = new UUID(0L, 0L);
    public static final int MAXIMUM_CHOICES = 3;
    public static final CustomPacketPayload.Type<OpenChipSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_chip_selection"));
    public static final StreamCodec<ByteBuf, OpenChipSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenChipSelectionPayload::boardId,
            Choice.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CHOICES)), OpenChipSelectionPayload::choices,
            ByteBufCodecs.VAR_INT, OpenChipSelectionPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenChipSelectionPayload::timeoutDurationTicks,
            OpenChipSelectionPayload::new);

    public OpenChipSelectionPayload {
        boardId = boardId == null ? NO_BOARD : boardId;
        choices = List.copyOf(choices);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    public OpenChipSelectionPayload(List<Choice> choices) {
        this(NO_BOARD, choices, 0, 1);
    }

    public boolean boardSelection() {
        return !NO_BOARD.equals(this.boardId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Choice(Identifier id, String nameKey, String effectKey, Identifier icon) {

        public static final StreamCodec<ByteBuf, Choice> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Choice::id,
                ByteBufCodecs.STRING_UTF8, Choice::nameKey,
                ByteBufCodecs.STRING_UTF8, Choice::effectKey,
                Identifier.STREAM_CODEC, Choice::icon,
                Choice::new);
    }

}
