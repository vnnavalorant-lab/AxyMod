package com.axymod.client.mixin;

import com.axymod.client.AxyAnimator;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Faz a armadura acompanhar a escala do corpo do jogador. */
@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin {
    @Inject(method = "copyBipedStateTo", at = @At("TAIL"))
    private void axy$copyScale(BipedEntityModel<?> model, CallbackInfo ci) {
        AxyAnimator.copyScales((BipedEntityModel<?>) (Object) this, model);
    }
}
