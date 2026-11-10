package com.axymod.client.mixin;

import com.axymod.client.AxyAnimator;
import com.axymod.client.MorphManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mesmo ajuste da elytra para a capa. */
@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {
    @Unique private boolean axy$pushed;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At("HEAD"), require = 0)
    private void axy$start(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity,
                           float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                           float headYaw, float headPitch, CallbackInfo ci) {
        axy$pushed = MorphManager.isMorphed(entity);
        if (axy$pushed) {
            matrices.push();
            AxyAnimator.bodyAttachTransform(matrices);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At("RETURN"), require = 0)
    private void axy$end(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity,
                         float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                         float headYaw, float headPitch, CallbackInfo ci) {
        if (axy$pushed) {
            matrices.pop();
            axy$pushed = false;
        }
    }
}
