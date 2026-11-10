package com.axymod.client.mixin;

import com.axymod.client.AxyConfig;
import com.axymod.client.EmoteManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Troca a skin do jogador pela skin Axy do emote atual e força o modelo de braço fino (Alex/slim). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void axy$skin(CallbackInfoReturnable<Identifier> cir) {
        if (AxyConfig.enabled) {
            cir.setReturnValue(EmoteManager.skinFor((AbstractClientPlayerEntity) (Object) this));
        }
    }

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void axy$model(CallbackInfoReturnable<String> cir) {
        if (AxyConfig.enabled) {
            cir.setReturnValue("slim");
        }
    }
}
