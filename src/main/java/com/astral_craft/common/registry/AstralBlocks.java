package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.PlatformBlocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AstralCraft.MOD_ID);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_CANDY_GHOST = BLOCKS.registerBlock("platform_candy_ghost", PlatformBlocks.CandyGhost::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_CARD = BLOCKS.registerBlock("platform_card", PlatformBlocks.Card::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DAMAGE = BLOCKS.registerBlock("platform_damage", PlatformBlocks.Damage::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DESTINY = BLOCKS.registerBlock("platform_destiny", PlatformBlocks.Destiny::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DIVINE = BLOCKS.registerBlock("platform_divine", PlatformBlocks.Divine::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_EVENT = BLOCKS.registerBlock("platform_event", PlatformBlocks.Event::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_FIRE = BLOCKS.registerBlock("platform_fire", PlatformBlocks.Fire::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GAMBLE = BLOCKS.registerBlock("platform_gamble", PlatformBlocks.Gamble::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GIFT = BLOCKS.registerBlock("platform_gift", PlatformBlocks.Gift::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GIMMICK = BLOCKS.registerBlock("platform_gimmick", PlatformBlocks.Gimmick::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GOLD = BLOCKS.registerBlock("platform_gold", PlatformBlocks.Gold::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_HEAL = BLOCKS.registerBlock("platform_heal", PlatformBlocks.Heal::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_HOSPITAL = BLOCKS.registerBlock("platform_hospital", PlatformBlocks.Hospital::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_JUMP = BLOCKS.registerBlock("platform_jump", PlatformBlocks.Jump::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_LOTTERY = BLOCKS.registerBlock("platform_lottery", PlatformBlocks.Lottery::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_MONSTER = BLOCKS.registerBlock("platform_monster", PlatformBlocks.Monster::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_MOVE_AGAIN = BLOCKS.registerBlock("platform_move_again", PlatformBlocks.MoveAgain::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_RELIC = BLOCKS.registerBlock("platform_relic", PlatformBlocks.Relic::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_SHOP = BLOCKS.registerBlock("platform_shop", PlatformBlocks.Shop::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_START = BLOCKS.registerBlock("platform_start", PlatformBlocks.Start::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_TELEPORT = BLOCKS.registerBlock("platform_teleport", PlatformBlocks.Teleport::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_TELEPORT_POINT = BLOCKS.registerBlock("platform_teleport_point", PlatformBlocks.TeleportPoint::new);

}