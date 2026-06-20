package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AstralCharacterDataProvider implements DataProvider {

    protected final PackOutput output;

    public AstralCharacterDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (AstralCharacterDataCatalog.CharacterEntry entry : AstralCharacterDataCatalog.CHARACTERS) {
            futures.add(this.saveCharacter(cache, this.character(entry)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "AstralCraft Characters";
    }

    protected CompletableFuture<?> saveCharacter(CachedOutput cache, JsonObject json) {
        String id = json.get("id").getAsString().substring((AstralCraft.MOD_ID + ":").length());
        Path target = this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(AstralCraft.MOD_ID)
                .resolve(CharacterManager.DIRECTORY)
                .resolve(id + ".json");
        return DataProvider.saveStable(cache, json, target);
    }

    protected JsonObject character(AstralCharacterDataCatalog.CharacterEntry entry) {
        return this.character(entry.id(), entry.attack(), entry.defense(), entry.health(), entry.unlockedByDefault(), entry.sortOrder(), entry.activeCooldown(), entry.skins());
    }

    protected JsonObject character(String id, int attack, int defense, int health, boolean unlockedByDefault, int sortOrder, int activeCooldown, List<AstralCharacterDataCatalog.SkinEntry> skinEntries) {
        this.normalizeSkinEntries(id, skinEntries);
        JsonObject json = new JsonObject();
        json.addProperty("id", AstralCraft.prefix(id).toString());
        json.addProperty("name_key", "character.astral_craft." + id + ".name");
        json.addProperty("title_key", "character.astral_craft." + id + ".title");
        json.addProperty("model", "astral_craft:humanoid");
        json.addProperty("preview_texture", this.skinTexture(id, "default"));
        json.addProperty("entity_type", "astral_craft:astral_character");
        json.addProperty("renderer", "astral_craft:player");
        json.addProperty("animation_set", "astral_craft:humanoid");
        json.addProperty("preview_action", "idle");
        json.addProperty("max_pve_level", 6);
        json.addProperty("max_friendship_level", 5);

        JsonObject stats = new JsonObject();
        stats.addProperty("attack", attack);
        stats.addProperty("defense", defense);
        stats.addProperty("health", health);
        json.add("base_stats", stats);

        JsonArray skills = new JsonArray();
        skills.add(this.skill("active", id, activeCooldown));
        skills.add(this.skill("passive", id, 0));
        json.add("skills", skills);

        JsonArray profile = new JsonArray();
        JsonObject profileSection = new JsonObject();
        profileSection.addProperty("title_key", "");
        profileSection.addProperty("body_key", "character.astral_craft." + id + ".profile.basic.body");
        profile.add(profileSection);
        json.add("profile", profile);

        json.add("skins", new JsonArray());

        json.addProperty("unlocked_by_default", unlockedByDefault);
        json.addProperty("unlock_hint_key", "character.astral_craft." + id + ".unlock_hint");
        json.addProperty("sort_order", sortOrder);
        return json;
    }

    protected List<AstralCharacterDataCatalog.SkinEntry> normalizeSkinEntries(String characterId, List<AstralCharacterDataCatalog.SkinEntry> skins) {
        Set<String> seen = new LinkedHashSet<>();
        List<AstralCharacterDataCatalog.SkinEntry> result = new ArrayList<>();
        if (skins != null) {
            for (AstralCharacterDataCatalog.SkinEntry skin : skins) {
                if (skin == null || skin.id() == null || skin.id().isBlank()) continue;
                if (!seen.add(skin.id())) {
                    throw new IllegalArgumentException("Duplicate skin id '" + skin.id() + "' for character '" + characterId + "'");
                }
                result.add(skin);
            }
        }
        return List.copyOf(result);
    }

    protected String skinTexture(String characterId, String skinId) {
        return AstralCraft.prefix("textures/entity/character/skin_" + characterId + "_" + skinId + ".png").toString();
    }

    protected JsonObject skill(String type, String id, int cooldown) {
        JsonObject json = new JsonObject();
        json.addProperty("id", type);
        json.addProperty("name_key", "character.astral_craft." + id + ".skill." + type);
        json.addProperty("description_key", "character.astral_craft." + id + ".skill." + type + ".desc");
        json.addProperty("cooldown", cooldown);
        return json;
    }

    protected JsonObject skin(String skinId, String characterId, String texture, boolean unlockedByDefault, String rarity) {
        JsonObject json = new JsonObject();
        json.addProperty("id", skinId);
        json.addProperty("name_key", "character.astral_craft." + characterId + ".skin." + skinId);
        json.addProperty("texture", texture);
        json.addProperty("unlocked_by_default", unlockedByDefault);
        json.addProperty("rarity", rarity == null || rarity.isBlank() ? "none" : rarity);
        return json;
    }

}
