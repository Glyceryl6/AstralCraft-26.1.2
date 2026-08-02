package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.BoardNetworkCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record BoardPanelSelectionPayload(UUID boardId, ItemStack cardStack, int handIndex,
                                         @Nullable Identifier nodeId) implements CustomPacketPayload {

    public static final Type<BoardPanelSelectionPayload> TYPE = new Type<>(AstralCraft.prefix("board_panel_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoardPanelSelectionPayload> STREAM_CODEC =
            StreamCodec.ofMember(BoardPanelSelectionPayload::encode, BoardPanelSelectionPayload::new);

    private BoardPanelSelectionPayload(RegistryFriendlyByteBuf buffer) {
        this(BoardNetworkCodecs.UUID_STREAM_CODEC.decode(buffer), ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer) ? Identifier.STREAM_CODEC.decode(buffer) : null);
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        BoardNetworkCodecs.UUID_STREAM_CODEC.encode(buffer, this.boardId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.cardStack);
        ByteBufCodecs.VAR_INT.encode(buffer, this.handIndex);
        ByteBufCodecs.BOOL.encode(buffer, this.nodeId != null);
        if (this.nodeId != null) Identifier.STREAM_CODEC.encode(buffer, this.nodeId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}