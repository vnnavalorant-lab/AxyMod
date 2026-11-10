package com.axymod.client.mixin;

import com.axymod.client.AxyRenderState;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Evita que a escala não-uniforme do braço achate o item segurado. */
@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void axy$itemStart(CallbackInfo ci) {
        AxyRenderState.suppressScale = true;
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void axy$itemEnd(CallbackInfo ci) {
        AxyRenderState.suppressScale = false;
    }
}
