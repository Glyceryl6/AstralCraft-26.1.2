package com.astral_craft.mixin;

import com.astral_craft.client.gui.phrase.QuickPhraseSidebar;
import com.astral_craft.client.gui.phrase.QuickPhraseSidebarHost;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends Screen implements QuickPhraseSidebarHost {

    @Unique
    private final QuickPhraseSidebar astralCraft$quickPhraseSidebar = new QuickPhraseSidebar();

    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Override
    public QuickPhraseSidebar astralCraft$getQuickPhraseSidebar() {
        return this.astralCraft$quickPhraseSidebar;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void astralCraft$renderQuickPhrases(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        this.astralCraft$quickPhraseSidebar.render(graphics, this.font, this.width, this.height, mouseX, mouseY);
    }

}