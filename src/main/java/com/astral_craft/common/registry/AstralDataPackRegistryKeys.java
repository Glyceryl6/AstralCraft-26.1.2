package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.cardback.CardBackDefinition;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.CharacterSkinAddition;
import com.astral_craft.common.gameplay.character.SkinRarityDefinition;
import com.astral_craft.common.gameplay.event.AstralEventDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class AstralDataPackRegistryKeys {

    public static final ResourceKey<Registry<CharacterDefinition>> CHARACTERS = ResourceKey.createRegistryKey(AstralCraft.prefix("characters"));
    public static final ResourceKey<Registry<CharacterSkinAddition>> CHARACTER_SKINS = ResourceKey.createRegistryKey(AstralCraft.prefix("character_skins"));
    public static final ResourceKey<Registry<SkinRarityDefinition>> SKIN_RARITIES = ResourceKey.createRegistryKey(AstralCraft.prefix("skin_rarities"));
    public static final ResourceKey<Registry<CardBackDefinition>> CARD_BACKS = ResourceKey.createRegistryKey(AstralCraft.prefix("card_backs"));
    public static final ResourceKey<Registry<AstralEventDefinition>> EVENTS = ResourceKey.createRegistryKey(AstralCraft.prefix("events"));

}