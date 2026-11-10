package com.axymod.client.mixin;

import com.axymod.client.AxyRenderState;
import com.axymod.client.AxyScalable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adiciona uma escala própria (x, y, z) a cada ModelPart, aplicada em torno do pivô. */
@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements AxyScalable {
    @Unique private float axy$sx = 1f;
    @Unique private float axy$sy = 1f;
    @Unique private float axy$sz = 1f;

    @Override
    public void axy$setScale(float x, float y, float z) {
        this.axy$sx = x;
        this.axy$sy = y;
        this.axy$sz = z;
    }

    @Override public float axy$scaleX() { return this.axy$sx; }
    @Override public float axy$scaleY() { return this.axy$sy; }
    @Override public float axy$scaleZ() { return this.axy$sz; }

    @Inject(method = "rotate(Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
    private void axy$applyScale(MatrixStack matrices, CallbackInfo ci) {
        if (AxyRenderState.suppressScale) return;
        if (axy$sx != 1f || axy$sy != 1f || axy$sz != 1f) {
            matrices.scale(axy$sx, axy$sy, axy$sz);
        }
    }
}
