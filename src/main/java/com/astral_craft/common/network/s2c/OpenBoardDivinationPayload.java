package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.fortune.BoardFortuneCategory;
import com.astral_craft.common.gameplay.fortune.BoardFortuneDefinition;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record OpenBoardDivinationPayload(UUID boardId, List<Option> options, boolean selectable,
                                         int timeoutTicks, int timeoutDurationTicks) implements CustomPacketPayload {

    public static final Type<OpenBoardDivinationPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_divination"));
    public static final StreamCodec<ByteBuf, OpenBoardDivinationPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardDivinationPayload::boardId,
            Option.STREAM_CODEC.apply(ByteBufCodecs.list(2)), OpenBoardDivinationPayload::options,
            ByteBufCodecs.BOOL, OpenBoardDivinationPayload::selectable,
            ByteBufCodecs.VAR_INT, OpenBoardDivinationPayload::timeoutTicks,
            ByteBufCodecs.VAR_INT, OpenBoardDivinationPayload::timeoutDurationTicks,
            OpenBoardDivinationPayload::new);

    public OpenBoardDivinationPayload {
        options = List.copyOf(options);
        timeoutTicks = Math.max(0, timeoutTicks);
        timeoutDurationTicks = Math.max(1, timeoutDurationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Option(Identifier id, String nameKey, String descriptionKey, Identifier texture, BoardFortuneCategory category) {
        public static final StreamCodec<ByteBuf, Option> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Option::id,
                ByteBufCodecs.STRING_UTF8, Option::nameKey,
                ByteBufCodecs.STRING_UTF8, Option::descriptionKey,
                Identifier.STREAM_CODEC, Option::texture,
                BoardFortuneCategory.STREAM_CODEC, Option::category, Option::new);

        public static Option from(BoardFortuneDefinition definition) {
            return new Option(definition.id(), definition.nameKey(), definition.descriptionKey(),
                    definition.texture(), definition.category());
        }
    }

}