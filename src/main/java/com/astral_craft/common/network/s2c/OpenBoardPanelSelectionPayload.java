package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import com.astral_craft.common.network.BoardPanelEdgeView;
import com.astral_craft.common.network.BoardPanelNodeView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public record OpenBoardPanelSelectionPayload(UUID boardId, ItemStack cardStack, int handIndex,
                                             List<BoardPanelNodeView> nodes,
                                             List<BoardPanelEdgeView> edges) implements CustomPacketPayload {

    public static final Type<OpenBoardPanelSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("open_board_panel_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoardPanelSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, OpenBoardPanelSelectionPayload::boardId,
            ItemStack.OPTIONAL_STREAM_CODEC, OpenBoardPanelSelectionPayload::cardStack,
            ByteBufCodecs.VAR_INT, OpenBoardPanelSelectionPayload::handIndex,
            BoardPanelNodeView.STREAM_CODEC.apply(ByteBufCodecs.list(512)), OpenBoardPanelSelectionPayload::nodes,
            BoardPanelEdgeView.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), OpenBoardPanelSelectionPayload::edges,
            OpenBoardPanelSelectionPayload::new);

    public OpenBoardPanelSelectionPayload {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}