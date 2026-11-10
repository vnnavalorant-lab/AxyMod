package com.axymod.client.mixin;

import com.axymod.client.AxyAnimator;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
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

    /** Mesmo ajuste do item na mão, caso o PlayerEntityModel use o setArmAngle herdado. */
    @Inject(method = "setArmAngle", at = @At("TAIL"), require = 0)
    private void axy$armAngle(Arm arm, MatrixStack matrices, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntityModel<?> player) {
            AxyAnimator.shiftHeldItem(player, arm, matrices);
        }
    }
}
