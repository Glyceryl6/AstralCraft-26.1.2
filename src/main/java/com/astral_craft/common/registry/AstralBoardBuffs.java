package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.type.CounterBoardBuff;
import com.astral_craft.common.gameplay.buff.type.LeakingPocketBoardBuff;
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

    // Reusable attribute buffs. Specific cards and characters can reuse these instead of creating one-off logic.
    public static final DeferredHolder<BoardBuff, BoardBuff> ATTACK_UP = register("attack_up", BoardBuff.Properties.of(0xFFE65050).stacking().attack(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> ATTACK_DOWN = register("attack_down", BoardBuff.Properties.of(0xFF8A4C61).stacking().attack(-1));
    public static final DeferredHolder<BoardBuff, BoardBuff> DEFENSE_UP = register("defense_up", BoardBuff.Properties.of(0xFF4A8FE7).stacking().defense(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> DEFENSE_DOWN = register("defense_down", BoardBuff.Properties.of(0xFF4D5D82).stacking().defense(-1));
    public static final DeferredHolder<BoardBuff, BoardBuff> DAMAGE_TAKEN_UP = register("damage_taken_up", BoardBuff.Properties.of(0xFFE67A50).stacking().incomingDamage(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> DAMAGE_TAKEN_DOWN = register("damage_taken_down", BoardBuff.Properties.of(0xFF6CC7B5).stacking().damageReduction(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> SPEED_UP = register("speed_up", BoardBuff.Properties.of(0xFF63D5E8).stacking().speed(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> SPEED_DOWN = register("speed_down", BoardBuff.Properties.of(0xFF8293A5).stacking().speed(-1));
    public static final DeferredHolder<BoardBuff, BoardBuff> HEAL = register("heal", BoardBuff.Properties.of(0xFF72D572).stacking().permanent().healAtTurnStart(1).halveLevelsAtTurnEnd());
    public static final DeferredHolder<BoardBuff, BoardBuff> FIGHT_FIRE_WITH_FIRE = register("fight_fire_with_fire", BoardBuff.Properties.of(0xFF72D572).healAtTurnStart(3));
    public static final DeferredHolder<BoardBuff, BoardBuff> STARLIGHT = register("starlight", BoardBuff.Properties.of(0xFFFFE66D).stacking().permanent().roundReward(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> MARK = register("mark", BoardBuff.Properties.of(0xFFFF6B6B).stacking().permanent().incomingDamageFlat(1).decayLevelsAtTurnEnd());
    public static final DeferredHolder<BoardBuff, BoardBuff> BERSERK = register("berserk", BoardBuff.Properties.of(0xFFE34B4B).stacking().attack(3).incomingDamage(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> POISON = register("poison", BoardBuff.Properties.of(0xFF72B04A).damageAtTurnStart(2));
    public static final DeferredHolder<BoardBuff, BoardBuff> CURSE = register("curse", BoardBuff.Properties.of(0xFF8C65B8));
    public static final DeferredHolder<BoardBuff, BoardBuff> STUN = register("stun", BoardBuff.Properties.of(0xFFFFC857));
    public static final DeferredHolder<BoardBuff, BoardBuff> COUNTER = register("counter", new CounterBoardBuff(0xFF6CA0DC));
    public static final DeferredHolder<BoardBuff, BoardBuff> OVERCLOCK = register("overclock", BoardBuff.Properties.of(0xFF59D6D6).stacking().attack(2).speed(2));
    public static final DeferredHolder<BoardBuff, BoardBuff> PROBLEM_STUDENT = register("problem_student", BoardBuff.Properties.of(0xFFE4A04E));
    public static final DeferredHolder<BoardBuff, BoardBuff> AWAKENING = register("awakening", BoardBuff.Properties.of(0xFFFF8C42).stacking().permanent());
    public static final DeferredHolder<BoardBuff, BoardBuff> SNOWBALL_SLOW = register("snowball_slow", BoardBuff.Properties.of(0xFFB9E8FF).stacking().speed(-4));
    public static final DeferredHolder<BoardBuff, BoardBuff> DIRECTED_BOOST = register("directed_boost", BoardBuff.Properties.of(0xFF76E6FF).stacking().speed(3));
    public static final DeferredHolder<BoardBuff, BoardBuff> ENHANCED_BARRICADE_BOOST = register("enhanced_barricade_boost", BoardBuff.Properties.of(0xFFFFC65C).stacking().speed(2));
    public static final DeferredHolder<BoardBuff, BoardBuff> KING_POWER = register("king_power", BoardBuff.Properties.of(0xFFB24444).stacking().attack(5));
    public static final DeferredHolder<BoardBuff, BoardBuff> IMMOVABLE = register("immovable", BoardBuff.Properties.of(0xFF5D8ED6).stacking().defense(6));
    public static final DeferredHolder<BoardBuff, BoardBuff> CHARGE = register("charge", BoardBuff.Properties.of(0xFFFFB347).stacking().attack(5));
    public static final DeferredHolder<BoardBuff, BoardBuff> AWAKENED_ATTACK = register("awakened_attack", BoardBuff.Properties.of(0xFFFF8C42).stacking().attack(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> DRAGON_ROAR_POWER = register("dragon_roar_power", BoardBuff.Properties.of(0xFFC85BFF).stacking().attack(3));
    public static final DeferredHolder<BoardBuff, BoardBuff> DRAGON_ROAR_WEAKNESS = register("dragon_roar_weakness", BoardBuff.Properties.of(0xFF74509C).stacking().defense(-3).speed(-9));
    public static final DeferredHolder<BoardBuff, BoardBuff> ALL_OR_NOTHING = register("all_or_nothing", BoardBuff.Properties.of(0xFFC81D45).attack(5).preventsEvade().knockDownOwnerWhenAttackFails().consumeAfterAttack());
    public static final DeferredHolder<BoardBuff, BoardBuff> EQUALITY_GUARD = register(EQUALITY_GUARD_ID.getPath(), BoardBuff.Properties.of(0xFFFFF2A6).damageReduction(10).consumeAfterIncomingDamage());
    public static final DeferredHolder<BoardBuff, BoardBuff> LEAKING_POCKET = register(LEAKING_POCKET_ID.getPath(), new LeakingPocketBoardBuff(0xFFD7A867));
    public static final DeferredHolder<BoardBuff, BoardBuff> CUSTOM = register("custom", BoardBuff.Properties.of(0xFFBBBBBB));

    private static DeferredHolder<BoardBuff, BoardBuff> register(String name, BoardBuff.Properties properties) {
        return register(name, new BoardBuff(properties));
    }

    private static DeferredHolder<BoardBuff, BoardBuff> register(String name, BoardBuff buff) {
        return BUFFS.register(name, () -> buff);
    }

}