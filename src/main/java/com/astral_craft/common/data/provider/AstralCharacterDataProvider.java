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
        futures.add(this.saveCharacter(cache, this.character("al", 1, 1, 10, 0, false, 26, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("alana", 2, 0, 10, 0, true, 12, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("ame", 2, 0, 9, 1, false, 38, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("bonnie", 1, 1, 10, 1, false, 36, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("dorothy", 2, 1, 10, 0, false, 40, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("fanny", 1, 2, 10, 0, true, 11, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("fen", 2, 1, 9, 1, false, 21, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("haiqing", 1, 1, 10, 0, false, 22, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("inkshadow", 2, 1, 9, 1, false, 35, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("jasmine", 1, 2, 11, 0, false, 25, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("jill", 1, 2, 11, 0, false, 39, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("kangel", 1, 1, 10, 0, false, 37, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("komachi", 1, 1, 9, 1, true, 13, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("luka", 2, 0, 9, 1, false, 27, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("lulu", 1, 1, 10, 0, false, 20, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("mamushi", 2, 0, 9, 1, false, 34, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("megas", 3, 0, 11, -1, false, 30, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("mimi", 1, 2, 10, 0, true, 17, 5, List.of("default", "detective", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("misaki", 2, 1, 10, 0, false, 23, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("moses", 2, 1, 10, 0, false, 33, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("nancy_lu", 1, 1, 10, 0, false, 28, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("nardis", 2, 2, 10, 0, false, 24, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("padman", 2, 1, 11, 0, false, 14, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("pandaman", 2, 2, 12, -1, false, 19, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("papara", 2, 0, 9, 1, false, 15, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("parunan", 1, 1, 10, 0, true, 10, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("ren", 1, 2, 10, 0, false, 16, 4, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("rin", 2, 1, 9, 1, false, 29, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("teru", 1, 1, 10, 1, false, 32, 5, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("z3000", 1, 3, 12, -1, false, 18, 3, List.of("default", "alt"))));
        futures.add(this.saveCharacter(cache, this.character("zhao", 1, 2, 10, 0, false, 31, 4, List.of("default", "alt"))));
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

    protected JsonObject character(String id, int attack, int defense, int health, int speed, boolean unlockedByDefault, int sortOrder, int activeCooldown, List<String> skinIds) {
        List<String> normalizedSkins = this.normalizeSkinIds(id, skinIds);
        JsonObject json = new JsonObject();
        json.addProperty("id", AstralCraft.prefix(id).toString());
        json.addProperty("name_key", "character.astral_craft." + id + ".name");
        json.addProperty("title_key", "character.astral_craft." + id + ".title");
        json.addProperty("model", "astral_craft:humanoid");
        json.addProperty("preview_texture", this.skinTexture(id, normalizedSkins.getFirst()));
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
        stats.addProperty("speed", speed);
        json.add("base_stats", stats);

        JsonArray skills = new JsonArray();
        skills.add(this.skill("active", id, activeCooldown));
        skills.add(this.skill("passive", id, 0));
        json.add("skills", skills);

        JsonArray profile = new JsonArray();
        JsonObject profileSection = new JsonObject();
        profileSection.addProperty("title_key", "character.astral_craft." + id + ".profile.basic");
        profileSection.addProperty("body_key", "character.astral_craft." + id + ".profile.basic.body");
        profile.add(profileSection);
        json.add("profile", profile);

        JsonArray skins = new JsonArray();
        for (int i = 0; i < normalizedSkins.size(); i++) {
            String skinId = normalizedSkins.get(i);
            skins.add(this.skin(skinId, id, this.skinTexture(id, skinId), i == 0));
        }
        json.add("skins", skins);

        json.addProperty("unlocked_by_default", unlockedByDefault);
        json.addProperty("unlock_hint_key", "character.astral_craft." + id + ".unlock_hint");
        json.addProperty("sort_order", sortOrder);
        return json;
    }

    protected List<String> normalizeSkinIds(String characterId, List<String> skinIds) {
        Set<String> seen = new LinkedHashSet<>();
        if (skinIds != null) {
            for (String skinId : skinIds) {
                if (skinId == null || skinId.isBlank()) continue;
                if (!seen.add(skinId)) {
                    throw new IllegalArgumentException("Duplicate skin id '" + skinId + "' for character '" + characterId + "'");
                }
            }
        }
        if (seen.isEmpty()) {
            seen.add("default");
        }
        return List.copyOf(seen);
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

    protected JsonObject skin(String skinId, String characterId, String texture, boolean unlockedByDefault) {
        JsonObject json = new JsonObject();
        json.addProperty("id", skinId);
        json.addProperty("name_key", "character.astral_craft." + characterId + ".skin." + skinId);
        json.addProperty("texture", texture);
        json.addProperty("unlocked_by_default", unlockedByDefault);
        return json;
    }

}
