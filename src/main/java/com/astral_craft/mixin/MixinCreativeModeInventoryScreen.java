package com.astral_craft.mixin;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralTabs;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class MixinCreativeModeInventoryScreen {

    @Inject(method = "extractTabButton", at = @At(value = "TAIL"), cancellable = true)
    protected void extractTabButton(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, CreativeModeTab tab, CallbackInfo ci,
            @Local(name = "iconX") int iconX, @Local(name = "iconY") int iconY) {
        if (tab == AstralTabs.NORMAL_TAB.get()) {
            final int size = 16;
            final Identifier location = AstralCraft.prefix("textures/gui/astral_craft_logo.png");
            graphics.blit(RenderPipelines.GUI_TEXTURED, location, iconX, iconY, 0.0F, 0.0F, size, size, size, size);
            ci.cancel();
        }
    }

}