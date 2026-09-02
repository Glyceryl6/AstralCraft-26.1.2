package com.astral_craft.common.registry.bootstrap;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.platform.HospitalPlatform;
import com.astral_craft.common.gameplay.board.BoardMechanicsState;
import com.astral_craft.common.gameplay.event.*;
import com.astral_craft.common.gameplay.event.effects.*;
import com.astral_craft.common.gameplay.event.type.AstralEventLocalizationKeys;
import com.astral_craft.common.gameplay.event.type.AstralEventTimings;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;

import java.util.List;

/** @noinspection deprecation*/
public class AstralEventBootstrap {

    public static final ResourceKey<Registry<AstralEventDefinition>> EVENTS = ResourceKey.createRegistryKey(AstralCraft.prefix("events"));
    public static final ResourceKey<AstralEventDefinition> REDISTRIBUTION = key("redistribution");
    public static final ResourceKey<AstralEventDefinition> HASTE = key("haste");
    public static final ResourceKey<AstralEventDefinition> PHILANTHROPY = key("philanthropy");
    public static final ResourceKey<AstralEventDefinition> SERVER_BUG = key("server_bug");
    public static final ResourceKey<AstralEventDefinition> FOOD_SAFETY = key("food_safety");
    public static final ResourceKey<AstralEventDefinition> LOTTERY = key("lottery");
    public static final ResourceKey<AstralEventDefinition> MY_GODDESS = key("my_goddess");
    public static final ResourceKey<AstralEventDefinition> DEAFENING_NOISE = key("deafening_noise");
    public static final ResourceKey<AstralEventDefinition> CARD_DESTRUCTION = key("card_destruction");
    public static final ResourceKey<AstralEventDefinition> BIG_SALES = key("big_sales");
    public static final ResourceKey<AstralEventDefinition> CROWD = key("crowd");
    public static final ResourceKey<AstralEventDefinition> FALLING_GIFTS = key("falling_gifts");
    public static final ResourceKey<AstralEventDefinition> ISOLATION = key("isolation");
    public static final ResourceKey<AstralEventDefinition> OVERSPENDING = key("overspending");
    public static final ResourceKey<AstralEventDefinition> BROKEN_POCKET = key("leaking_pocket");
    public static final ResourceKey<AstralEventDefinition> IT_IS_WAR = key("it_is_war");
    public static final ResourceKey<AstralEventDefinition> EQUALITY = key("equality");
    public static final ResourceKey<AstralEventDefinition> SWITCHEROO = key("switcheroo");
    public static final ResourceKey<AstralEventDefinition> MONSTER_INVASION = key("monster_invasion");
    private static final Identifier EQUALITY_GUARD_BUFF = AstralCraft.prefix("equality_guard");
    private static final Identifier FOOD_SAFETY_BUFF = AstralCraft.prefix("food_safety");
    public static final List<ResourceKey<AstralEventDefinition>> BOARD_EVENTS = List.of(
            REDISTRIBUTION, HASTE, PHILANTHROPY, SERVER_BUG, FOOD_SAFETY, LOTTERY,
            MY_GODDESS, DEAFENING_NOISE, CARD_DESTRUCTION, BIG_SALES, CROWD,
            FALLING_GIFTS, ISOLATION, OVERSPENDING, BROKEN_POCKET, IT_IS_WAR, EQUALITY, SWITCHEROO, MONSTER_INVASION);

    public static void bootstrap(BootstrapContext<AstralEventDefinition> context) {
        context.register(REDISTRIBUTION, boardEvent("redistribution", List.of(new BoardBalanceCoinsEventEffect()), List.of()));
        context.register(HASTE, boardEvent("haste", List.of(all(new BoardMoveDiceEventEffect(1))), List.of()));
        context.register(PHILANTHROPY, boardEvent("philanthropy",
                List.of(selected(BoardForEachParticipantEventEffect.Selection.RICHEST, new BoardCoinEventEffect(-5))), List.of()));
        context.register(SERVER_BUG, boardEvent("server_bug",
                List.of(new BoardTeleportParticipantsEventEffect(BoardTeleportParticipantsEventEffect.Mode.ROTATE_CURRENT)), List.of()));
        context.register(FOOD_SAFETY, boardEvent("food_safety", List.of(
                all(new DamageEventEffect(1.0F)),
                all(new BoardStatusEventEffect(AstralBoardBuffs.turnStartDamage(
                        FOOD_SAFETY_BUFF, 1).duration(3).build()))), List.of()));
        context.register(LOTTERY, boardEvent("lottery", List.of(new BoardSharedLotteryEventEffect()), List.of()));
        context.register(MY_GODDESS, boardEvent("my_goddess", List.of(selected(
                BoardForEachParticipantEventEffect.Selection.ACTIVE, new HealEventEffect(2.0F))), List.of()));
        context.register(DEAFENING_NOISE, boardEvent("deafening_noise", List.of(all(new DamageEventEffect(2.0F))), List.of()));
        context.register(CARD_DESTRUCTION, boardEvent("card_destruction",
                List.of(all(new BoardHandEventEffect(BoardHandEventEffect.Action.DISCARD_RANDOM, 1))), List.of()));
        context.register(BIG_SALES, boardEvent("big_sales", List.of(new BoardActivateRoundEventEffect(3)), List.of()));
        context.register(CROWD, boardEvent("crowd",
                List.of(new BoardTeleportParticipantsEventEffect(BoardTeleportParticipantsEventEffect.Mode.CONNECTED_RANDOM)), List.of()));
        context.register(FALLING_GIFTS, boardEvent("falling_gifts",
                List.of(all(new BoardCoinEventEffect(3), new BoardHandEventEffect(BoardHandEventEffect.Action.GIVE_RANDOM, 1))), List.of()));
        context.register(ISOLATION, boardEvent("isolation",
                List.of(new BoardTeleportParticipantsEventEffect(BoardTeleportParticipantsEventEffect.Mode.HOSPITAL),
                        all(new BoardRoundStatusEventEffect(HospitalPlatform.HOSPITALIZED_STATUS, 1))), List.of()));
        context.register(OVERSPENDING, boardEvent("overspending",
                List.of(all(new BoardTrapEventEffect(BoardMechanicsState.BoardTrapType.DEMOLITION))), List.of()));
        context.register(BROKEN_POCKET, boardEvent("broken_pocket",
                List.of(all(new BoardStatusEventEffect(AstralBoardBuffs.instance(AstralBoardBuffs.LEAKING_POCKET_ID,
                        AstralBoardBuffs.LEAKING_POCKET.get()).permanent().build()))), List.of()));
        context.register(IT_IS_WAR, boardEvent("it_is_war",
                List.of(all(new GiveItemEventEffect(AstralItems.HANDCARD_ATTACK_G.get().builtInRegistryHolder(), 1))), List.of()));
        context.register(EQUALITY, boardEvent("equality",
                List.of(all(new BoardSetHealthEventEffect(1), new BoardStatusEventEffect(
                        AstralBoardBuffs.incomingDamage(EQUALITY_GUARD_BUFF, -10)
                                .permanent().consumeAfterIncomingDamage().build()))), List.of()));
        context.register(SWITCHEROO, boardEvent("switcheroo", List.of(new BoardTransferHandsEventEffect()), List.of()));
        context.register(MONSTER_INVASION, boardEvent("monster_invasion", List.of(new BoardSpawnMonstersEventEffect(2)), List.of()));
    }

    public static ResourceKey<AstralEventDefinition> key(String path) {
        return ResourceKey.create(EVENTS, AstralCraft.prefix(path));
    }

    private static BoardForEachParticipantEventEffect all(AstralEventEffect... effects) {
        return new BoardForEachParticipantEventEffect(List.of(effects));
    }

    private static BoardForEachParticipantEventEffect selected(BoardForEachParticipantEventEffect.Selection selection,
                                                               AstralEventEffect... effects) {
        return new BoardForEachParticipantEventEffect(selection, List.of(effects));
    }

    private static AstralEventDefinition boardEvent(String id, List<AstralEventEffect> effects,
                                                    List<AstralEventEffect> intervalEffects) {
        return event(id, false, List.of(), AstralEventTargetDefinition.DEFAULT, AstralEventTriggerSettings.DEFAULT,
                effects, intervalEffects, List.of(), List.of(), 0, 1.0D, AstralEventTimings.INSTANT, 0, 20);
    }

    public static AstralEventDefinition event(
            String id, boolean triggers, List<AstralEventTriggerCondition> conditions,
            AstralEventTargetDefinition target, AstralEventTriggerSettings triggerSettings,
            List<AstralEventEffect> effects, List<AstralEventEffect> intervalEffects,
            List<AstralActiveEventCondition> activeConditions, List<AstralEventEffect> activeEffects,
            int cooldownTicks, double chance, Identifier timing, int durationTicks, int intervalTicks) {
        return event(id, triggers, conditions, List.of(), target, triggerSettings, effects, intervalEffects,
                activeConditions, activeEffects, cooldownTicks, chance, timing, durationTicks, intervalTicks);
    }

    public static AstralEventDefinition event(
            String id, boolean triggers, List<AstralEventTriggerCondition> conditions,
            List<Difficulty> difficulties, AstralEventTargetDefinition target,
            AstralEventTriggerSettings triggerSettings, List<AstralEventEffect> effects,
            List<AstralEventEffect> intervalEffects, List<AstralActiveEventCondition> activeConditions,
            List<AstralEventEffect> activeEffects, int cooldownTicks, double chance, Identifier timing,
            int durationTicks, int intervalTicks) {
        Identifier eventId = AstralCraft.prefix(id);
        return new AstralEventDefinition(eventId, AstralEventLocalizationKeys.name(eventId),
                AstralEventLocalizationKeys.description(eventId), texture(eventId), triggers, conditions, difficulties,
                target, triggerSettings, effects, intervalEffects, activeConditions, activeEffects, List.of(),
                cooldownTicks, chance, false, timing, durationTicks, intervalTicks);
    }

    public static Identifier texture(Identifier id) {
        return AstralCraft.prefix("textures/gui/cards/event/" + id.getPath() + ".jpg");
    }

}