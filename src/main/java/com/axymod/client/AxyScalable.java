package com.axymod.client;

/** Escala extra por ModelPart, adicionada ao ModelPart via mixin. */
public interface AxyScalable {
    void axy$setScale(float x, float y, float z);
    float axy$scaleX();
    float axy$scaleY();
    float axy$scaleZ();
}
