package dev.taste.engine.api;

@FunctionalInterface
public interface ShaderPolicyProvider {
    enum ColorSpace { VANILLA_ENCODED, LINEAR, ALREADY_TONEMAPPED }
    enum Alpha { OPAQUE, STRAIGHT, PREMULTIPLIED, ADDITIVE }
    record Policy(ColorSpace colorSpace, Alpha alpha, boolean writesDepth, boolean criticalVisibility) { }
    Policy policy(String programName);
}
