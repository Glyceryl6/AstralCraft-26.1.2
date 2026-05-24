package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AstralCraft.MOD_ID);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_CANDY_GHOST = BLOCKS.registerBlock("platform_candy_ghost", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_CARD = BLOCKS.registerBlock("platform_card", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DAMAGE = BLOCKS.registerBlock("platform_damage", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DESTINY = BLOCKS.registerBlock("platform_destiny", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_DIVINE = BLOCKS.registerBlock("platform_divine", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_EVENT = BLOCKS.registerBlock("platform_event", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_FIRE = BLOCKS.registerBlock("platform_fire", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GAMBLE = BLOCKS.registerBlock("platform_gamble", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GIFT = BLOCKS.registerBlock("platform_gift", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GIMMICK = BLOCKS.registerBlock("platform_gimmick", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_GOLD = BLOCKS.registerBlock("platform_gold", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_HEAL = BLOCKS.registerBlock("platform_heal", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_HOSPITAL = BLOCKS.registerBlock("platform_hospital", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_JUMP = BLOCKS.registerBlock("platform_jump", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_LOTTERY = BLOCKS.registerBlock("platform_lottery", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_MONSTER = BLOCKS.registerBlock("platform_monster", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_MOVE_AGAIN = BLOCKS.registerBlock("platform_move_again", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_RELIC = BLOCKS.registerBlock("platform_relic", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_SHOP = BLOCKS.registerBlock("platform_shop", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_START = BLOCKS.registerBlock("platform_start", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_TELEPORT = BLOCKS.registerBlock("platform_teleport", BasePlatform::new);
    public static final DeferredHolder<Block, ? extends Block> PLATFORM_TELEPORT_POINT = BLOCKS.registerBlock("platform_teleport_point", BasePlatform::new);

}