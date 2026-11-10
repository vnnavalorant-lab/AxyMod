package com.axymod.client.mixin;

import com.axymod.client.AxyAnimator;
import com.axymod.client.MorphManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leva a elytra para o tronco menor da Axy (senão ela fica flutuando no tamanho vanilla).
 * Desenha numa MatrixStack NOVA (cópia da original) em vez de push()/pop() em dois pontos,
 * então a pilha original nunca fica desbalanceada.
 */
@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {
    @Unique private boolean axy$reentry;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void axy$start(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity,
                           float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                           float headYaw, float headPitch, CallbackInfo ci) {
        if (axy$reentry) return;
        if (!(entity instanceof AbstractClientPlayerEntity player) || !MorphManager.isMorphed(player)) return;

        MatrixStack copy = new MatrixStack();
        copy.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
        copy.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());
        AxyAnimator.bodyAttachTransform(copy);

        axy$reentry = true;
        try {
            ElytraFeatureRenderer self = (ElytraFeatureRenderer) (Object) this;
            self.render(copy, vertexConsumers, light, entity,
                    limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
        } finally {
            axy$reentry = false;
        }
        ci.cancel();
    }
}
