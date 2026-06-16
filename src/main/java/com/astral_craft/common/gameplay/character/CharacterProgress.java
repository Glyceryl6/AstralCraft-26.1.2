package com.astral_craft.common.gameplay.character;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public class CharacterProgress {

    protected Identifier selectedCharacter;
    protected String selectedSkin;
    protected int level;
    protected int experience;
    protected int friendship;
    protected final Set<String> unlockedSkins = new HashSet<>();

    public CharacterProgress(Identifier selectedCharacter) {
        this.selectedCharacter = selectedCharacter;
        this.selectedSkin = "default";
        this.level = 1;
        this.experience = 0;
        this.friendship = 0;
        this.unlockedSkins.add("default");
    }

    public Identifier selectedCharacter() {
        return this.selectedCharacter;
    }

    public void setSelectedCharacter(Identifier selectedCharacter) {
        this.selectedCharacter = selectedCharacter;
    }

    public String selectedSkin() {
        return this.selectedSkin;
    }

    public void setSelectedSkin(String selectedSkin) {
        this.selectedSkin = selectedSkin;
        this.unlockedSkins.add(selectedSkin);
    }

    public int level() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int experience() {
        return this.experience;
    }

    public void addExperience(int amount) {
        this.experience = Math.max(0, this.experience + amount);
    }

    public int friendship() {
        return this.friendship;
    }

    public void addFriendship(int amount) {
        this.friendship = Math.max(0, this.friendship + amount);
    }

    public boolean isSkinUnlocked(String skinId) {
        return this.unlockedSkins.contains(skinId);
    }

    public void unlockSkin(String skinId) {
        this.unlockedSkins.add(skinId);
    }

}