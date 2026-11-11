package com.axymod.client;

import com.axymod.Emote;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

/**
 * Aplica as proporções do Axy no modelo do jogador e toca as animações dos emotes.
 *
 * <h3>Proporções (% do tamanho normal do jogador)</h3>
 * <pre>
 *   cabeça  : 90% altura / largura / profundidade
 *   tronco  : 69% altura, 71% largura, 70% profundidade
 *   braços  : 59% altura, 71% largura, 70% profundidade
 *   pernas  : 57% altura, 73% largura, 70% profundidade
 * </pre>
 * A escala é aplicada em cada ModelPart (em torno do pivô) e os pivôs são
 * reposicionados para as partes continuarem encaixadas e os pés no chão.
 */
public final class AxyAnimator {
    // ---- escalas (1.0 = 100%) ----
    public static final float HEAD = 0.90f;
    // v1.2.2: proporções medidas pixel a pixel no print de referência (cabeça = 62px = 90%):
    //   tronco 71px de altura (o MAIOR segmento do corpo), braços ~60px, pernas ~59px;
    //   tronco, braços e pernas com ~49px / ~18px / ~25px de largura cada.
    // (Antes o tronco estava curto demais (60%) e as pernas compridas demais (70%).)
    // Profundidades (Z) não aparecem de frente e ficam em 70%.
    public static final float BODY_X = 0.71f, BODY_Y = 0.69f, BODY_Z = 0.70f;
    public static final float ARM_X = 0.71f, ARM_Y = 0.59f, ARM_Z = 0.70f;
    // As duas pernas juntas (7,8 * LEG_X) ficam com a mesma largura do tronco (8 * BODY_X).
    public static final float LEG_X = 0.73f, LEG_Y = 0.57f, LEG_Z = 0.70f;
    /** A cabeça "afunda" um pouco no tronco (em unidades de textura), como na referência. */
    public static final float HEAD_SINK = 0.25f;

    // ---- layout do modelo (1 unidade = 1 pixel de textura; o chão fica em y = 24) ----
    private static final float LEG_PIVOT_Y = 24f - 12f * LEG_Y;            // topo das pernas
    private static final float BODY_PIVOT_Y = LEG_PIVOT_Y - 12f * BODY_Y;  // topo do tronco
    private static final float HEAD_PIVOT_Y = BODY_PIVOT_Y;                // base da cabeça = topo do tronco
    private static final float ARM_PIVOT_Y = BODY_PIVOT_Y + 2f * ARM_Y;    // ombro (braço vanilla: pivô 2px abaixo do topo)
    // Braço fino (3px): borda interna fica 1px do pivô. Pivô vanilla X = 5.
    private static final float ARM_X_RATIO = (4f * BODY_X + 1f * ARM_X) / 5f;
    // Pernas coladas no meio, como no Steve/Alex: o vanilla usa pivô X = 1.9 com perna de 4px (borda interna
    // 0.1px além do centro, sem vão). Escalar o afastamento pela MESMA escala da largura da perna mantém essa
    // proporção exata, então as pernas da Axy se encostam sem vão (antes o afastamento usava a escala do
    // tronco, 70%, enquanto a perna ficava mais fina, deixando um vão no meio).
    private static final float LEG_X_RATIO = LEG_X;

    private static final float VANILLA_ARM_Y = 2f;
    private static final float VANILLA_LEG_Y = 12f;

    // Valores vanilla que o Minecraft NÃO reaplica a cada frame (por isso precisamos restaurar nós mesmos).
    private static final float VANILLA_ARM_X = 5f;
    private static final float VANILLA_LEG_X = 1.9f;

    /**
     * O modelo do jogador é reaproveitado entre frames, e o vanilla só reescreve parte dos valores
     * (pivôs X/Z da cabeça, tronco e pernas e o roll da cabeça ficam como estavam). Como o Axy mexe
     * nesses valores, sem restaurar eles acumulavam frame a frame: pernas se fundindo no meio,
     * cabeça se afastando/inclinando, etc. Aqui voltamos tudo para o estado vanilla antes de aplicar o Axy.
     */
    private static void restoreVanillaBase(PlayerEntityModel<?> m) {
        m.head.pivotX = 0f;
        m.head.pivotZ = 0f;
        m.head.roll = 0f;
        m.body.pivotX = 0f;
        m.body.pivotZ = 0f;
        m.body.roll = 0f;
        m.rightArm.pivotX = -VANILLA_ARM_X;
        m.leftArm.pivotX = VANILLA_ARM_X;
        m.rightLeg.pivotX = -VANILLA_LEG_X;
        m.leftLeg.pivotX = VANILLA_LEG_X;
    }

    public static void apply(PlayerEntityModel<?> m, AbstractClientPlayerEntity player, float progress) {
        restoreVanillaBase(m);
        if (player == null || !MorphManager.isMorphed(player)) {
            setScales(m, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f);
            return;
        }
        // Na primeira pessoa só a escala do braço vale (os pivôs ficam como no vanilla).
        if (!AxyRenderState.firstPersonArm) {
            reposition(m);
            Emote emote = EmoteManager.current(player, progress);
            if (emote != Emote.NORMAL) {
                float t = EmoteManager.elapsed(player, progress);
                animate(m, emote, t, EmoteManager.envelope(t, emote.duration));
            }
            syncOverlays(m);
        }
        setScales(m, HEAD, HEAD, HEAD, BODY_X, BODY_Y, BODY_Z, ARM_X, ARM_Y, ARM_Z, LEG_X, LEG_Y, LEG_Z);
    }

    // ------------------------------------------------------------------ itens, elytra e agachar

    /**
     * Deslocamento vertical do modelo quando o jogador transformado está agachado (em blocos).
     * O vanilla usa -0.125, o que afundaria as pernas curtas da Axy no chão; por isso aqui é 0.
     * Se os pés ficarem flutuando, use um valor entre -0.125 e 0 (ex.: -0.05f).
     */
    public static final float SNEAK_OFFSET_Y = 0f;

    /**
     * O item na mão é posicionado no vanilla a 10px (0.625) abaixo do ombro, onde ficaria a mão do braço
     * normal. Como o braço da Axy é mais curto, sobe o item na mesma proporção para ficar na mão dela.
     */
    public static void shiftHeldItem(PlayerEntityModel<?> m, Arm arm, MatrixStack matrices) {
        if (AxyRenderState.heldShifted) return;
        ModelPart part = arm == Arm.RIGHT ? m.rightArm : m.leftArm;
        float sy = ((AxyScalable) (Object) part).axy$scaleY();
        if (sy == 1f) return;
        matrices.translate(0f, -0.625f * (1f - sy), 0f);
        AxyRenderState.heldShifted = true;
    }

    /** Elytra e capa são desenhadas a partir do topo do tronco vanilla; leva para o tronco (menor) da Axy. */
    public static void bodyAttachTransform(MatrixStack matrices) {
        matrices.translate(0f, BODY_PIVOT_Y / 16f, 0f);
        matrices.scale(BODY_X, BODY_Y, BODY_Z);
    }

    // ------------------------------------------------------------------ layout

    private static void reposition(PlayerEntityModel<?> m) {
        m.head.pivotY = HEAD_PIVOT_Y + HEAD_SINK + m.head.pivotY * BODY_Y;
        m.body.pivotY = BODY_PIVOT_Y + m.body.pivotY * BODY_Y;

        m.rightArm.pivotX *= ARM_X_RATIO;
        m.leftArm.pivotX *= ARM_X_RATIO;
        m.rightArm.pivotY = ARM_PIVOT_Y + (m.rightArm.pivotY - VANILLA_ARM_Y) * ARM_Y;
        m.leftArm.pivotY = ARM_PIVOT_Y + (m.leftArm.pivotY - VANILLA_ARM_Y) * ARM_Y;
        m.rightArm.pivotZ *= ARM_Z;
        m.leftArm.pivotZ *= ARM_Z;

        m.rightLeg.pivotX *= LEG_X_RATIO;
        m.leftLeg.pivotX *= LEG_X_RATIO;
        m.rightLeg.pivotY = LEG_PIVOT_Y + (m.rightLeg.pivotY - VANILLA_LEG_Y) * LEG_Y;
        m.leftLeg.pivotY = LEG_PIVOT_Y + (m.leftLeg.pivotY - VANILLA_LEG_Y) * LEG_Y;
        m.rightLeg.pivotZ *= LEG_Z;
        m.leftLeg.pivotZ *= LEG_Z;
    }

    private static void syncOverlays(PlayerEntityModel<?> m) {
        m.hat.copyTransform(m.head);
        m.jacket.copyTransform(m.body);
        m.leftSleeve.copyTransform(m.leftArm);
        m.rightSleeve.copyTransform(m.rightArm);
        m.leftPants.copyTransform(m.leftLeg);
        m.rightPants.copyTransform(m.rightLeg);
    }

    // ------------------------------------------------------------------ escala

    private static void setScales(PlayerEntityModel<?> m,
                                  float hx, float hy, float hz,
                                  float bx, float by, float bz,
                                  float ax, float ay, float az,
                                  float lx, float ly, float lz) {
        scale(m.head, hx, hy, hz);
        scale(m.hat, hx, hy, hz);
        scale(m.body, bx, by, bz);
        scale(m.jacket, bx, by, bz);
        scale(m.rightArm, ax, ay, az);
        scale(m.leftArm, ax, ay, az);
        scale(m.rightSleeve, ax, ay, az);
        scale(m.leftSleeve, ax, ay, az);
        scale(m.rightLeg, lx, ly, lz);
        scale(m.leftLeg, lx, ly, lz);
        scale(m.rightPants, lx, ly, lz);
        scale(m.leftPants, lx, ly, lz);
    }

    private static void scale(ModelPart part, float x, float y, float z) {
        ((AxyScalable) (Object) part).axy$setScale(x, y, z);
    }

    /** Copia a escala do modelo do jogador para o modelo da armadura (chamado por BipedEntityModelMixin). */
    public static void copyScales(BipedEntityModel<?> from, BipedEntityModel<?> to) {
        copy(from.head, to.head);
        copy(from.hat, to.hat);
        copy(from.body, to.body);
        copy(from.rightArm, to.rightArm);
        copy(from.leftArm, to.leftArm);
        copy(from.rightLeg, to.rightLeg);
        copy(from.leftLeg, to.leftLeg);
    }

    private static void copy(ModelPart from, ModelPart to) {
        AxyScalable f = (AxyScalable) (Object) from;
        ((AxyScalable) (Object) to).axy$setScale(f.axy$scaleX(), f.axy$scaleY(), f.axy$scaleZ());
    }

    // ------------------------------------------------------------------ emotes

    private static float blend(float current, float target, float env) {
        return current + (target - current) * env;
    }

    private static float sin(float v) {
        return MathHelper.sin(v);
    }

    private static void shiftY(PlayerEntityModel<?> m, float dy) {
        m.head.pivotY += dy;
        m.body.pivotY += dy;
        m.rightArm.pivotY += dy;
        m.leftArm.pivotY += dy;
        m.rightLeg.pivotY += dy;
        m.leftLeg.pivotY += dy;
    }

    /**
     * Convenções (espaço do modelo, y para baixo): pitch negativo levanta o braço para frente/cima;
     * em braço pendurado, roll positivo no braço direito / negativo no esquerdo abre o braço;
     * yaw negativo no direito / positivo no esquerdo leva a mão para o centro do corpo.
     * Com o braço já levantado (pitch ~ -3) o sinal do roll inverte.
     */
    private static void animate(PlayerEntityModel<?> m, Emote emote, float t, float env) {
        switch (emote) {
            case CONFUSED -> {
                m.head.roll += env * 0.22f * sin(t * 0.18f);
                m.head.yaw += env * 0.25f * sin(t * 0.09f);
                // mão direita coçando a cabeça
                m.rightArm.pitch = blend(m.rightArm.pitch, -2.0f + 0.12f * sin(t * 0.5f), env);
                m.rightArm.yaw = blend(m.rightArm.yaw, -0.5f, env);
            }
            case EXCITED -> {
                float wave = sin(t * 0.9f);
                shiftY(m, -Math.abs(sin(t * 0.45f)) * 1.4f * env); // pulinhos
                m.head.pitch -= 0.15f * env;
                m.head.roll += env * 0.10f * sin(t * 0.45f);
                // braços para cima (V)
                m.rightArm.pitch = blend(m.rightArm.pitch, -2.9f + 0.25f * wave, env);
                m.rightArm.roll = blend(m.rightArm.roll, -(0.35f + 0.25f * wave), env);
                m.leftArm.pitch = blend(m.leftArm.pitch, -2.9f - 0.25f * wave, env);
                m.leftArm.roll = blend(m.leftArm.roll, 0.35f + 0.25f * wave, env);
            }
            case MAD -> {
                m.head.yaw += env * 0.10f * sin(t * 1.6f);
                m.head.pitch += env * 0.20f;
                // punhos abertos para os lados, tremendo
                float shake = 0.05f * sin(t * 2.0f);
                m.rightArm.roll = blend(m.rightArm.roll, 0.35f + shake, env);
                m.leftArm.roll = blend(m.leftArm.roll, -0.35f - shake, env);
                m.rightArm.pitch = blend(m.rightArm.pitch, 0.1f, env);
                m.leftArm.pitch = blend(m.leftArm.pitch, 0.1f, env);
                // batendo o pé
                float st = sin(t * 0.6f);
                m.rightLeg.pitch = blend(m.rightLeg.pitch, -0.5f * Math.max(st, 0f), env);
                m.leftLeg.pitch = blend(m.leftLeg.pitch, -0.5f * Math.max(-st, 0f), env);
            }
            case SAD -> {
                m.head.pitch += env * (0.45f + 0.04f * sin(t * 0.9f)); // soluçando
                m.head.yaw += env * 0.08f * sin(t * 0.07f);
                m.rightArm.pitch = blend(m.rightArm.pitch, 0.15f, env);
                m.leftArm.pitch = blend(m.leftArm.pitch, 0.15f, env);
                m.rightArm.roll = blend(m.rightArm.roll, -0.08f, env);
                m.leftArm.roll = blend(m.leftArm.roll, 0.08f, env);
            }
            case SCARED -> {
                float tremble = sin(t * 2.8f) * 0.04f * env;
                float jitter = sin(t * 3.1f) * 0.25f * env;
                m.head.yaw += tremble * 1.5f;
                m.head.roll += tremble;
                m.rightArm.pitch = blend(m.rightArm.pitch, -1.5f + 0.05f * sin(t * 2.2f), env);
                m.leftArm.pitch = blend(m.leftArm.pitch, -1.5f + 0.05f * sin(t * 2.2f + 1f), env);
                m.rightArm.yaw = blend(m.rightArm.yaw, -0.6f, env);
                m.leftArm.yaw = blend(m.leftArm.yaw, 0.6f, env);
                m.rightLeg.pitch += tremble;
                m.leftLeg.pitch -= tremble;
                m.head.pivotX += jitter;
                m.body.pivotX += jitter;
                m.rightArm.pivotX += jitter;
                m.leftArm.pivotX += jitter;
                m.rightLeg.pivotX += jitter;
                m.leftLeg.pivotX += jitter;
            }
            case SICK -> {
                shiftY(m, 0.3f * sin(t * 0.3f) * env); // balançando enjoada
                m.head.pitch += env * 0.35f;
                m.head.roll += env * 0.2f * sin(t * 0.09f);
                // mãos na barriga
                m.rightArm.pitch = blend(m.rightArm.pitch, -0.8f, env);
                m.leftArm.pitch = blend(m.leftArm.pitch, -0.8f, env);
                m.rightArm.yaw = blend(m.rightArm.yaw, -0.6f, env);
                m.leftArm.yaw = blend(m.leftArm.yaw, 0.6f, env);
            }
            case SILLY -> {
                float flap = sin(t * 0.7f);
                shiftY(m, -Math.abs(sin(t * 0.35f)) * 0.6f * env);
                m.head.roll += env * 0.5f * sin(t * 0.5f);
                m.head.yaw += env * 0.3f * sin(t * 0.25f);
                m.head.pitch -= 0.1f * env;
                m.rightArm.pitch = blend(m.rightArm.pitch, -0.5f + 0.9f * flap, env);
                m.rightArm.roll = blend(m.rightArm.roll, 0.5f + 0.5f * sin(t * 0.7f + 1f), env);
                m.leftArm.pitch = blend(m.leftArm.pitch, -0.5f - 0.9f * flap, env);
                m.leftArm.roll = blend(m.leftArm.roll, -(0.5f + 0.5f * sin(t * 0.7f + 1f + 3.1416f)), env);
                m.rightLeg.pitch = blend(m.rightLeg.pitch, 0.6f * flap, env);
                m.leftLeg.pitch = blend(m.leftLeg.pitch, -0.6f * flap, env);
            }
            default -> {
            }
        }
    }

    private AxyAnimator() {}
}
