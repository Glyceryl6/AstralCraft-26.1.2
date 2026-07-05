package com.astral_craft.mixin;

import com.astral_craft.common.gameplay.handcard.CardUseService;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void astralCraft$suppressHandCardDeckSwing(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (CardUseService.shouldSuppressDeckSwing(this.player)) {
            ci.cancel();
        }
    }

}