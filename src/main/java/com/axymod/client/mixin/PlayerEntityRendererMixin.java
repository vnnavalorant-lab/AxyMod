package com.axymod.client.mixin;

import com.axymod.client.AxyRenderState;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marca quando o braço de primeira pessoa está sendo desenhado. */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("HEAD"))
    private void axy$fpStart(CallbackInfo ci) {
        AxyRenderState.firstPersonArm = true;
    }

    @Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("RETURN"))
    private void axy$fpEnd(CallbackInfo ci) {
        AxyRenderState.firstPersonArm = false;
    }
}
