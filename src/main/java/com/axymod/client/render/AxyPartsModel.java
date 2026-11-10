package com.axymod.client.render;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/**
 * Cauda e guelras do axolote (convertidas do addon "Axolotl Parts" de Strippz, formato Bedrock).
 *
 * Conversão Bedrock -> Java: y_java = 24 - y_bedrock; os pivôs ficam relativos à parte do corpo
 * onde são encaixados (cabeça / tronco), cujo pivô vanilla está em y = 24 (Bedrock).
 * Os ângulos de pitch/yaw têm o mesmo sinal nos dois formatos.
 */
public final class AxyPartsModel {
    private static final int TEX = 64;

    private static float rad(float degrees) {
        return degrees * (float) Math.PI / 180f;
    }

    /** Guelras: 2 leques no topo da cabeça + 2 placas de cada lado. Coordenadas relativas à cabeça. */
    public static ModelPart createGills() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        // leques do topo
        root.addChild("top_l", ModelPartBuilder.create().uv(-5, 6).cuboid(-4f, 0f, 0f, 3f, 0f, 5f),
                ModelTransform.of(0f, -8f, 0f, rad(40f), 0f, 0f));
        root.addChild("top_r", ModelPartBuilder.create().uv(1, 6).cuboid(1f, 0f, 0f, 3f, 0f, 5f),
                ModelTransform.of(0f, -8f, 0f, rad(40f), 0f, 0f));

        // lado esquerdo (x negativo)
        ModelTransform left = ModelTransform.of(-3.999f, -4f, -1f, rad(90f), rad(50f), 0f);
        root.addChild("side_l1", ModelPartBuilder.create().uv(-4, 12).cuboid(-5f, 0f, 0f, 5f, 0f, 4f), left);
        root.addChild("side_l2", ModelPartBuilder.create().uv(-3, 17).cuboid(-4f, 0f, -3f, 4f, 0f, 3f), left);

        // lado direito (espelhado)
        ModelTransform right = ModelTransform.of(3.999f, -4f, -1f, rad(90f), rad(-50f), 0f);
        root.addChild("side_r1", ModelPartBuilder.create().uv(-4, 12).mirrored(true).cuboid(0f, 0f, 0f, 5f, 0f, 4f), right);
        root.addChild("side_r2", ModelPartBuilder.create().uv(-3, 17).mirrored(true).cuboid(0f, 0f, -3f, 4f, 0f, 3f), right);

        return TexturedModelData.of(data, TEX, TEX).createModel();
    }

    /** Cauda (nadadeira vertical) + espinha dorsal. Coordenadas relativas ao tronco. */
    public static ModelPart createTail() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        root.addChild("tail", ModelPartBuilder.create().uv(0, 0).cuboid(0f, 0f, 0f, 10f, 5f, 0f),
                ModelTransform.of(0f, 7f, 2f, 0f, rad(-90f), 0f));
        root.addChild("spine", ModelPartBuilder.create().uv(21, 0).cuboid(-1f, -6f, 0f, 1f, 3f, 0f),
                ModelTransform.of(0f, 10f, 3f, 0f, rad(-90f), 0f));

        return TexturedModelData.of(data, TEX, TEX).createModel();
    }

    private AxyPartsModel() {}
}
