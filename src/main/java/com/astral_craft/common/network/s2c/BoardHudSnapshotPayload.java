package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/** Typed snapshot for the client board HUD and protected-area outline. */
public record BoardHudSnapshotPayload(
        UUID boardId, BlockPos center, BlockPos areaMin, BlockPos areaMax,
        boolean protectionEnabled, boolean playing, List<PawnView> pawns,
        int round, UUID currentSlotId) implements CustomPacketPayload {

    private static final int MAXIMUM_PAWNS = 16;
    private static final UUID EMPTY_SLOT_ID = new UUID(0L, 0L);

    public static final CustomPacketPayload.Type<BoardHudSnapshotPayload> TYPE =
            new CustomPacketPayload.Type<>(AstralCraft.prefix("board_hud_snapshot"));
    public static final StreamCodec<ByteBuf, BoardHudSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardHudSnapshotPayload::boardId,
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, BoardHudSnapshotPayload::center,
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, BoardHudSnapshotPayload::areaMin,
            BoardNetworkCodecs.BLOCK_POS_STREAM_CODEC, BoardHudSnapshotPayload::areaMax,
            ByteBufCodecs.BOOL, BoardHudSnapshotPayload::protectionEnabled,
            ByteBufCodecs.BOOL, BoardHudSnapshotPayload::playing,
            PawnView.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_PAWNS)), BoardHudSnapshotPayload::pawns,
            ByteBufCodecs.VAR_INT, BoardHudSnapshotPayload::round,
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardHudSnapshotPayload::currentSlotId,
            BoardHudSnapshotPayload::new);

    public BoardHudSnapshotPayload {
        pawns = List.copyOf(pawns == null ? List.of() : pawns);
        round = Math.max(1, round);
        currentSlotId = currentSlotId == null ? EMPTY_SLOT_ID : currentSlotId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record PawnView(
            Identifier characterId, Identifier skinId, UUID slotId, String controllerName,
            int starCoins, int health, int maximumHealth, int stars,
            boolean knockedDown, boolean disconnectedHuman, int handCount) {

        public static final StreamCodec<ByteBuf, PawnView> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, PawnView::characterId,
                Identifier.STREAM_CODEC, PawnView::skinId,
                BoardNetworkCodecs.UUID_STREAM_CODEC, PawnView::slotId,
                ByteBufCodecs.STRING_UTF8, PawnView::controllerName,
                ByteBufCodecs.VAR_INT, PawnView::starCoins,
                ByteBufCodecs.VAR_INT, PawnView::health,
                ByteBufCodecs.VAR_INT, PawnView::maximumHealth,
                ByteBufCodecs.VAR_INT, PawnView::stars,
                ByteBufCodecs.BOOL, PawnView::knockedDown,
                ByteBufCodecs.BOOL, PawnView::disconnectedHuman,
                ByteBufCodecs.VAR_INT, PawnView::handCount,
                PawnView::new);

        public PawnView {
            controllerName = controllerName == null ? "" : controllerName;
            starCoins = Math.max(0, starCoins);
            health = Math.max(0, health);
            maximumHealth = Math.max(1, maximumHealth);
            stars = Math.clamp(stars, 0, 3);
            handCount = Math.max(0, handCount);
        }

    }

}