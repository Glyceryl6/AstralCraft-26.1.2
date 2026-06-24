package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.OpenHandCardDeckPayload;
import com.astral_craft.common.registry.AstralAttachments;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class AstralHandCardManager {

    public static AstralHandCards hand(ServerPlayer player) {
        return player.getData(AstralAttachments.HAND_CARDS);
    }

    public static void add(ServerPlayer player, Identifier cardId, int count) {
        if (player == null || cardId == null || count <= 0) return;
        AstralHandCards handCards = hand(player);
        handCards.add(cardId, count);
        save(player, handCards);
    }

    public static boolean remove(ServerPlayer player, Identifier cardId, int count) {
        if (player == null || cardId == null || count <= 0) return false;
        AstralHandCards handCards = hand(player);
        boolean removed = handCards.remove(cardId, count);
        if (removed) {
            save(player, handCards);
        }
        return removed;
    }

    public static void open(ServerPlayer player) {
        if (player == null) return;
        ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (!state.active() && !player.getAbilities().instabuild) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.hand_card_deck.need_character"), true);
            return;
        }

        boolean creative = player.getAbilities().instabuild;
        String encodedCards = creative ? encodeCreativeEffectCards() : hand(player).encode();
        PacketDistributor.sendToPlayer(player, new OpenHandCardDeckPayload(encodedCards, creative));
    }

    public static Identifier safeCardId(String rawCardId) {
        String raw = rawCardId == null ? "" : rawCardId.trim();
        if (raw.contains(":")) {
            return Identifier.parse(raw);
        }

        return AstralCraft.prefix(raw);
    }

    public static boolean isUsableEffectCard(Item item) {
        if (!(item instanceof BaseHandCard)) {
            return false;
        }

        ItemStack stack = new ItemStack(item);
        return stack.get(AstralDataComponents.CARD_TYPE) == CardType.EFFECT;
    }

    protected static String encodeCreativeEffectCards() {
        StringBuilder builder = new StringBuilder();
        for (AstralItems.ModelledCardItem modelledCardItem : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = modelledCardItem.item().get();
            if (!isUsableEffectCard(item)) continue;
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(itemId).append('|').append(1);
        }

        return builder.toString();
    }

    protected static void save(ServerPlayer player, AstralHandCards handCards) {
        player.setData(AstralAttachments.HAND_CARDS, handCards);
    }

}