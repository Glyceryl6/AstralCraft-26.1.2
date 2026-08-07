package com.astral_craft.common.data.provider;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralBlocks;
import com.astral_craft.common.tags.AstralBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class AstralBlockTagsProvider extends BlockTagsProvider {

    public AstralBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, AstralCraft.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(AstralBlockTags.PVP_BOARD_PANELS).add(
                AstralBlocks.PLATFORM_CARD.get(),
                AstralBlocks.PLATFORM_DAMAGE.get(),
                AstralBlocks.PLATFORM_DESTINY.get(),
                AstralBlocks.PLATFORM_DIVINE.get(),
                AstralBlocks.PLATFORM_EVENT.get(),
                AstralBlocks.PLATFORM_FIRE.get(),
                AstralBlocks.PLATFORM_GAMBLE.get(),
                AstralBlocks.PLATFORM_GIMMICK.get(),
                AstralBlocks.PLATFORM_GOLD.get(),
                AstralBlocks.PLATFORM_HOSPITAL.get(),
                AstralBlocks.PLATFORM_JUMP.get(),
                AstralBlocks.PLATFORM_LOTTERY.get(),
                AstralBlocks.PLATFORM_MOVE_AGAIN.get(),
                AstralBlocks.PLATFORM_TELEPORT_POINT.get());
        this.tag(AstralBlockTags.PVE_BOARD_PANELS).add(
                AstralBlocks.PLATFORM_CANDY_GHOST.get(),
                AstralBlocks.PLATFORM_HEAL.get(),
                AstralBlocks.PLATFORM_MONSTER.get(),
                AstralBlocks.PLATFORM_RELIC.get());
    }
}
