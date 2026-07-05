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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AstralHandCardManager {

    public static AstralHandCards hand(ServerPlayer player) {
        return player.getData(AstralAttachments.HAND_CARDS);
    }

    public static void add(ServerPlayer player, Identifier cardId, int count) {
        if (player == null || cardId == null || count <= 0) return;
        Item item = BuiltInRegistries.ITEM.getValue(cardId);
        if (!isUsableEffectCard(item)) return;
        ItemStack stack = new ItemStack(item, count);
        if (!player.addItem(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    public static int clear(ServerPlayer player) {
        if (player == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !isUsableEffectCard(stack.getItem())) continue;
            count += stack.getCount();
            stack.setCount(0);
        }

        return count;
    }

    public static void addRandomEffectCards(ServerPlayer player, int count) {
        if (player == null || count <= 0) return;
        List<Identifier> cards = effectCardIds();
        if (cards.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            Identifier cardId = cards.get(player.getRandom().nextInt(cards.size()));
            add(player, cardId, 1);
        }
    }

    public static void open(ServerPlayer player) {
        if (player == null) return;
        ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (!state.active()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.hand_card_deck.need_character"), true);
            return;
        }

        CardUseService.markDeckScreenOpen(player);
        PacketDistributor.sendToPlayer(player, new OpenHandCardDeckPayload(encodeInventoryEffectCards(player), false));
    }

    public static Identifier safeCardId(String rawCardId) {
        String raw = rawCardId == null ? "" : rawCardId.trim();
        if (raw.contains(":")) {
            return Identifier.parse(raw);
        }

        return AstralCraft.prefix(raw);
    }

    public static boolean isUsableEffectCard(Item item) {
        if (!(item instanceof BaseHandCard)) return false;
        ItemStack stack = new ItemStack(item);
        return stack.get(AstralDataComponents.CARD_TYPE) == CardType.EFFECT;
    }

    public static boolean hasInventoryCard(ServerPlayer player, Identifier cardId) {
        return countInventoryCard(player, cardId) > 0;
    }

    public static int countInventoryCard(ServerPlayer player, Identifier cardId) {
        if (player == null || cardId == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !cardId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) continue;
            if (!isUsableEffectCard(stack.getItem())) continue;
            count += stack.getCount();
        }

        return count;
    }

    public static ItemStack firstInventoryCardStack(ServerPlayer player, Identifier cardId) {
        if (player == null || cardId == null) return ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !cardId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) continue;
            if (!isUsableEffectCard(stack.getItem())) continue;
            return stack;
        }

        return ItemStack.EMPTY;
    }

    public static void removeFromInventory(ServerPlayer player, ItemStack cardStack, int count) {
        if (player == null || cardStack.isEmpty() || count <= 0) return;
        int remaining = count;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (remaining <= 0) break;
            if (stack.isEmpty() || !ItemStack.isSameItem(cardStack, stack)) continue;
            if (!isUsableEffectCard(stack.getItem())) continue;
            int removed = Math.min(remaining, stack.getCount());
            if (!player.getAbilities().instabuild) {
                stack.shrink(removed);
                remaining -= removed;
            }
        }
    }

    protected static String encodeInventoryEffectCards(ServerPlayer player) {
        Map<Identifier, Integer> entries = new LinkedHashMap<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !isUsableEffectCard(stack.getItem())) continue;
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            entries.merge(itemId, stack.getCount(), Integer::sum);
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Identifier, Integer> entry : entries.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (!builder.isEmpty()) builder.append(';');
            builder.append(entry.getKey()).append('|').append(entry.getValue());
        }

        return builder.toString();
    }

    protected static List<Identifier> effectCardIds() {
        List<Identifier> result = new ArrayList<>();
        for (AstralItems.ModelledCardItem modelledCardItem : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = modelledCardItem.item().get();
            if (!isUsableEffectCard(item)) continue;
            result.add(BuiltInRegistries.ITEM.getKey(item));
        }

        return result;
    }

    protected static void save(ServerPlayer player, AstralHandCards handCards) {
        player.setData(AstralAttachments.HAND_CARDS, handCards);
    }

}