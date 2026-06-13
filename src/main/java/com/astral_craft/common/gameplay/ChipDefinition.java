package com.astral_craft.common.gameplay;

import com.astral_craft.AstralCraft;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public record ChipDefinition(String id, String nameKey, String effectKey, ChipRarity rarity, Optional<BuffKind> keyword, StatBundle stats) {

    public ChipDefinition(String id, String nameKey, String effectKey, ChipRarity rarity, BuffKind keyword, StatBundle stats) {
        this(id, nameKey, effectKey, rarity, Optional.ofNullable(keyword), stats);
    }

    public Component displayName() {
        return Component.translatable(this.nameKey);
    }

    public Component effectText() {
        return Component.translatable(this.effectKey);
    }

    public static String nameKey(String id) {
        return "chip." + AstralCraft.MOD_ID + "." + id;
    }

    public static String effectKey(String id) {
        return "chip." + AstralCraft.MOD_ID + "." + id + ".desc";
    }
}
