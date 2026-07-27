package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.gameplay.buff.type.AttributeBoardBuff;
import com.astral_craft.common.gameplay.buff.type.EqualityGuardBoardBuff;
import com.astral_craft.common.gameplay.buff.type.HasteBoardBuff;
import com.astral_craft.common.gameplay.buff.type.LeakingPocketBoardBuff;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    public static final Identifier EQUALITY_GUARD_ID = AstralCraft.prefix("equality_guard");
    public static final Identifier LEAKING_POCKET_ID = AstralCraft.prefix("leaking_pocket");

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
            BoardBuff.Properties.of(0xFF72D572).stacking().healAtTurnStart(1).halveLevelsAtTurnEnd());
    public static final DeferredHolder<BoardBuff, BoardBuff> STARLIGHT = register(STARLIGHT_ID.getPath(),
            BoardBuff.Properties.of(0xFFFFE66D).stacking().permanent().roundReward(1));
    public static final DeferredHolder<BoardBuff, BoardBuff> MARK = register(MARK_ID.getPath(),
            BoardBuff.Properties.of(0xFFFF6B6B).stacking().flatIncomingDamage(1).decayLevelsAtTurnEnd());
    public static final DeferredHolder<BoardBuff, BoardBuff> COUNTER = register(COUNTER_ID.getPath(), BoardBuff.Properties.of(0xFF6CA0DC).stacking());
    public static final DeferredHolder<BoardBuff, BoardBuff> HASTE = register(HASTE_ID.getPath(), new HasteBoardBuff(0xFF66D9FF));
    public static final DeferredHolder<BoardBuff, BoardBuff> SOUL_LINK = register(SOUL_LINK_ID.getPath(), new BoardBuff(BoardBuff.Properties.of(0xFFD579FF)));
    public static final DeferredHolder<BoardBuff, BoardBuff> EQUALITY_GUARD = register(EQUALITY_GUARD_ID.getPath(), new EqualityGuardBoardBuff(0xFFFFF2A6));
    public static final DeferredHolder<BoardBuff, BoardBuff> LEAKING_POCKET = register(LEAKING_POCKET_ID.getPath(), new LeakingPocketBoardBuff(0xFFD7A867));

    public static BoardBuffInstance.Builder instance(Identifier id, BoardBuff buff) {
        return BoardBuffInstance.builder(id, buff).presentation(displayName(id), icon(id));
    }

    public static BoardBuffInstance.Builder attack(Identifier id, int value) {
        return instance(part(id, "attack"), ATTACK.get()).value(value).presentation(displayName(id), icon(id));
    }

    public static BoardBuffInstance.Builder defense(Identifier id, int value) {
        return instance(part(id, "defense"), DEFENSE.get()).value(value).presentation(displayName(id), icon(id));
    }

    public static BoardBuffInstance.Builder incomingDamage(Identifier id, int value) {
        return instance(part(id, "incoming_damage"), INCOMING_DAMAGE.get()).value(value).presentation(displayName(id), icon(id));
    }

    public static BoardBuffInstance.Builder speed(Identifier id, int value) {
        return instance(part(id, "speed"), SPEED.get()).value(value).presentation(displayName(id), icon(id));
    }

    public static BoardBuffInstance.Builder state(Identifier id) {
        return instance(id, STATE.get());
    }

    public static BoardBuffInstance.Builder turnStartHeal(Identifier id, int value) {
        return instance(id, TURN_START_HEAL.get()).value(value);
    }

    public static BoardBuffInstance.Builder turnStartDamage(Identifier id, int value) {
        return instance(id, TURN_START_DAMAGE.get()).value(value);
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

    public static BoardBuffInstance legacyInstance(Identifier id, int duration, int amplifier, int intrinsicLevels, boolean fresh) {
        BoardBuff buff = REGISTRY.getValue(id);
        if (buff == null) return null;
        return instance(id, buff).duration(duration).amplifier(amplifier).intrinsicLevels(intrinsicLevels).fresh(fresh).build();
    }

    private static DeferredHolder<BoardBuff, BoardBuff> register(String name, BoardBuff.Properties properties) {
        return register(name, new BoardBuff(properties));
    }

    private static DeferredHolder<BoardBuff, BoardBuff> register(String name, BoardBuff buff) {
        return BUFFS.register(name, () -> buff);
    }

}