package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.gameplay.event.effects.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class AstralEventEffectTypes {

    public static final ResourceKey<Registry<MapCodec<? extends AstralEventEffect>>> REGISTRY_KEY = ResourceKey.createRegistryKey(AstralCraft.prefix("astral_event_effect_types"));
    public static final DeferredRegister<MapCodec<? extends AstralEventEffect>> EFFECT_TYPES = DeferredRegister.create(REGISTRY_KEY, AstralCraft.MOD_ID);
    public static final Registry<MapCodec<? extends AstralEventEffect>> REGISTRY = EFFECT_TYPES.makeRegistry(_ -> {});

    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<NoopEventEffect>> NOOP = register("noop", NoopEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<ChanceEventEffect>> CHANCE = register("chance", ChanceEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<ConditionalEventEffect>> CONDITIONAL = register("conditional", ConditionalEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<RepeatEventEffect>> REPEAT = register("repeat", RepeatEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<RandomChoiceEventEffect>> RANDOM_CHOICE = register("random_choice", RandomChoiceEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<ForNearbyEntitiesEventEffect>> FOR_NEARBY_ENTITIES = register("for_nearby_entities", ForNearbyEntitiesEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<GiveItemEventEffect>> GIVE_ITEM = register("give_item", GiveItemEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<DropItemEventEffect>> DROP_ITEM = register("drop_item", DropItemEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<SummonEntityEventEffect>> SUMMON_ENTITY = register("summon_entity", SummonEntityEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<MobEffectEventEffect>> MOB_EFFECT = register("effect", MobEffectEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<ClearMobEffectEventEffect>> CLEAR_MOB_EFFECT = register("clear_effect", ClearMobEffectEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<DamageEventEffect>> DAMAGE = register("damage", DamageEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<HealEventEffect>> HEAL = register("heal", HealEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<AddExperienceEventEffect>> ADD_EXPERIENCE = register("add_experience", AddExperienceEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<AddHungerEventEffect>> ADD_HUNGER = register("add_hunger", AddHungerEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<SetFireEventEffect>> SET_FIRE = register("set_fire", SetFireEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<CommandEventEffect>> COMMAND = register("command", CommandEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<GainSelectedCharacterFriendshipEventEffect>> GAIN_SELECTED_CHARACTER_FRIENDSHIP = register("gain_selected_character_friendship", GainSelectedCharacterFriendshipEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardForEachParticipantEventEffect>> BOARD_FOR_EACH_PARTICIPANT = register("board_for_each_participant", BoardForEachParticipantEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardCoinEventEffect>> BOARD_COINS = register("board_coins", BoardCoinEventEffect.CODEC);
//    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardDamageEventEffect>> BOARD_DAMAGE = register("board_damage", BoardDamageEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardMoveDiceEventEffect>> BOARD_MOVE_DICE = register("board_move_dice", BoardMoveDiceEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardStatusEventEffect>> BOARD_STATUS = register("board_status", BoardStatusEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardRoundStatusEventEffect>> BOARD_ROUND_STATUS = register("board_round_status", BoardRoundStatusEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardSetHealthEventEffect>> BOARD_SET_HEALTH = register("board_set_health", BoardSetHealthEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardHandEventEffect>> BOARD_HAND = register("board_hand", BoardHandEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardTrapEventEffect>> BOARD_TRAP = register("board_trap", BoardTrapEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardBalanceCoinsEventEffect>> BOARD_BALANCE_COINS = register("board_balance_coins", BoardBalanceCoinsEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardTransferHandsEventEffect>> BOARD_TRANSFER_HANDS = register("board_transfer_hands", BoardTransferHandsEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardActivateRoundEventEffect>> BOARD_ACTIVATE_ROUND_EVENT = register("board_activate_round_event", BoardActivateRoundEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardTeleportParticipantsEventEffect>> BOARD_TELEPORT_PARTICIPANTS = register("board_teleport_participants", BoardTeleportParticipantsEventEffect.CODEC);
    public static final DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<BoardSharedLotteryEventEffect>> BOARD_SHARED_LOTTERY = register("board_shared_lottery", BoardSharedLotteryEventEffect.CODEC);

    public static <T extends AstralEventEffect> DeferredHolder<MapCodec<? extends AstralEventEffect>, MapCodec<T>> register(String path, MapCodec<T> codec) {
        return EFFECT_TYPES.register(path, () -> codec);
    }

    public static DeferredRegister<MapCodec<? extends AstralEventEffect>> createRegister(String modId) {
        return DeferredRegister.create(REGISTRY_KEY, modId);
    }

    public static Codec<? extends AstralEventEffect> codecFor(String rawId) {
        Identifier id = parse(rawId);
        MapCodec<? extends AstralEventEffect> codec = REGISTRY.getValue(id);
        if (codec == null) {
            codec = NoopEventEffect.CODEC;
        }
        return codec.codec();
    }

    public static Identifier parse(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return AstralCraft.prefix("noop");
        }

        try {
            return rawId.contains(":") ? Identifier.parse(rawId) : AstralCraft.prefix(rawId);
        } catch (Exception ignored) {
            return AstralCraft.prefix("noop");
        }
    }

    public static String id(Identifier id) {
        return id.toString();
    }

}