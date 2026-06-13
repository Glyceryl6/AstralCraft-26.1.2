package com.astral_craft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.data.models.ModelProvider$ItemInfoCollector")
public class MixinItemInfoCollector {

    @Inject(method = "finalizeAndValidate", at = @At(value = "INVOKE", target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V"), cancellable = true)
    public void finalizeAndValidate(CallbackInfo ci) {
        ci.cancel();
    }

}