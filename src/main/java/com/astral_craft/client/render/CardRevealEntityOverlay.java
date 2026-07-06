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
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CardRevealEntityOverlay {

    public static final ContextKey<EntityCardReveal> CARD_REVEAL = new ContextKey<>(AstralCraft.prefix("card_reveal_entity"));
    public static final CardRevealSettings SETTINGS = new CardRevealSettings();
    public static final CardRevealDebugSettings DEBUG_SETTINGS = new CardRevealDebugSettings();

    private static final Map<Identifier, CardRevealAnimation> ANIMATIONS = new HashMap<>();
    private static final Map<Integer, EntityCardReveal> ACTIVE_BY_ID = new HashMap<>();
    private static final Map<UUID, EntityCardReveal> ACTIVE_BY_UUID = new HashMap<>();

    static {
        registerAnimation(new FlipCardRevealAnimation());
        registerAnimation(new ApproachCardRevealAnimation());
    }

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
        Entity entity = clientEntity(payload.entityId());
        UUID entityUuid = entity == null ? null : entity.getUUID();
        EntityCardReveal reveal = new EntityCardReveal(
                payload.entityId(), entityUuid, payload.cardId(),
                payload.stack(), payload.cardType(),
                payload.title().getString(), payload.body().getString(),
                payload.largeFrontTexture(), payload.largeBackTexture(),
                animationId, currentClientGameTicks(), duration);
        put(reveal);
    }

    public static void tick() {
        float now = currentClientGameTicks();
        Iterator<EntityCardReveal> iterator = ACTIVE_BY_ID.values().iterator();
        while (iterator.hasNext()) {
            EntityCardReveal reveal = iterator.next();
            if (now - reveal.startedAtTicks() >= reveal.durationTicks()) {
                iterator.remove();
                if (reveal.entityUuid() != null) {
                    ACTIVE_BY_UUID.remove(reveal.entityUuid());
                }
            }
        }
    }

    public static Collection<EntityCardReveal> activeReveals() {
        if (ACTIVE_BY_ID.isEmpty()) return List.of();
        List<EntityCardReveal> reveals = new ArrayList<>();
        for (EntityCardReveal reveal : List.copyOf(ACTIVE_BY_ID.values())) {
            EntityCardReveal active = cleanOrNull(reveal);
            if (active != null) reveals.add(active);
        }

        return reveals;
    }

    @Nullable
    public static EntityCardReveal activeFor(Entity entity) {
        EntityCardReveal reveal = activeFor(entity.getId());
        if (reveal != null) return reveal;
        return activeFor(entity.getUUID());
    }

    @Nullable
    public static EntityCardReveal activeFor(int entityId) {
        return cleanOrNull(ACTIVE_BY_ID.get(entityId));
    }

    @Nullable
    public static EntityCardReveal activeFor(@Nullable UUID entityUuid) {
        if (entityUuid == null) return null;
        return cleanOrNull(ACTIVE_BY_UUID.get(entityUuid));
    }

    public static CardRevealFrame frame(EntityCardReveal reveal) {
        float age = Math.max(0.0F, currentClientGameTicks() - reveal.startedAtTicks());
        CardRevealAnimation animation = ANIMATIONS.getOrDefault(reveal.animation(), ANIMATIONS.get(CardRevealAnimations.FLIP));
        if (animation instanceof ApproachCardRevealAnimation approach) return approach.frame(age, SETTINGS);
        if (animation instanceof FlipCardRevealAnimation flip) return flip.frame(age, SETTINGS);
        return new CardRevealFrame(true, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0, true);
    }

    private static void put(EntityCardReveal reveal) {
        EntityCardReveal previous = ACTIVE_BY_ID.put(reveal.entityId(), reveal);
        if (previous != null && previous.entityUuid() != null) {
            ACTIVE_BY_UUID.remove(previous.entityUuid());
        }

        if (reveal.entityUuid() != null) {
            EntityCardReveal previousUuid = ACTIVE_BY_UUID.put(reveal.entityUuid(), reveal);
            if (previousUuid != null && previousUuid.entityId() != reveal.entityId()) {
                ACTIVE_BY_ID.remove(previousUuid.entityId());
            }
        }
    }

    @Nullable
    private static EntityCardReveal cleanOrNull(@Nullable EntityCardReveal reveal) {
        if (reveal == null) return null;
        if (currentClientGameTicks() - reveal.startedAtTicks() < reveal.durationTicks()) return reveal;
        ACTIVE_BY_ID.remove(reveal.entityId());
        if (reveal.entityUuid() != null) {
            ACTIVE_BY_UUID.remove(reveal.entityUuid());
        }

        return null;
    }

    private static float currentClientGameTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return 0.0F;
        return minecraft.level.getGameTime();
    }

    private static Identifier normalizeAnimation(@Nullable Identifier animation) {
        if (animation != null && ANIMATIONS.containsKey(animation)) return animation;
        return CardRevealAnimations.FLIP;
    }

    @Nullable
    private static Entity clientEntity(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        return minecraft.level.getEntity(entityId);
    }

    public record EntityCardReveal(int entityId,
                                   @Nullable UUID entityUuid,
                                   String cardId,
                                   ItemStack stack,
                                   String cardType,
                                   String title,
                                   String body,
                                   Identifier frontTexture,
                                   Identifier backTexture,
                                   Identifier animation,
                                   float startedAtTicks,
                                   int durationTicks) {}

}