package com.axymod.client.mixin;

import com.axymod.client.AxyAnimator;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Depois do vanilla posicionar o modelo: aplica proporções e animações do Axy. */
@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void axy$afterSetAngles(LivingEntity entity, float limbAngle, float limbDistance,
                                    float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        AbstractClientPlayerEntity player = entity instanceof AbstractClientPlayerEntity p ? p : null;
        AxyAnimator.apply((PlayerEntityModel<?>) (Object) this, player, animationProgress);
    }

    /** Corrige a posição do item na mão para o braço curto da Axy (se o método existir nesta classe). */
    @Inject(method = "setArmAngle", at = @At("TAIL"), require = 0)
    private void axy$armAngle(Arm arm, MatrixStack matrices, CallbackInfo ci) {
        AxyAnimator.shiftHeldItem((PlayerEntityModel<?>) (Object) this, arm, matrices);
    }
}
