package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record OpenHandCardDeckPayload(List<HandCardEntry> cards, boolean creative) implements CustomPacketPayload {

    public static final int MAXIMUM_CARDS = 128;
    public static final CustomPacketPayload.Type<OpenHandCardDeckPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_hand_card_deck"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHandCardDeckPayload> STREAM_CODEC = StreamCodec.composite(
            HandCardEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAXIMUM_CARDS)),
            OpenHandCardDeckPayload::cards,
            ByteBufCodecs.BOOL,
            OpenHandCardDeckPayload::creative,
            OpenHandCardDeckPayload::new);

    public OpenHandCardDeckPayload {
        cards = List.copyOf(cards);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record HandCardEntry(ItemStack stack, int count) {

        public static final StreamCodec<RegistryFriendlyByteBuf, HandCardEntry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC,
                HandCardEntry::stack,
                ByteBufCodecs.VAR_INT,
                HandCardEntry::count,
                HandCardEntry::new);

        public HandCardEntry {
            stack = stack.copyWithCount(1);
            count = Math.max(1, count);
        }

    }

}