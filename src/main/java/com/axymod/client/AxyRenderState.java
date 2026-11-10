package com.axymod.client;

/** Flags de renderização (o render roda em uma única thread). */
public final class AxyRenderState {
    /** true enquanto o braço de primeira pessoa está sendo desenhado. */
    public static boolean firstPersonArm = false;
    /** true enquanto itens na mão são posicionados (para o item não ser achatado pela escala do braço). */
    public static boolean suppressScale = false;

    /** true depois que a posição do item na mão já foi corrigida nesta chamada de renderItem. */
    public static boolean heldShifted = false;

    private AxyRenderState() {}
}
