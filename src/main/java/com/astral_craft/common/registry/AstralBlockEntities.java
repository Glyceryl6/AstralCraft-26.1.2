package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blockentity.PlatformBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AstralBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AstralCraft.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlatformBlockEntity>> PLATFORM = BLOCK_ENTITIES.register(
            "platform", () -> new BlockEntityType<>(PlatformBlockEntity::new, false, platformBlocks()));

    private static Block[] platformBlocks() {
        return new Block[]{
                AstralBlocks.PLATFORM_CANDY_GHOST.get(), AstralBlocks.PLATFORM_CARD.get(),
                AstralBlocks.PLATFORM_DAMAGE.get(), AstralBlocks.PLATFORM_DESTINY.get(),
                AstralBlocks.PLATFORM_DIVINE.get(), AstralBlocks.PLATFORM_EVENT.get(),
                AstralBlocks.PLATFORM_FIRE.get(), AstralBlocks.PLATFORM_GAMBLE.get(),
                AstralBlocks.PLATFORM_GIFT.get(), AstralBlocks.PLATFORM_GIMMICK.get(),
                AstralBlocks.PLATFORM_GOLD.get(), AstralBlocks.PLATFORM_HEAL.get(),
                AstralBlocks.PLATFORM_HOSPITAL.get(), AstralBlocks.PLATFORM_JUMP.get(),
                AstralBlocks.PLATFORM_LOTTERY.get(), AstralBlocks.PLATFORM_MONSTER.get(),
                AstralBlocks.PLATFORM_MOVE_AGAIN.get(), AstralBlocks.PLATFORM_RELIC.get(),
                AstralBlocks.PLATFORM_SHOP.get(), AstralBlocks.PLATFORM_START.get(),
                AstralBlocks.PLATFORM_CHECK_POINT.get(), AstralBlocks.PLATFORM_TELEPORT.get(),
                AstralBlocks.PLATFORM_TELEPORT_POINT.get()
        };
    }
}
