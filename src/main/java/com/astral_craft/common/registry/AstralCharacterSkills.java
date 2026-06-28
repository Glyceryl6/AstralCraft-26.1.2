package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.*;
import com.astral_craft.common.gameplay.handcard.AstralHandCardManager;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AstralCharacterSkills {

    public static final ResourceKey<Registry<AstralCharacterSkillSet>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("character_skill_sets"));
    public static final DeferredRegister<AstralCharacterSkillSet> SKILL_SETS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<AstralCharacterSkillSet> REGISTRY = SKILL_SETS.makeRegistry(_ -> {});

    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> DEFAULT = register("default", AstralCharacterSkills::useFallbackSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> MIMI = register("mimi", AstralCharacterSkills::useMimiSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> FEN = register("fen", AstralCharacterSkills::useRecoverySkill, List.of(AstralCharacterSkills::passiveRecoveryPulse));
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> DOROTHY = register("dorothy", AstralCharacterSkills::useRecoverySkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> LULU = register("lulu", AstralCharacterSkills::useRecoverySkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> AME = register("ame", AstralCharacterSkills::useRecoverySkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> NARDIS = register("nardis", AstralCharacterSkills::useNardisSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> PANDAMAN = register("pandaman", AstralCharacterSkills::useFoodSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> PADMAN = register("padman", AstralCharacterSkills::useFoodSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> JILL = register("jill", AstralCharacterSkills::useAttackPulseSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> MEGAS = register("megas", AstralCharacterSkills::useAttackPulseSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> Z3000 = register("z3000", AstralCharacterSkills::useArmorPulseSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> INK_SHADOW = register("ink_shadow", AstralCharacterSkills::useInvisibilitySkill);

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill) {
        return register(characterPath, activeSkill, List.of());
    }

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills) {
        return register(characterPath, activeSkill, passiveSkills, null);
    }

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills, AstralCharacterSkillEffectHandler effectHandler) {
        Identifier characterId = AstralCraft.prefix(characterPath);
        return SKILL_SETS.register(characterPath, () -> new AstralCharacterSkillSet(characterId, activeSkill, passiveSkills, effectHandler, "skill"));
    }

    public static DeferredRegister<AstralCharacterSkillSet> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Optional<AstralCharacterSkillSet> get(Identifier id) {
        return Optional.ofNullable(REGISTRY.getValue(id));
    }

    public static AstralCharacterSkillSet getOrDefault(Identifier id) {
        return get(id).orElseGet(DEFAULT);
    }

    protected static boolean useMimiSkill(CharacterSkillContext context) {
        int cleared = AstralHandCardManager.clear(context.player());
        int drawCount = cleared + 1 + (context.potentialActivated() ? 1 : 0);
        AstralHandCardManager.addRandomEffectCards(context.player(), drawCount);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.mimi", drawCount), true);
        return true;
    }

    protected static boolean useRecoverySkill(CharacterSkillContext context) {
        int duration = AstralCharacterSkillService.durationTicks(context.skill());
        int amount = Math.clamp(context.state().friendship(), 1, 6) + (context.potentialActivated() ? 1 : 0);
        AstralCharacterSkillService.addStatusEffect(context.player(), status(context, AstralStatusEffects.RECOVERY_PULSE_ID, "effect.astral_craft.character_skill.generic", duration,
                Map.of(AstralStatusEffects.propertyKey(AstralCraft.prefix("recovery_amount")), String.valueOf(amount))));
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.heal", amount), true);
        return true;
    }

    protected static boolean useNardisSkill(CharacterSkillContext context) {
        int drawCount = context.potentialActivated() ? 4 : 3;
        AstralHandCardManager.addRandomEffectCards(context.player(), drawCount);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.draw", drawCount), true);
        return true;
    }

    protected static boolean useFoodSkill(CharacterSkillContext context) {
        List<Identifier> foods = List.of(AstralCraft.prefix("handcard_hamburger"), AstralCraft.prefix("handcard_chocolate_cake"));
        Identifier card = foods.get(context.player().getRandom().nextInt(foods.size()));
        AstralHandCardManager.add(context.player(), card, 1);
        int duration = AstralCharacterSkillService.durationTicks(context.skill()) + (context.potentialActivated() ? 200 : 0);
        AstralCharacterSkillService.addStatusEffect(context.player(), status(context, AstralStatusEffects.SNACK_TIME_ID, "effect.astral_craft.character_skill.snack_time", duration,
                Map.of(AstralStatusEffects.propertyKey(AstralCharacterStatSystem.PROPERTY_SPEED_BONUS_PERCENT), context.potentialActivated() ? "18" : "12")));
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.food"), true);
        return true;
    }

    protected static boolean useAttackPulseSkill(CharacterSkillContext context) {
        AstralHandCardManager.addRandomEffectCards(context.player(), 1);
        int duration = AstralCharacterSkillService.durationTicks(context.skill());
        AstralCharacterSkillService.addStatusEffect(context.player(), status(context, AstralStatusEffects.ATTACK_PULSE_ID, "effect.astral_craft.character_skill.attack_pulse", duration,
                Map.of(AstralStatusEffects.propertyKey(AstralCharacterStatSystem.PROPERTY_ATTACK_BONUS), context.potentialActivated() ? "3" : "2")));
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.attack_pulse", duration / 20), true);
        return true;
    }

    protected static boolean useArmorPulseSkill(CharacterSkillContext context) {
        int duration = AstralCharacterSkillService.durationTicks(context.skill());
        AstralCharacterSkillService.addStatusEffect(context.player(), status(context, AstralStatusEffects.ARMOR_PULSE_ID, "effect.astral_craft.character_skill.armor_pulse", duration,
                Map.of(AstralStatusEffects.propertyKey(AstralCharacterStatSystem.PROPERTY_DEFENSE_BONUS), context.potentialActivated() ? "6" : "4")));
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.shield", context.potentialActivated() ? 6 : 4), true);
        return true;
    }

    protected static boolean useInvisibilitySkill(CharacterSkillContext context) {
        int duration = AstralCharacterSkillService.durationTicks(context.skill());
        AstralCharacterSkillService.addStatusEffect(context.player(), status(context, AstralStatusEffects.SHADOW_CLOAK_ID, "effect.astral_craft.character_skill.shadow_cloak", duration,
                Map.of(AstralStatusEffects.propertyKey(AstralCharacterStatSystem.PROPERTY_SPEED_BONUS_PERCENT), context.potentialActivated() ? "24" : "18", AstralStatusEffects.propertyKey(AstralCharacterStatSystem.PROPERTY_VISIBILITY_MODE), context.potentialActivated() ? "deep_shadow" : "shadow")));
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.shadow_cloak", duration / 20), true);
        return true;
    }

    protected static boolean useFallbackSkill(CharacterSkillContext context) {
        AstralHandCardManager.addRandomEffectCards(context.player(), context.potentialActivated() ? 2 : 1);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.fallback"), true);
        return true;
    }

    protected static CharacterSkillEffect status(CharacterSkillContext context, Identifier statusId, String nameKey, int duration, Map<String, String> properties) {
        Identifier handler = context.skill().safeHandler(context.definition().id());
        Identifier safeStatusId = statusId == null ? AstralStatusEffects.GENERIC_STATUS_ID : statusId;
        Map<String, String> mergedProperties = new LinkedHashMap<>();
        if (properties != null) {
            mergedProperties.putAll(properties);
        }
        mergedProperties.putIfAbsent(AstralStatusEffects.propertyKey(AstralStatusEffects.PROPERTY_SKIN), context.state().skinId());
        return new CharacterSkillEffect(safeStatusId.toString(), context.definition().id(), handler, nameKey, duration, 0, mergedProperties);
    }

    protected static void passiveRecoveryPulse(CharacterSkillContext context) {
        if (context.player().tickCount % 200 != 0) return;
        if (context.player().getFoodData().getFoodLevel() < 16) return;
        int duration = 100;
        AstralCharacterSkillService.addStatusEffect(context.player(), status(context, AstralStatusEffects.PASSIVE_RECOVERY_ID, "effect.astral_craft.character_skill.generic", duration,
                Map.of(AstralStatusEffects.propertyKey(AstralCraft.prefix("recovery_amount")), "1")));
    }

}
