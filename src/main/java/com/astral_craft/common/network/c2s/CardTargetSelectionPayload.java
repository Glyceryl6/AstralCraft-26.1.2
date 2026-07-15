package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CardTargetSelectionPayload(ItemStack cardStack, int handIndex, List<Integer> selectedEntityIds) implements CustomPacketPayload {

    public static final int MAX_SELECTED_TARGETS = 32;

    public static final CustomPacketPayload.Type<CardTargetSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("card_target_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardTargetSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            CardTargetSelectionPayload::cardStack,
            ByteBufCodecs.VAR_INT,
            CardTargetSelectionPayload::handIndex,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_SELECTED_TARGETS)),
            CardTargetSelectionPayload::selectedEntityIds,
            CardTargetSelectionPayload::new);

    public CardTargetSelectionPayload {
        selectedEntityIds = List.copyOf(selectedEntityIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}