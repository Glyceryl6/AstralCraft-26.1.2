package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.AstralCharacterActiveSkill;
import com.astral_craft.common.gameplay.character.AstralCharacterPassiveSkill;
import com.astral_craft.common.gameplay.character.AstralCharacterSkillSet;
import com.astral_craft.common.gameplay.character.CharacterSkillContext;
import com.astral_craft.common.gameplay.handcard.AstralHandCardManager;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AstralCharacterSkills {

    public static final ResourceKey<Registry<AstralCharacterSkillSet>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("character_skill_sets"));
    public static final DeferredRegister<AstralCharacterSkillSet> SKILL_SETS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<AstralCharacterSkillSet> REGISTRY = SKILL_SETS.makeRegistry(_ -> {});

    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> DEFAULT = register("default", AstralCharacterSkills::useFallbackSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> MIMI = register("mimi", AstralCharacterSkills::useMimiSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> FEN = register("fen", AstralCharacterSkills::useHealSkill, List.of(AstralCharacterSkills::passiveRegenWhenHungry));
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> DOROTHY = register("dorothy", AstralCharacterSkills::useHealSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> LULU = register("lulu", AstralCharacterSkills::useHealSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> AME = register("ame", AstralCharacterSkills::useHealSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> NARDIS = register("nardis", AstralCharacterSkills::useNardisSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> PANDAMAN = register("pandaman", AstralCharacterSkills::useFoodSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> PADMAN = register("padman", AstralCharacterSkills::useFoodSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> JILL = register("jill", AstralCharacterSkills::useDrawOneSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> MEGAS = register("megas", AstralCharacterSkills::useDrawOneSkill);
    public static final DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> Z3000 = register("z3000", AstralCharacterSkills::useArmorPulseSkill);

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill) {
        return register(characterPath, activeSkill, List.of());
    }

    public static DeferredHolder<AstralCharacterSkillSet, AstralCharacterSkillSet> register(String characterPath, AstralCharacterActiveSkill activeSkill, List<AstralCharacterPassiveSkill> passiveSkills) {
        return SKILL_SETS.register(characterPath, () -> new AstralCharacterSkillSet(AstralCraft.prefix(characterPath), activeSkill, passiveSkills, "skill"));
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

    public static Collection<DeferredHolder<AstralCharacterSkillSet, ? extends AstralCharacterSkillSet>> allHolders() {
        return SKILL_SETS.getEntries();
    }

    protected static boolean useMimiSkill(CharacterSkillContext context) {
        int cleared = AstralHandCardManager.clear(context.player());
        AstralHandCardManager.addRandomEffectCards(context.player(), cleared + 1);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.mimi", cleared + 1), true);
        return true;
    }

    protected static boolean useHealSkill(CharacterSkillContext context) {
        float amount = Math.clamp(context.state().friendship(), 2.0F, 6.0F);
        context.player().heal(amount);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.heal", (int) amount), true);
        return true;
    }

    protected static boolean useNardisSkill(CharacterSkillContext context) {
        AstralHandCardManager.addRandomEffectCards(context.player(), 3);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.draw", 3), true);
        return true;
    }

    protected static boolean useFoodSkill(CharacterSkillContext context) {
        List<Identifier> foods = List.of(AstralCraft.prefix("handcard_hamburger"), AstralCraft.prefix("handcard_chocolate_cake"));
        Identifier card = foods.get(context.player().getRandom().nextInt(foods.size()));
        AstralHandCardManager.add(context.player(), card, 1);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.food"), true);
        return true;
    }

    protected static boolean useDrawOneSkill(CharacterSkillContext context) {
        AstralHandCardManager.addRandomEffectCards(context.player(), 1);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.draw", 1), true);
        return true;
    }

    protected static boolean useArmorPulseSkill(CharacterSkillContext context) {
        context.player().setAbsorptionAmount(Math.max(context.player().getAbsorptionAmount(), 4.0F));
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.shield", 4), true);
        return true;
    }

    protected static boolean useFallbackSkill(CharacterSkillContext context) {
        context.player().heal(1.0F);
        AstralHandCardManager.addRandomEffectCards(context.player(), 1);
        context.player().sendSystemMessage(Component.translatable("message.astral_craft.skill.fallback"), true);
        return true;
    }

    protected static void passiveRegenWhenHungry(CharacterSkillContext context) {
        if (context.player().tickCount % 200 != 0) return;
        if (context.player().getHealth() >= context.player().getMaxHealth()) return;
        if (context.player().getFoodData().getFoodLevel() < 16) return;
        context.player().heal(0.5F);
    }

}
