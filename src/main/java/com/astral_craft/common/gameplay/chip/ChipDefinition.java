package com.astral_craft.common.gameplay.chip;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.StatBundle;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.registry.AstralBoardBuffs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record ChipDefinition(String id, String nameKey, String effectKey, ChipRarity rarity,
                             Optional<BoardBuff> keyword, StatBundle stats, ChipPool pool,
                             Optional<Identifier> mapRestriction) {

    public ChipDefinition(String id, String nameKey, String effectKey, ChipRarity rarity, BoardBuff keyword, StatBundle stats) {
        this(id, nameKey, effectKey, rarity, Optional.ofNullable(keyword), stats, ChipPool.GENERAL, Optional.empty());
    }

    public ChipDefinition(String id, String nameKey, String effectKey, ChipRarity rarity, BoardBuff keyword,
                          StatBundle stats, ChipPool pool) {
        this(id, nameKey, effectKey, rarity, Optional.ofNullable(keyword), stats,
                pool == null ? ChipPool.GENERAL : pool, Optional.empty());
    }

    public Component displayName() {
        return Component.translatable(this.nameKey);
    }

    public Component effectText() {
        return Component.translatable(this.effectKey);
    }

    public Identifier registryId() {
        return AstralCraft.prefix(this.id);
    }

    public Optional<Identifier> keywordId() {
        return this.keyword.map(AstralBoardBuffs.REGISTRY::getKey).filter(id -> id != null);
    }

    public boolean availableOn(Optional<Identifier> mapId) {
        return this.mapRestriction.isEmpty() || this.mapRestriction.equals(mapId);
    }

    public static String nameKey(String id) {
        return "chip." + AstralCraft.MOD_ID + "." + id;
    }

    public static String effectKey(String id) {
        return "chip." + AstralCraft.MOD_ID + "." + id + ".desc";
    }
}
