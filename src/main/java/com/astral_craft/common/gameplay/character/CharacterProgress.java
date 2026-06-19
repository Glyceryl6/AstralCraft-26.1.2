package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CharacterProgress {

    protected static final Codec<Map<Identifier, CharacterProgressEntry>> ENTRY_MAP_CODEC = Codec.unboundedMap(Identifier.CODEC, CharacterProgressEntry.CODEC);

    public static final Codec<CharacterProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("selected_character", AstralCraft.prefix("mimi")).forGetter(CharacterProgress::selectedCharacter),
            ENTRY_MAP_CODEC.optionalFieldOf("characters", Map.of()).forGetter(CharacterProgress::entries)
    ).apply(instance, CharacterProgress::new));

    protected Identifier selectedCharacter;
    protected Map<Identifier, CharacterProgressEntry> entries = new HashMap<>();

    public CharacterProgress(Identifier selectedCharacter) {
        this(selectedCharacter, Map.of(selectedCharacter, CharacterProgressEntry.unlockedDefault()));
    }

    public CharacterProgress(Identifier selectedCharacter, Map<Identifier, CharacterProgressEntry> entries) {
        this.selectedCharacter = selectedCharacter == null ? AstralCraft.prefix("mimi") : selectedCharacter;
        if (entries != null) {
            this.entries.putAll(entries);
        }
        this.ensureEntry(this.selectedCharacter, true);
    }

    public Identifier selectedCharacter() {
        return this.selectedCharacter;
    }

    public void setSelectedCharacter(Identifier selectedCharacter) {
        this.selectedCharacter = selectedCharacter == null ? AstralCraft.prefix("mimi") : selectedCharacter;
        this.ensureEntry(this.selectedCharacter, true);
    }

    public String selectedSkin() {
        return this.entry(this.selectedCharacter).selectedSkin();
    }

    public void setSelectedSkin(String selectedSkin) {
        CharacterProgressEntry entry = this.entry(this.selectedCharacter).withSelectedSkin(selectedSkin);
        this.entries.put(this.selectedCharacter, entry.unlock());
    }

    public int level() {
        return this.entry(this.selectedCharacter).level();
    }

    public int level(Identifier characterId) {
        return this.entry(characterId).level();
    }

    public void setLevel(int level) {
        this.setLevel(this.selectedCharacter, level);
    }

    public void setLevel(Identifier characterId, int level) {
        this.entries.put(characterId, this.entry(characterId).withLevel(level));
    }

    public int experience() {
        return this.entry(this.selectedCharacter).experience();
    }

    public int experience(Identifier characterId) {
        return this.entry(characterId).experience();
    }

    public void addExperience(int amount) {
        this.addExperience(this.selectedCharacter, amount);
    }

    public void addExperience(Identifier characterId, int amount) {
        this.entries.put(characterId, this.entry(characterId).addExperience(amount));
    }

    public int friendship() {
        return this.entry(this.selectedCharacter).friendship();
    }

    public int friendship(Identifier characterId) {
        return this.entry(characterId).friendship();
    }

    public void addFriendship(int amount) {
        this.addFriendship(this.selectedCharacter, amount);
    }

    public void addFriendship(Identifier characterId, int amount) {
        this.entries.put(characterId, this.entry(characterId).addFriendship(amount));
    }

    public void setFriendshipLevel(int level) {
        this.setFriendshipLevel(this.selectedCharacter, level);
    }

    public void setFriendshipLevel(Identifier characterId, int level) {
        this.entries.put(characterId, this.entry(characterId).withFriendshipLevel(level));
    }

    public boolean isCharacterUnlocked(Identifier characterId) {
        return this.entry(characterId).unlocked();
    }

    public void unlockCharacter(Identifier characterId) {
        this.entries.put(characterId, this.entry(characterId).unlock());
    }

    public Set<Identifier> unlockedCharacters() {
        Set<Identifier> result = new HashSet<>();
        for (Map.Entry<Identifier, CharacterProgressEntry> entry : this.entries.entrySet()) {
            if (entry.getValue().unlocked()) {
                result.add(entry.getKey());
            }
        }
        return Set.copyOf(result);
    }

    public boolean isSkinUnlocked(String skinId) {
        return this.isSkinUnlocked(this.selectedCharacter, skinId);
    }

    public boolean isSkinUnlocked(Identifier characterId, String skinId) {
        return this.entry(characterId).isSkinUnlocked(skinId);
    }

    public void unlockSkin(String skinId) {
        this.unlockSkin(this.selectedCharacter, skinId);
    }

    public void unlockSkin(Identifier characterId, String skinId) {
        this.entries.put(characterId, this.entry(characterId).unlockSkin(skinId));
    }

    public Set<String> unlockedSkins() {
        return this.unlockedSkins(this.selectedCharacter);
    }

    public Set<String> unlockedSkins(Identifier characterId) {
        return this.entry(characterId).unlockedSkins();
    }

    public Map<Identifier, CharacterProgressEntry> entries() {
        return Map.copyOf(this.entries);
    }

    public CharacterProgressEntry entry(Identifier characterId) {
        return this.ensureEntry(characterId, false);
    }

    public void syncUnlockedDefaults(Iterable<CharacterDefinition> definitions) {
        for (CharacterDefinition definition : definitions) {
            CharacterProgressEntry entry = this.entry(definition.id());
            if (definition.unlockedByDefault()) {
                entry = entry.unlock();
            }
            for (CharacterSkinDefinition skin : definition.skins()) {
                if (skin.unlockedByDefault()) {
                    entry = entry.unlockSkin(skin.id());
                }
            }
            this.entries.put(definition.id(), entry);
        }
        this.ensureEntry(this.selectedCharacter, true);
    }

    protected CharacterProgressEntry ensureEntry(Identifier characterId, boolean unlocked) {
        Identifier safeId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        CharacterProgressEntry entry = this.entries.getOrDefault(safeId, unlocked ? CharacterProgressEntry.unlockedDefault() : CharacterProgressEntry.locked());
        if (unlocked && !entry.unlocked()) {
            entry = entry.unlock();
        }
        this.entries.put(safeId, entry);
        return entry;
    }

}
