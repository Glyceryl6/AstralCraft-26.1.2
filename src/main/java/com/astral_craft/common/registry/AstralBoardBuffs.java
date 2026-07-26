package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.buff.type.AllOrNothingBoardBuff;
import com.astral_craft.common.gameplay.buff.type.AttributeBoardBuff;
import com.astral_craft.common.gameplay.buff.type.CounterBoardBuff;
import com.astral_craft.common.gameplay.buff.type.EqualityGuardBoardBuff;
import com.astral_craft.common.gameplay.buff.type.LeakingPocketBoardBuff;
import com.astral_craft.common.gameplay.buff.type.MarkBoardBuff;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class AstralBoardBuffs {

    public static final ResourceKey<Registry<BoardBuff>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("board_buffs"));
    public static final DeferredRegister<BoardBuff> BUFFS = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<BoardBuff> REGISTRY = BUFFS.makeRegistry(builder -> builder.sync(true));

    public static final Identifier HEAL_ID = AstralCraft.prefix("heal");
    public static final Identifier STARLIGHT_ID = AstralCraft.prefix("starlight");
    public static final Identifier MARK_ID = AstralCraft.prefix("mark");
    public static final Identifier COUNTER_ID = AstralCraft.prefix("counter");
    public static final Identifier HASTE_ID = AstralCraft.prefix("haste");
    public static final Identifier SOUL_LINK_ID = AstralCraft.prefix("soul_link");
    public static final Identifier ALL_OR_NOTHING_ID = AstralCraft.prefix("all_or_nothing");
    public static final Identifier EQUALITY_GUARD_ID = AstralCraft.prefix("equality_guard");
    public static final Identifier LEAKING_POCKET_ID = AstralCraft.prefix("leaking_pocket");

    public static final Identifier BERSERK_ID = AstralCraft.prefix("berserk");
    public static final Identifier FIGHT_FIRE_WITH_FIRE_ID = AstralCraft.prefix("fight_fire_with_fire");
    public static final Identifier OVERCLOCK_ID = AstralCraft.prefix("overclock");
    public static final Identifier PROBLEM_STUDENT_ID = AstralCraft.prefix("problem_student");
    public static final Identifier AWAKENING_ID = AstralCraft.prefix("awakening");
    public static final Identifier SNOWBALL_SLOW_ID = AstralCraft.prefix("snowball_slow");
    public static final Identifier DIRECTED_BOOST_ID = AstralCraft.prefix("directed_boost");
    public static final Identifier ENHANCED_BARRICADE_BOOST_ID = AstralCraft.prefix("enhanced_barricade_boost");
    public static final Identifier KING_POWER_ID = AstralCraft.prefix("king_power");
    public static final Identifier IMMOVABLE_ID = AstralCraft.prefix("immovable");
    public static final Identifier CHARGE_ID = AstralCraft.prefix("charge");
    public static final Identifier AWAKENED_ATTACK_ID = AstralCraft.prefix("awakened_attack");
    public static final Identifier DRAGON_ROAR_POWER_ID = AstralCraft.prefix("dragon_roar_power");
    public static final Identifier DRAGON_ROAR_WEAKNESS_ID = AstralCraft.prefix("dragon_roar_weakness");
    public static final Identifier POISON_ID = AstralCraft.prefix("poison");
    public static final Identifier CURSE_ID = AstralCraft.prefix("curse");
    public static final Identifier STUN_ID = AstralCraft.prefix("stun");
    public static final Identifier CUSTOM_ID = AstralCraft.prefix("custom");

    public static final DeferredHolder<BoardBuff, BoardBuff> ATTACK = register("attack",
            new AttributeBoardBuff(0xFFE65050, AttributeBoardBuff.Attribute.ATTACK));
    public static final DeferredHolder<BoardBuff, BoardBuff> DEFENSE = register("defense",
            new AttributeBoardBuff(0xFF4A8FE7, AttributeBoardBuff.Attribute.DEFENSE));
    public static final DeferredHolder<BoardBuff, BoardBuff> INCOMING_DAMAGE = register("incoming_damage",
            new AttributeBoardBuff(0xFFE67A50, AttributeBoardBuff.Attribute.INCOMING_DAMAGE));
    public static final DeferredHolder<BoardBuff, BoardBuff> SPEED = register("speed",
            new AttributeBoardBuff(0xFF63D5E8, AttributeBoardBuff.Attribute.SPEED));
    public static final DeferredHolder<BoardBuff, BoardBuff> STATE = register("state",
            BoardBuff.Properties.of(0xFFBBBBBB).stacking());
    public static final DeferredHolder<BoardBuff, BoardBuff> TURN_START_HEAL = register("turn_start_heal",
            BoardBuff.Properties.of(0xFF72D572).healAtTurnStart(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> TURN_START_DAMAGE = register("turn_start_damage",
            BoardBuff.Properties.of(0xFF72B04A).damageAtTurnStart(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> HEAL = register(HEAL_ID.getPath(),
            BoardBuff.Properties.of(0xFF72D572).stacking().permanent().healAtTurnStart(1).halveLevelsAtTurnEnd());
    public static final DeferredHolder<BoardBuff, BoardBuff> STARLIGHT = register(STARLIGHT_ID.getPath(),
            BoardBuff.Properties.of(0xFFFFE66D).stacking().permanent().roundReward(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> MARK = register(MARK_ID.getPath(), new MarkBoardBuff(0xFFFF6B6B));
    public static final DeferredHolder<BoardBuff, BoardBuff> COUNTER = register(COUNTER_ID.getPath(), new CounterBoardBuff(0xFF6CA0DC));
    public static final DeferredHolder<BoardBuff, BoardBuff> HASTE = register(HASTE_ID.getPath(),
            BoardBuff.Properties.of(0xFF66D9FF).stacking().permanent().extraMoveDice(1).consumeAfterMoveRoll());
    public static final DeferredHolder<BoardBuff, BoardBuff> SOUL_LINK = register(SOUL_LINK_ID.getPath(), new BoardBuff(0xFFD579FF));
    public static final DeferredHolder<BoardBuff, BoardBuff> ALL_OR_NOTHING = register(ALL_OR_NOTHING_ID.getPath(),
            new AllOrNothingBoardBuff(0xFFC81D45));
    public static final DeferredHolder<BoardBuff, BoardBuff> EQUALITY_GUARD = register(EQUALITY_GUARD_ID.getPath(),
            new EqualityGuardBoardBuff(0xFFFFF2A6));
    public static final DeferredHolder<BoardBuff, BoardBuff> LEAKING_POCKET = register(LEAKING_POCKET_ID.getPath(),
            new LeakingPocketBoardBuff(0xFFD7A867));

    public static BoardBuffInstance.Builder instance(Identifier id, BoardBuff buff) {
        return BoardBuffInstance.builder(id, buff).presentation(displayName(id), icon(id));
    }

    public static BoardBuffInstance.Builder partInstance(Identifier id, String part, BoardBuff buff) {
        return BoardBuffInstance.builder(part(id, part), buff).presentation(displayName(id), icon(id));
    }

    public static Component displayName(Identifier id) {
        return Component.translatable("board_buff." + id.getNamespace() + "." + id.getPath());
    }

    public static Identifier icon(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/gui/board_buff/" + id.getPath() + ".png");
    }

    public static Identifier part(Identifier id, String part) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/" + part);
    }

    public static List<BoardBuffInstance> legacyInstances(Identifier id, int duration, int amplifier,
                                                          int intrinsicLevels, boolean fresh) {
        int level = amplifier + 1;
        List<BoardBuffInstance> result = new ArrayList<>();
        switch (id.getPath()) {
            case "attack_up" -> result.add(legacy(id, ATTACK.get(), duration, level, intrinsicLevels, fresh, 1));
            case "attack_down" -> result.add(legacy(id, ATTACK.get(), duration, level, intrinsicLevels, fresh, -1));
            case "defense_up" -> result.add(legacy(id, DEFENSE.get(), duration, level, intrinsicLevels, fresh, 1));
            case "defense_down" -> result.add(legacy(id, DEFENSE.get(), duration, level, intrinsicLevels, fresh, -1));
            case "damage_taken_up" -> result.add(legacy(id, INCOMING_DAMAGE.get(), duration, level, intrinsicLevels, fresh, 1));
            case "damage_taken_down" -> result.add(legacy(id, INCOMING_DAMAGE.get(), duration, level, intrinsicLevels, fresh, -1));
            case "speed_up" -> result.add(legacy(id, SPEED.get(), duration, level, intrinsicLevels, fresh, 1));
            case "speed_down" -> result.add(legacy(id, SPEED.get(), duration, level, intrinsicLevels, fresh, -1));
            case "berserk" -> {
                result.add(legacy(part(BERSERK_ID, "attack"), ATTACK.get(), duration, level, intrinsicLevels, fresh, 3, BERSERK_ID));
                result.add(legacy(part(BERSERK_ID, "damage"), INCOMING_DAMAGE.get(), duration, level, intrinsicLevels, fresh, 1, BERSERK_ID));
            }
            case "fight_fire_with_fire" -> result.add(legacy(FIGHT_FIRE_WITH_FIRE_ID, TURN_START_HEAL.get(), duration,
                    level, intrinsicLevels, fresh, 3));
            case "overclock" -> {
                result.add(legacy(part(OVERCLOCK_ID, "attack"), ATTACK.get(), duration, level, intrinsicLevels, fresh, 2, OVERCLOCK_ID));
                result.add(legacy(part(OVERCLOCK_ID, "speed"), SPEED.get(), duration, level, intrinsicLevels, fresh, 2, OVERCLOCK_ID));
            }
            case "problem_student" -> result.add(legacy(PROBLEM_STUDENT_ID, STATE.get(), duration, level, intrinsicLevels, fresh, 1));
            case "awakening" -> result.add(legacy(AWAKENING_ID, STATE.get(), duration, level, intrinsicLevels, fresh, 1));
            case "snowball_slow" -> result.add(legacy(SNOWBALL_SLOW_ID, SPEED.get(), duration, level, intrinsicLevels, fresh, -4));
            case "directed_boost" -> result.add(legacy(DIRECTED_BOOST_ID, SPEED.get(), duration, level, intrinsicLevels, fresh, 3));
            case "enhanced_barricade_boost" -> result.add(legacy(ENHANCED_BARRICADE_BOOST_ID, SPEED.get(), duration,
                    level, intrinsicLevels, fresh, 2));
            case "king_power" -> result.add(legacy(KING_POWER_ID, ATTACK.get(), duration, level, intrinsicLevels, fresh, 5));
            case "immovable" -> result.add(legacy(IMMOVABLE_ID, DEFENSE.get(), duration, level, intrinsicLevels, fresh, 6));
            case "charge" -> result.add(legacy(CHARGE_ID, ATTACK.get(), duration, level, intrinsicLevels, fresh, 5));
            case "awakened_attack" -> result.add(legacy(AWAKENED_ATTACK_ID, ATTACK.get(), duration, level,
                    intrinsicLevels, fresh, 1));
            case "dragon_roar_power" -> result.add(legacy(DRAGON_ROAR_POWER_ID, ATTACK.get(), duration, level,
                    intrinsicLevels, fresh, 3));
            case "dragon_roar_weakness" -> {
                result.add(legacy(part(DRAGON_ROAR_WEAKNESS_ID, "defense"), DEFENSE.get(), duration, level,
                        intrinsicLevels, fresh, -3, DRAGON_ROAR_WEAKNESS_ID));
                result.add(legacy(part(DRAGON_ROAR_WEAKNESS_ID, "speed"), SPEED.get(), duration, level,
                        intrinsicLevels, fresh, -9, DRAGON_ROAR_WEAKNESS_ID));
            }
            case "poison" -> result.add(legacy(POISON_ID, TURN_START_DAMAGE.get(), duration, level, intrinsicLevels, fresh, 2));
            case "curse" -> result.add(legacy(CURSE_ID, STATE.get(), duration, level, intrinsicLevels, fresh, 1));
            case "stun" -> result.add(legacy(STUN_ID, STATE.get(), duration, level, intrinsicLevels, fresh, 1));
            case "custom" -> result.add(legacy(CUSTOM_ID, STATE.get(), duration, level, intrinsicLevels, fresh, 1));
            default -> {
                BoardBuff buff = REGISTRY.getValue(id);
                if (buff != null) result.add(legacy(id, buff, duration, level, intrinsicLevels, fresh, 1));
            }
        }
        return result;
    }

    private static BoardBuffInstance legacy(Identifier id, BoardBuff buff, int duration, int level, int intrinsicLevels,
                                            boolean fresh, int value) {
        return legacy(id, buff, duration, level, intrinsicLevels, fresh, value, id);
    }

    private static BoardBuffInstance legacy(Identifier id, BoardBuff buff, int duration, int level, int intrinsicLevels,
                                            boolean fresh, int value, Identifier presentationId) {
        return instance(id, buff).duration(duration).level(level).intrinsicLevels(intrinsicLevels).fresh(fresh)
                .value(value).presentation(displayName(presentationId), icon(presentationId)).build();
    }

    private static DeferredHolder<BoardBuff, BoardBuff> register(String name, BoardBuff.Properties properties) {
        return register(name, new BoardBuff(properties));
    }

    private static DeferredHolder<BoardBuff, BoardBuff> register(String name, BoardBuff buff) {
        return BUFFS.register(name, () -> buff);
    }
}
