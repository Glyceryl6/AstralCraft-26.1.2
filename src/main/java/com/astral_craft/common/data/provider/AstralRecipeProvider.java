package com.astral_craft.common.data.provider;

import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class AstralRecipeProvider extends RecipeProvider {

    protected AstralRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.TOOLS,
                        AstralItems.BOARD_PROJECTOR.get())
                .pattern("GAG")
                .pattern("RMR")
                .pattern("IRI")
                .define('G', Items.GLASS)
                .define('A', Items.AMETHYST_SHARD)
                .define('R', Items.REDSTONE)
                .define('M', Items.MAP)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_redstone", this.has(Items.REDSTONE))
                .save(this.output);

        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.TOOLS,
                        AstralItems.BOARD_SPECTATOR.get())
                .pattern(" GE")
                .pattern(" SG")
                .pattern("P  ")
                .define('G', Items.GLASS)
                .define('E', Items.ENDER_EYE)
                .define('S', Items.SPYGLASS)
                .define('P', Items.PAPER)
                .unlockedBy("has_spyglass", this.has(Items.SPYGLASS))
                .save(this.output);

        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.TOOLS,
                        AstralItems.BOARD_LOBBY.get())
                .pattern(" E ")
                .pattern("BCB")
                .pattern(" R ")
                .define('E', Items.EMERALD)
                .define('B', Items.BELL)
                .define('C', Items.COMPASS)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_compass", this.has(Items.COMPASS))
                .save(this.output);

        ShapedRecipeBuilder.shaped(this.registries.lookupOrThrow(Registries.ITEM), RecipeCategory.TOOLS,
                        AstralItems.BOARD_DISMANTLER.get())
                .pattern("ISI")
                .pattern("IRI")
                .pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.SHEARS)
                .define('R', Items.REDSTONE_TORCH)
                .unlockedBy("has_shears", this.has(Items.SHEARS))
                .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new AstralRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Astral Recipes";
        }

    }

}