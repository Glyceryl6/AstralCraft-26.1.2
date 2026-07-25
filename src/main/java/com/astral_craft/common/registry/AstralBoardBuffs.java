package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.type.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralBoardBuffs {

    public static final ResourceKey<Registry<BoardBuff>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("board_buffs"));
    public static final DeferredRegister<BoardBuff> BUFFS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Identifier EQUALITY_GUARD_ID = AstralCraft.prefix("equality_guard");
    public static final Identifier LEAKING_POCKET_ID = AstralCraft.prefix("leaking_pocket");
    public static final Registry<BoardBuff> REGISTRY = BUFFS.makeRegistry(builder -> builder.sync(true));

    public static final DeferredHolder<BoardBuff, BoardBuff> HEAL = register("heal", new HalvingHealingBoardBuff(0xFF72D572, 1));
    public static final DeferredHolder<BoardBuff, BoardBuff> FIGHT_FIRE_WITH_FIRE = register("fight_fire_with_fire", new HealingBoardBuff(0xFF72D572, 3));
    public static final DeferredHolder<BoardBuff, BoardBuff> STARLIGHT = register("starlight", new BoardBuff(0xFFFFE66D));
    public static final DeferredHolder<BoardBuff, BoardBuff> MARK = register("mark", new DecayingStackingBoardBuff(0xFFFF6B6B, true, 0, 1));
    public static final DeferredHolder<BoardBuff, BoardBuff> BERSERK = register("berserk", new BerserkBoardBuff(0xFFE34B4B));
    public static final DeferredHolder<BoardBuff, BoardBuff> POISON = register("poison", new DamagingBoardBuff(0xFF72B04A, 2));
    public static final DeferredHolder<BoardBuff, BoardBuff> CURSE = register("curse", new BoardBuff(0xFF8C65B8));
    public static final DeferredHolder<BoardBuff, BoardBuff> STUN = register("stun", new BoardBuff(0xFFFFC857));
    public static final DeferredHolder<BoardBuff, BoardBuff> COUNTER = register("counter", new BoardBuff(0xFF6CA0DC));
    public static final DeferredHolder<BoardBuff, BoardBuff> OVERCLOCK = register("overclock", new StackingAttributeBoardBuff(0xFF59D6D6, 2, 0, 2, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> PROBLEM_STUDENT = register("problem_student", new BoardBuff(0xFFE4A04E));
    public static final DeferredHolder<BoardBuff, BoardBuff> AWAKENING = register("awakening", new AwakeningBoardBuff(0xFFFF8C42));
    public static final DeferredHolder<BoardBuff, BoardBuff> SNOWBALL_SLOW = register("snowball_slow", new StackingAttributeBoardBuff(0xFFB9E8FF, 0, 0, -4, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> DIRECTED_BOOST = register("directed_boost", new StackingAttributeBoardBuff(0xFF76E6FF, 0, 0, 3, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> ENHANCED_BARRICADE_BOOST = register("enhanced_barricade_boost", new StackingAttributeBoardBuff(0xFFFFC65C, 0, 0, 2, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> KING_POWER = register("king_power", new StackingAttributeBoardBuff(0xFFB24444, 5, 0, 0, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> IMMOVABLE = register("immovable", new StackingAttributeBoardBuff(0xFF5D8ED6, 0, 6, 0, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> CHARGE = register("charge", new StackingAttributeBoardBuff(0xFFFFB347, 5, 0, 0, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> AWAKENED_ATTACK = register("awakened_attack", new StackingAttributeBoardBuff(0xFFFF8C42, 1, 0, 0, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> DRAGON_ROAR_POWER = register("dragon_roar_power", new StackingAttributeBoardBuff(0xFFC85BFF, 3, 0, 0, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> DRAGON_ROAR_WEAKNESS = register("dragon_roar_weakness", new StackingAttributeBoardBuff(0xFF74509C, 0, -3, -9, 0));
    public static final DeferredHolder<BoardBuff, BoardBuff> ALL_OR_NOTHING = register("all_or_nothing", new AllOrNothingBoardBuff(0xFFC81D45));
    public static final DeferredHolder<BoardBuff, BoardBuff> EQUALITY_GUARD = register(EQUALITY_GUARD_ID.getPath(), new DamageShieldBoardBuff(0xFFFFF2A6, 10));
    public static final DeferredHolder<BoardBuff, BoardBuff> LEAKING_POCKET = register(LEAKING_POCKET_ID.getPath(), new LeakingPocketBoardBuff(0xFFD7A867));
    public static final DeferredHolder<BoardBuff, BoardBuff> CUSTOM = register("custom", new BoardBuff(0xFFBBBBBB));

    private static DeferredHolder<BoardBuff, BoardBuff> register(String name, BoardBuff buff) {
        return BUFFS.register(name, () -> buff);
    }

}