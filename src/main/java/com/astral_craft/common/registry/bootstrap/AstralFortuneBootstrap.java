package com.astral_craft.common.registry.bootstrap;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.astral_craft.common.gameplay.event.effects.BoardCoinEventEffect;
import com.astral_craft.common.gameplay.event.effects.BoardDropCoinsEventEffect;
import com.astral_craft.common.gameplay.event.effects.BoardHandEventEffect;
import com.astral_craft.common.gameplay.event.effects.BoardScaleCoinsEventEffect;
import com.astral_craft.common.gameplay.event.effects.BoardStatusEventEffect;
import com.astral_craft.common.gameplay.event.effects.DamageEventEffect;
import com.astral_craft.common.gameplay.event.effects.HealEventEffect;
import com.astral_craft.common.gameplay.fortune.BoardFortuneCategory;
import com.astral_craft.common.gameplay.fortune.BoardFortuneDefinition;
import com.astral_craft.common.registry.AstralBoardBuffs;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public class AstralFortuneBootstrap {

    public static final ResourceKey<Registry<BoardFortuneDefinition>> FORTUNES =
            ResourceKey.createRegistryKey(AstralCraft.prefix("fortunes"));
    public static final ResourceKey<BoardFortuneDefinition> SLEEP_IN = key("sleep_in");
    public static final ResourceKey<BoardFortuneDefinition> LUCKY_GUARD = key("lucky_guard");
    public static final ResourceKey<BoardFortuneDefinition> SHOP_GUEST = key("shop_guest");
    public static final ResourceKey<BoardFortuneDefinition> WALLET_FOUND = key("wallet_found");
    public static final ResourceKey<BoardFortuneDefinition> ANKLE_INJURY = key("ankle_injury");
    public static final ResourceKey<BoardFortuneDefinition> WEAK_RESISTANCE = key("weak_resistance");
    public static final ResourceKey<BoardFortuneDefinition> PUNISHMENT = key("punishment");
    public static final ResourceKey<BoardFortuneDefinition> WALLET_LOST = key("wallet_lost");
    public static final ResourceKey<BoardFortuneDefinition> STOCK_SURGE = key("stock_surge");
    public static final ResourceKey<BoardFortuneDefinition> JACKPOT = key("jackpot");
    public static final ResourceKey<BoardFortuneDefinition> TYPHOON = key("typhoon");
    public static final ResourceKey<BoardFortuneDefinition> ALIEN_RAID = key("alien_raid");
    private static final Identifier LUCKY_GUARD_BUFF = AstralCraft.prefix("fortune/lucky_guard");
    private static final Identifier WEAK_RESISTANCE_BUFF = AstralCraft.prefix("fortune/weak_resistance");

    public static void bootstrap(BootstrapContext<BoardFortuneDefinition> context) {
        context.register(SLEEP_IN, fortune("sleep_in", BoardFortuneCategory.GOOD_LUCK, new HealEventEffect(2.0F)));
        context.register(LUCKY_GUARD, fortune("lucky_guard", BoardFortuneCategory.GOOD_LUCK, new BoardStatusEventEffect(
                AstralBoardBuffs.incomingDamage(LUCKY_GUARD_BUFF, -2).permanent().consumeAfterIncomingDamage().build())));
        context.register(SHOP_GUEST, fortune("shop_guest", BoardFortuneCategory.GOOD_LUCK,
                new BoardHandEventEffect(BoardHandEventEffect.Action.GIVE_RANDOM, 1)));
        context.register(WALLET_FOUND, fortune("wallet_found", BoardFortuneCategory.GOOD_LUCK, new BoardCoinEventEffect(5)));
        context.register(ANKLE_INJURY, fortune("ankle_injury", BoardFortuneCategory.BAD_LUCK, new DamageEventEffect(2.0F)));
        context.register(WEAK_RESISTANCE, fortune("weak_resistance", BoardFortuneCategory.BAD_LUCK, new BoardStatusEventEffect(
                AstralBoardBuffs.incomingDamage(WEAK_RESISTANCE_BUFF, 2).permanent().consumeAfterIncomingDamage().build())));
        context.register(PUNISHMENT, fortune("punishment", BoardFortuneCategory.BAD_LUCK,
                new BoardHandEventEffect(BoardHandEventEffect.Action.DISCARD_RANDOM, 1)));
        context.register(WALLET_LOST, fortune("wallet_lost", BoardFortuneCategory.BAD_LUCK, new BoardDropCoinsEventEffect(5)));
        context.register(STOCK_SURGE, fortune("stock_surge", BoardFortuneCategory.GOOD_LUCK, new BoardScaleCoinsEventEffect(2.0F)));
        context.register(JACKPOT, fortune("jackpot", BoardFortuneCategory.FORTUNE,
                new BoardHandEventEffect(BoardHandEventEffect.Action.GIVE_RANDOM, 3)));
        context.register(TYPHOON, fortune("typhoon", BoardFortuneCategory.MISFORTUNE,
                new BoardHandEventEffect(BoardHandEventEffect.Action.DISCARD_RANDOM, 3)));
        context.register(ALIEN_RAID, fortune("alien_raid", BoardFortuneCategory.MISFORTUNE,
                new BoardScaleCoinsEventEffect(0.5F)));
    }

    private static BoardFortuneDefinition fortune(String path, BoardFortuneCategory category, AstralEventEffect... effects) {
        return new BoardFortuneDefinition(AstralCraft.prefix(path), "fortune.astral_craft." + category.getSerializedName() + ".name",
                "fortune.astral_craft." + path + ".description",
                AstralCraft.prefix("textures/gui/cards/fortune/" + path + ".png"), category, 1, List.of(effects));
    }

    private static ResourceKey<BoardFortuneDefinition> key(String path) {
        return ResourceKey.create(FORTUNES, AstralCraft.prefix(path));
    }

}
