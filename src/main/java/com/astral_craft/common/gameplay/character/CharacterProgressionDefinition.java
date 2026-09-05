package com.astral_craft.common.gameplay.character;

/**
 * Static unlock and potential metadata, kept separate from rendering and combat properties.
 */
public class CharacterProgressionDefinition {

    protected CharacterPotentialDefinition potential = CharacterPotentialDefinition.NONE;
    protected boolean implicitBondSkin = true;
    protected boolean unlockedByDefault = true;
    protected String unlockHintKey;
    protected int sortOrder = 1000;

    public static CharacterProgressionDefinition of(int sortOrder) {
        return new CharacterProgressionDefinition().sortOrder(sortOrder);
    }

    public CharacterProgressionDefinition potential(CharacterPotentialDefinition potential) {
        this.potential = potential == null ? CharacterPotentialDefinition.NONE : potential;
        return this;
    }

    public CharacterProgressionDefinition implicitBondSkin(boolean value) {
        this.implicitBondSkin = value;
        return this;
    }

    public CharacterProgressionDefinition unlockedByDefault() {
        this.unlockedByDefault = true;
        return this;
    }

    public CharacterProgressionDefinition unlockHintKey(String unlockHintKey) {
        this.unlockHintKey = unlockHintKey;
        return this;
    }

    public CharacterProgressionDefinition sortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public CharacterPotentialDefinition potential() {
        return this.potential;
    }

    public boolean implicitBondSkin() {
        return this.implicitBondSkin;
    }

    public boolean unlockedByDefaultValue() {
        return this.unlockedByDefault;
    }

    public String unlockHintKey() {
        return this.unlockHintKey;
    }

    public int sortOrder() {
        return this.sortOrder;
    }

    public CharacterProgressionDefinition copy() {
        return new CharacterProgressionDefinition().potential(this.potential).implicitBondSkin(this.implicitBondSkin)
                .unlockHintKey(this.unlockHintKey).sortOrder(this.sortOrder).setUnlocked(this.unlockedByDefault);
    }

    protected CharacterProgressionDefinition setUnlocked(boolean value) {
        this.unlockedByDefault = value;
        return this;
    }

}