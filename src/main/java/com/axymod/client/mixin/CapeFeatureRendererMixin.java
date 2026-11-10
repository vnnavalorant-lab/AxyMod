package com.axymod.client.mixin;

import com.axymod.client.AxyAnimator;
import com.axymod.client.MorphManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mesmo ajuste da elytra para a capa.
 * Em vez de push() no início e pop() no fim (dois pontos que podem ficar desbalanceados e causar
 * "Pose stack not empty"), desenha a capa uma vez numa MatrixStack NOVA, copiada da original.
 * A pilha original nunca é modificada.
 */
@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {
    @Unique private boolean axy$reentry;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void axy$start(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity,
                           float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                           float headYaw, float headPitch, CallbackInfo ci) {
        if (axy$reentry || !MorphManager.isMorphed(entity)) return;

        MatrixStack copy = new MatrixStack();
        copy.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
        copy.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());
        AxyAnimator.bodyAttachTransform(copy);

        axy$reentry = true;
        try {
            ((CapeFeatureRenderer) (Object) this).render(copy, vertexConsumers, light, entity,
                    limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
        } finally {
            axy$reentry = false;
        }
        ci.cancel();
    }
}
