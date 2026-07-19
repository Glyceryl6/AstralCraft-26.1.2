package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public record BoardPanelSelectionPayload(UUID boardId, ItemStack cardStack, int handIndex, Optional<Identifier> nodeId) implements CustomPacketPayload {

    public static final Type<BoardPanelSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("board_panel_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoardPanelSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            BoardNetworkCodecs.UUID_STREAM_CODEC, BoardPanelSelectionPayload::boardId,
            ItemStack.OPTIONAL_STREAM_CODEC, BoardPanelSelectionPayload::cardStack,
            ByteBufCodecs.VAR_INT, BoardPanelSelectionPayload::handIndex,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), BoardPanelSelectionPayload::nodeId,
            BoardPanelSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}