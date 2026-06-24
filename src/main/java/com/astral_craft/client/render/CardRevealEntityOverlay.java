package com.astral_craft.client.render;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.reveal.ApproachCardRevealAnimation;
import com.astral_craft.client.gui.reveal.CardRevealAnimation;
import com.astral_craft.client.gui.reveal.CardRevealAnimations;
import com.astral_craft.client.gui.reveal.CardRevealDebugSettings;
import com.astral_craft.client.gui.reveal.CardRevealFrame;
import com.astral_craft.client.gui.reveal.CardRevealSettings;
import com.astral_craft.client.gui.reveal.FlipCardRevealAnimation;
import com.astral_craft.common.network.CardRevealEntityPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class CardRevealEntityOverlay {

    public static final ContextKey<EntityCardReveal> CARD_REVEAL = new ContextKey<>(AstralCraft.prefix("card_reveal_entity"));
    public static final CardRevealSettings SETTINGS = new CardRevealSettings();
    public static final CardRevealDebugSettings DEBUG_SETTINGS = new CardRevealDebugSettings();

    private static final Map<Identifier, CardRevealAnimation> ANIMATIONS = new HashMap<>();
    private static final Map<Integer, EntityCardReveal> ACTIVE = new HashMap<>();

    static {
        registerAnimation(new FlipCardRevealAnimation());
        registerAnimation(new ApproachCardRevealAnimation());
    }

    private CardRevealEntityOverlay() {}

    public static void registerAnimation(CardRevealAnimation animation) {
        ANIMATIONS.put(animation.id(), animation);
    }

    public static void show(CardRevealEntityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> show(payload));
    }

    public static void show(CardRevealEntityPayload payload) {
        Identifier animationId = normalizeAnimation(payload.animation());
        CardRevealAnimation animation = ANIMATIONS.get(animationId);
        int defaultDuration = animation.defaultDuration(SETTINGS);
        int duration = payload.durationTicks() > 0 ? Math.max(payload.durationTicks(), defaultDuration) : defaultDuration;
        ACTIVE.put(payload.entityId(), new EntityCardReveal(payload.entityId(), payload.cardId(), payload.cardType(),
                Component.translatable(payload.titleKey()).getString(),
                Component.translatable(payload.bodyKey()).getString(),
                safeParse(payload.largeFrontTexture(), AstralCraft.prefix("textures/item/template_handcard_effect.png")),
                safeParse(payload.largeBackTexture(), AstralCraft.prefix("textures/item/template_handcard_effect.png")),
                animationId, currentClientGameTicks(), duration));
    }

    public static void tick() {
        float now = currentClientGameTicks();
        Iterator<EntityCardReveal> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            EntityCardReveal reveal = iterator.next();
            if (now - reveal.startedAtTicks() >= reveal.durationTicks()) {
                iterator.remove();
            }
        }
    }

    public static EntityCardReveal activeFor(int entityId) {
        EntityCardReveal reveal = ACTIVE.get(entityId);
        if (reveal == null) {
            return null;
        }
        if (currentClientGameTicks() - reveal.startedAtTicks() >= reveal.durationTicks()) {
            ACTIVE.remove(entityId);
            return null;
        }
        return reveal;
    }

    public static CardRevealFrame frame(EntityCardReveal reveal) {
        float age = Math.max(0.0F, currentClientGameTicks() - reveal.startedAtTicks());
        CardRevealAnimation animation = ANIMATIONS.getOrDefault(reveal.animation(), ANIMATIONS.get(CardRevealAnimations.FLIP));
        if (animation instanceof ApproachCardRevealAnimation approach) {
            return approach.frame(age, SETTINGS);
        }
        if (animation instanceof FlipCardRevealAnimation flip) {
            return flip.frame(age, SETTINGS);
        }
        return new CardRevealFrame(true, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0, true);
    }

    private static float currentClientGameTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return minecraft.level.getGameTime();
    }

    private static Identifier normalizeAnimation(String animation) {
        Identifier id = parseAnimationId(animation);
        if (ANIMATIONS.containsKey(id)) {
            return id;
        }
        return CardRevealAnimations.FLIP;
    }

    private static Identifier parseAnimationId(String animation) {
        if (animation == null || animation.isBlank()) {
            return CardRevealAnimations.FLIP;
        }
        try {
            if (animation.indexOf(':') >= 0) {
                return Identifier.parse(animation);
            }
            return AstralCraft.prefix(animation);
        } catch (Exception ignored) {
            return CardRevealAnimations.FLIP;
        }
    }

    private static Identifier safeParse(String id, Identifier fallback) {
        try {
            return Identifier.parse(id);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record EntityCardReveal(int entityId,
                                   String cardId,
                                   String cardType,
                                   String title,
                                   String body,
                                   Identifier frontTexture,
                                   Identifier backTexture,
                                   Identifier animation,
                                   float startedAtTicks,
                                   int durationTicks) {}

}
