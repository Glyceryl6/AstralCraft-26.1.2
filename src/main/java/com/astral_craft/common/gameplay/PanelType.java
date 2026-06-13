package com.astral_craft.common.gameplay;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PanelType {
    private final Identifier id;
    private final PanelTrigger trigger;
    private final String nameKey;
    private final String descriptionKey;

    public PanelType(Identifier id, PanelTrigger trigger, String nameKey, String descriptionKey) {
        this.id = id;
        this.trigger = trigger;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
    }

    public Identifier id() {
        return this.id;
    }

    public PanelTrigger trigger() {
        return this.trigger;
    }

    public String nameKey() {
        return this.nameKey;
    }

    public String descriptionKey() {
        return this.descriptionKey;
    }

    public Component displayName() {
        return Component.translatable(this.nameKey);
    }

    public Component description() {
        return Component.translatable(this.descriptionKey);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
