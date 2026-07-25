package com.astral_craft.client.gui.cardback;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

public record CardBackDefinition(Identifier id, String nameKey, Identifier texture, boolean defaultChoice) {

    public static CardBackDefinition scanned(Identifier texture) {
        return new CardBackDefinition(texture, "", texture, false);
    }

    public static CardBackDefinition builtinDefault() {
        Identifier texture = AstralCraft.prefix("textures/gui/cards/card_back.png");
        return new CardBackDefinition(texture, "card_back.astral_craft.default", texture, true);
    }
}
