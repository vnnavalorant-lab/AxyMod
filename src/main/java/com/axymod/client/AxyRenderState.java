package com.axymod.client;

/** Flags de renderização (o render roda em uma única thread). */
public final class AxyRenderState {
    /** true enquanto o braço de primeira pessoa está sendo desenhado. */
    public static boolean firstPersonArm = false;
    /** true enquanto itens na mão são posicionados (para o item não ser achatado pela escala do braço). */
    public static boolean suppressScale = false;

    private AxyRenderState() {}
}
