package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.platform.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AstralCraft.MOD_ID);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_CANDY_GHOST = BLOCKS.registerBlock("platform_candy_ghost", CandyGhostPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_CARD = BLOCKS.registerBlock("platform_card", CardPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DAMAGE = BLOCKS.registerBlock("platform_damage", DamagePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DESTINY = BLOCKS.registerBlock("platform_destiny", DestinyPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DIVINE = BLOCKS.registerBlock("platform_divine", DivinePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_EVENT = BLOCKS.registerBlock("platform_event", EventPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_FIRE = BLOCKS.registerBlock("platform_fire", FirePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GAMBLE = BLOCKS.registerBlock("platform_gamble", GamblePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GIFT = BLOCKS.registerBlock("platform_gift", GiftPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GIMMICK = BLOCKS.registerBlock("platform_gimmick", GimmickPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GOLD = BLOCKS.registerBlock("platform_gold", GoldPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_HEAL = BLOCKS.registerBlock("platform_heal", HealPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_HOSPITAL = BLOCKS.registerBlock("platform_hospital", HospitalPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_JUMP = BLOCKS.registerBlock("platform_jump", JumpPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_LOTTERY = BLOCKS.registerBlock("platform_lottery", LotteryPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_MONSTER = BLOCKS.registerBlock("platform_monster", MonsterPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_MOVE_AGAIN = BLOCKS.registerBlock("platform_move_again", MoveAgainPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_RELIC = BLOCKS.registerBlock("platform_relic", RelicPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_SHOP = BLOCKS.registerBlock("platform_shop", ShopPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_START = BLOCKS.registerBlock("platform_start", StartPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_CHECK_POINT = BLOCKS.registerBlock("platform_check_point", CheckPointPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_TELEPORT = BLOCKS.registerBlock("platform_teleport", TeleportPlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_TELEPORT_POINT = BLOCKS.registerBlock("platform_teleport_point", TeleportPointPlatform::new);

}