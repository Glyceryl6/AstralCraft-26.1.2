package com.astral_craft.client.model.character;

public enum AstralCharacterAction {

    IDLE("idle"),
    WALK("walk");

    private final String id;

    AstralCharacterAction(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

}
