package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record CharacterProgressEntry(
        boolean unlocked,
        String selectedSkin,
        int level,
        int experience,
        int friendship,
        Set<String> unlockedSkins,
        boolean potentialActivated) {

    public static final int MIN_PVE_LEVEL = 1;
    public static final int MAX_PVE_LEVEL = 6;
    public static final int MIN_FRIENDSHIP_LEVEL = 1;
    public static final int MAX_FRIENDSHIP_LEVEL = 5;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(HashSet::new, List::copyOf);

    public static final Codec<CharacterProgressEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(CharacterProgressEntry::unlocked),
            Codec.STRING.optionalFieldOf("selected_skin", "default").forGetter(CharacterProgressEntry::selectedSkin),
            Codec.INT.optionalFieldOf("level", MIN_PVE_LEVEL).forGetter(CharacterProgressEntry::level),
            Codec.INT.optionalFieldOf("experience", 0).forGetter(CharacterProgressEntry::experience),
            Codec.INT.optionalFieldOf("friendship", MIN_FRIENDSHIP_LEVEL).forGetter(CharacterProgressEntry::friendship),
            STRING_SET_CODEC.optionalFieldOf("unlocked_skins", Set.of("default")).forGetter(CharacterProgressEntry::unlockedSkins),
            Codec.BOOL.optionalFieldOf("potential_activated", false).forGetter(CharacterProgressEntry::potentialActivated)
    ).apply(instance, CharacterProgressEntry::new));

    public static CharacterProgressEntry locked() {
        return new CharacterProgressEntry(false, "default", MIN_PVE_LEVEL, 0, MIN_FRIENDSHIP_LEVEL, Set.of("default"), false);
    }

    public static CharacterProgressEntry unlockedDefault() {
        return new CharacterProgressEntry(true, "default", MIN_PVE_LEVEL, 0, MIN_FRIENDSHIP_LEVEL, Set.of("default"), false);
    }

    public CharacterProgressEntry {
        selectedSkin = selectedSkin == null || selectedSkin.isBlank() ? "default" : selectedSkin;
        level = clampPveLevel(level);
        experience = Math.max(0, experience);
        friendship = clampFriendshipLevel(friendship);
        Set<String> skins = new HashSet<>();
        if (unlockedSkins != null) {
            for (String skin : unlockedSkins) {
                if (skin != null && !skin.isBlank()) {
                    skins.add(skin);
                }
            }
        }

        skins.add("default");
        if (unlocked) {
            skins.add(selectedSkin);
        }
        unlockedSkins = Set.copyOf(skins);
    }

    public CharacterProgressEntry unlock() {
        return new CharacterProgressEntry(true, this.selectedSkin, this.level, this.experience, this.friendship, this.unlockedSkins, this.potentialActivated);
    }

    public CharacterProgressEntry withSelectedSkin(String skinId) {
        String safeSkin = skinId == null || skinId.isBlank() ? "default" : skinId;
        Set<String> next = new HashSet<>(this.unlockedSkins);
        next.add(safeSkin);
        return new CharacterProgressEntry(this.unlocked, safeSkin, this.level, this.experience, this.friendship, next, this.potentialActivated);
    }

    public CharacterProgressEntry unlockSkin(String skinId) {
        String safeSkin = skinId == null || skinId.isBlank() ? "default" : skinId;
        Set<String> next = new HashSet<>(this.unlockedSkins);
        next.add(safeSkin);
        return new CharacterProgressEntry(this.unlocked, this.selectedSkin, this.level, this.experience, this.friendship, next, this.potentialActivated);
    }

    public CharacterProgressEntry withLevel(int value) {
        return new CharacterProgressEntry(this.unlocked, this.selectedSkin, clampPveLevel(value), this.experience, this.friendship, this.unlockedSkins, this.potentialActivated);
    }

    public CharacterProgressEntry addExperience(int amount) {
        return new CharacterProgressEntry(this.unlocked, this.selectedSkin, this.level, Math.max(0, this.experience + amount), this.friendship, this.unlockedSkins, this.potentialActivated);
    }

    public CharacterProgressEntry withFriendshipLevel(int value) {
        return new CharacterProgressEntry(this.unlocked, this.selectedSkin, this.level, this.experience, clampFriendshipLevel(value), this.unlockedSkins, this.potentialActivated);
    }

    public CharacterProgressEntry addFriendship(int amount) {
        return this.withFriendshipLevel(this.friendship + amount);
    }

    public CharacterProgressEntry activatePotential() {
        return new CharacterProgressEntry(this.unlocked, this.selectedSkin, this.level, this.experience, this.friendship, this.unlockedSkins, true);
    }

    public boolean isSkinUnlocked(String skinId) {
        return this.unlockedSkins.contains(skinId);
    }

    public static int clampPveLevel(int value) {
        return Math.clamp(value, MIN_PVE_LEVEL, MAX_PVE_LEVEL);
    }

    public static int clampFriendshipLevel(int value) {
        return Math.clamp(value, MIN_FRIENDSHIP_LEVEL, MAX_FRIENDSHIP_LEVEL);
    }

}
