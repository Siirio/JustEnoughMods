package dev.taste.engine.render.forward;

import dev.taste.engine.api.ShaderPolicyProvider;
import java.util.LinkedHashMap;
import java.util.Map;

/** Keeps declared foreign color and alpha contracts instead of guessing shader source. */
public final class ForeignBatchBridge {
    private final Map<String, ShaderPolicyProvider.Policy> policies = new LinkedHashMap<>();
    public void register(String program, ShaderPolicyProvider.Policy policy) { policies.put(program, policy); }
    public ShaderPolicyProvider.Policy policy(String program) {
        return policies.getOrDefault(program, new ShaderPolicyProvider.Policy(
                ShaderPolicyProvider.ColorSpace.VANILLA_ENCODED, ShaderPolicyProvider.Alpha.STRAIGHT, true, false));
    }
    public boolean requiresNativeFallback(String program) { return !policies.containsKey(program) && program.contains("composite"); }
}
