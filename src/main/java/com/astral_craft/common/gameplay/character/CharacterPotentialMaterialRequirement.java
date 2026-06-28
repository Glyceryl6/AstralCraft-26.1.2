package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record CharacterPotentialMaterialRequirement(Identifier itemId, int count) {

    public static final Codec<CharacterPotentialMaterialRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("item").forGetter(CharacterPotentialMaterialRequirement::itemId),
            Codec.INT.optionalFieldOf("count", 1).forGetter(CharacterPotentialMaterialRequirement::count)
    ).apply(instance, CharacterPotentialMaterialRequirement::new));

    public CharacterPotentialMaterialRequirement {
        if (itemId == null) {
            itemId = Identifier.withDefaultNamespace("air");
        }
        count = Math.max(1, count);
    }

    public Item item() {
        return BuiltInRegistries.ITEM.getValue(this.itemId);
    }

    public ItemStack displayStack() {
        Item item = this.item();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public int count(Player player) {
        if (player == null) return 0;
        Item item = this.item();
        if (item == Items.AIR) return 0;
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public boolean satisfied(Player player) {
        return this.count(player) >= this.count;
    }

    public void consume(Player player) {
        if (player == null || this.count <= 0) return;
        Item item = this.item();
        if (item == Items.AIR) return;
        int remaining = this.count;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (remaining <= 0) break;
            if (!stack.is(item)) continue;
            int used = Math.min(stack.getCount(), remaining);
            stack.shrink(used);
            remaining -= used;
        }
        player.getInventory().setChanged();
    }

    public String encode() {
        return this.itemId + "@" + this.count;
    }

    public static CharacterPotentialMaterialRequirement decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return new CharacterPotentialMaterialRequirement(Identifier.withDefaultNamespace("air"), 1);
        }
        String[] parts = raw.split("@", -1);
        Identifier item = Identifier.parse(parts[0]);
        int amount = 1;
        if (parts.length >= 2) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        return new CharacterPotentialMaterialRequirement(item, amount);
    }

}