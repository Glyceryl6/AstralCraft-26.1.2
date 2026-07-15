package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.network.CardTargetCandidate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record OpenTargetSelectionPayload(ItemStack cardStack, int handIndex, int minTargets, int maxTargets, int range,
                                         List<CardTargetCandidate> candidates) implements CustomPacketPayload {

    public static final int MAX_CANDIDATES = 128;

    public static final CustomPacketPayload.Type<OpenTargetSelectionPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_target_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTargetSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            OpenTargetSelectionPayload::cardStack,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::handIndex,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::minTargets,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::maxTargets,
            ByteBufCodecs.VAR_INT,
            OpenTargetSelectionPayload::range,
            CardTargetCandidate.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CANDIDATES)),
            OpenTargetSelectionPayload::candidates,
            OpenTargetSelectionPayload::new);

    public OpenTargetSelectionPayload {
        candidates = List.copyOf(candidates);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}