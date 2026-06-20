package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class CharacterSkinManager extends SimpleJsonResourceReloadListener<CharacterSkinAddition> {

    public static final String DIRECTORY = "astral_craft/character_skins";
    public static final CharacterSkinManager INSTANCE = new CharacterSkinManager();

    protected final Map<Identifier, List<CharacterSkinDefinition>> skinsByCharacter = new LinkedHashMap<>();

    public CharacterSkinManager() {
        super(CharacterSkinAddition.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, CharacterSkinAddition> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, List<CharacterSkinDefinition>> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, CharacterSkinAddition> entry : elements.entrySet()) {
            CharacterSkinAddition addition = entry.getValue();
            loaded.computeIfAbsent(addition.character(), ignored -> new ArrayList<>()).add(addition.toSkinDefinition(entry.getKey()));
        }

        this.skinsByCharacter.clear();
        this.skinsByCharacter.putAll(loaded);
    }

    public List<CharacterSkinDefinition> skinsFor(Identifier characterId) {
        return this.skinsByCharacter.getOrDefault(characterId, List.of());
    }

    public static CharacterSkinDefinition defaultSkin(Identifier characterId) {
        return implicitSkin(characterId, "default", true, "none");
    }

    public static CharacterSkinDefinition bondSkin(Identifier characterId) {
        return implicitSkin(characterId, "bond", false, "none");
    }

    protected static CharacterSkinDefinition implicitSkin(Identifier characterId, String skinId, boolean unlockedByDefault, String rarity) {
        return new CharacterSkinDefinition(skinId,
                "character." + characterId.getNamespace() + "." + characterId.getPath() + ".skin." + skinId,
                defaultSkinTexture(characterId, skinId), unlockedByDefault, rarity);
    }

    public static Identifier defaultSkinTexture(Identifier characterId, String skinId) {
        return AstralCraft.prefix("entity/character/skin_" + characterId.getPath() + "_" + skinId);
    }

    public static String implicitSkinNameKey(String characterId, String skinId) {
        return "character." + AstralCraft.MOD_ID + "." + characterId + ".skin." + skinId;
    }

}