package com.astral_craft.common.gameplay.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AstralEventPreferences(String presentation) {

    public static final String PRESENTATION_ANIMATION = "animation";
    public static final String PRESENTATION_CHAT = "chat";
    public static final AstralEventPreferences DEFAULT = new AstralEventPreferences(PRESENTATION_ANIMATION);

    public static final Codec<AstralEventPreferences> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("presentation", PRESENTATION_ANIMATION).forGetter(AstralEventPreferences::presentation)
    ).apply(instance, AstralEventPreferences::new));

    public boolean prefersChat() {
        return PRESENTATION_CHAT.equals(this.presentation);
    }

    public AstralEventPreferences withPresentation(String presentation) {
        if (PRESENTATION_CHAT.equals(presentation)) {
            return new AstralEventPreferences(PRESENTATION_CHAT);
        }

        return DEFAULT;
    }

}