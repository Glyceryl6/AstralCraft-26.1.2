package com.astral_craft.common.gameplay.handcard;

import com.astral_craft.common.components.CardType;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.s2c.OpenHandCardDeckPayload;
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

    public static void add(ServerPlayer player, ItemStack cardStack, int count) {
        if (player == null || cardStack == null || cardStack.isEmpty() || count <= 0) return;
        if (!isUsableEffectCard(cardStack)) return;
        ItemStack stack = cardStack.copyWithCount(count);
        if (!player.addItem(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    public static int clear(ServerPlayer player) {
        if (player == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !isUsableEffectCard(stack)) continue;
            count += stack.getCount();
            stack.setCount(0);
        }

        return count;
    }

    public static void addRandomEffectCards(ServerPlayer player, int count) {
        if (player == null || count <= 0) return;
        List<ItemStack> cards = effectCardStacks();
        if (cards.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            ItemStack cardStack = cards.get(player.getRandom().nextInt(cards.size()));
            add(player, cardStack, 1);
        }
    }

    public static void open(ServerPlayer player) {
        if (player == null) return;
        ActiveCharacterState state = player.getData(AstralAttachments.ACTIVE_CHARACTER);
        if (!state.active()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.hand_card_deck.need_character"), true);
            return;
        }

        PacketDistributor.sendToPlayer(player, new OpenHandCardDeckPayload(encodeInventoryEffectCards(player), false));
    }

    public static boolean isUsableEffectCard(Item item) {
        if (!(item instanceof BaseHandCard)) return false;
        return isUsableEffectCard(new ItemStack(item));
    }

    public static boolean isUsableEffectCard(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BaseHandCard)) return false;
        return stack.get(AstralDataComponents.CARD_TYPE) == CardType.EFFECT;
    }

    public static boolean hasInventoryCard(ServerPlayer player, ItemStack cardStack) {
        return countInventoryCard(player, cardStack) > 0;
    }

    public static int countInventoryCard(ServerPlayer player, ItemStack cardStack) {
        if (player == null || cardStack == null || cardStack.isEmpty()) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, cardStack)) continue;
            if (!isUsableEffectCard(stack)) continue;
            count += stack.getCount();
        }

        return count;
    }

    public static ItemStack firstInventoryCardStack(ServerPlayer player, ItemStack cardStack) {
        if (player == null || cardStack == null || cardStack.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, cardStack)) continue;
            if (!isUsableEffectCard(stack)) continue;
            return stack;
        }

        return ItemStack.EMPTY;
    }

    public static void removeFromInventory(ServerPlayer player, ItemStack cardStack, int count) {
        if (player == null || cardStack == null || cardStack.isEmpty() || count <= 0) return;
        int remaining = count;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (remaining <= 0) break;
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, cardStack)) continue;
            if (!isUsableEffectCard(stack)) continue;
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
            if (stack.isEmpty() || !isUsableEffectCard(stack)) continue;
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

    protected static List<ItemStack> effectCardStacks() {
        List<ItemStack> result = new ArrayList<>();
        for (AstralItems.ModelledCardItem modelledCardItem : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = modelledCardItem.item().get();
            ItemStack stack = new ItemStack(item);
            if (!isUsableEffectCard(stack)) continue;
            result.add(stack);
        }

        return result;
    }

    protected static void save(ServerPlayer player, AstralHandCards handCards) {
        player.setData(AstralAttachments.HAND_CARDS, handCards);
    }

}