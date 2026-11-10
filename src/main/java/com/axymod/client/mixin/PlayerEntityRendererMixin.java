package com.axymod.client.mixin;

import com.axymod.client.AxyAnimator;
import com.axymod.client.AxyRenderState;
import com.axymod.client.MorphManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /** O vanilla abaixa o modelo 2px ao agachar; com as pernas curtas da Axy isso as enterrava no chão. */
    @Inject(method = "getPositionOffset(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void axy$sneakOffset(AbstractClientPlayerEntity player, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (MorphManager.isMorphed(player) && player.isInSneakingPose()) {
            cir.setReturnValue(new Vec3d(0.0, AxyAnimator.SNEAK_OFFSET_Y, 0.0));
        }
    }
}
