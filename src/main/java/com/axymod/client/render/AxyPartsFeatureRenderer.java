package com.axymod.client.render;

import com.axymod.AxyMod;
import com.axymod.Emote;
import com.axymod.client.AxyConfig;
import com.axymod.client.EmoteManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Desenha a cauda (presa ao tronco) e as guelras (presas à cabeça) do axolote no jogador. */
public class AxyPartsFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier TEXTURE = new Identifier(AxyMod.MOD_ID, "textures/axolotl_parts/axolotl_texture.png");
    private static final float HALF_PI = (float) (Math.PI / 2.0);

    private final ModelPart gills = AxyPartsModel.createGills();
    private final ModelPart tail = AxyPartsModel.createTail();

    public AxyPartsFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (!AxyConfig.enabled || player.isInvisible() || player.isSpectator()) return;

        animate(player, animationProgress, limbAngle, limbDistance);

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlay(player, 0.0f);
        PlayerEntityModel<AbstractClientPlayerEntity> model = this.getContextModel();

        matrices.push();
        model.head.rotate(matrices);
        gills.render(matrices, consumer, light, overlay);
        matrices.pop();

        matrices.push();
        model.body.rotate(matrices);
        tail.render(matrices, consumer, light, overlay);
        matrices.pop();
    }

    private void animate(AbstractClientPlayerEntity player, float age, float limbAngle, float limbDistance) {
        Emote emote = EmoteManager.current(player, age);

        // rabo balançando: mais rápido/forte conforme o emote
        float speed = 0.12f;
        float amplitude = 0.18f;
        switch (emote) {
            case EXCITED -> { speed = 0.6f; amplitude = 0.45f; }
            case SCARED -> { speed = 1.2f; amplitude = 0.2f; }
            case MAD -> { speed = 0.5f; amplitude = 0.35f; }
            case SILLY -> { speed = 0.5f; amplitude = 0.4f; }
            case SAD, SICK -> { speed = 0.05f; amplitude = 0.06f; }
            default -> { }
        }
        float sway = MathHelper.sin(age * speed) * amplitude
                + MathHelper.cos(limbAngle * 0.6662f) * limbDistance * 0.5f;
        tail.getChild("tail").yaw = -HALF_PI + sway;

        // guelras mexendo de leve
        float flap = MathHelper.sin(age * 0.15f) * 0.1f;
        float base = (float) Math.toRadians(50.0);
        gills.getChild("side_l1").yaw = base + flap;
        gills.getChild("side_l2").yaw = base + flap;
        gills.getChild("side_r1").yaw = -base - flap;
        gills.getChild("side_r2").yaw = -base - flap;
    }
}
