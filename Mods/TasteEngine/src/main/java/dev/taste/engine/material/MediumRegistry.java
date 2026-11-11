package dev.taste.engine.material;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class MediumRegistry {
    public record Medium(float ior, float absorptionRed, float absorptionGreen, float absorptionBlue,
            float scatteringRed, float scatteringGreen, float scatteringBlue, float distortion) { }
    private final Map<ResourceLocation, Medium> media = new LinkedHashMap<>();
    public MediumRegistry() {
        media.put(new ResourceLocation("minecraft", "water"), new Medium(1.333F, 0.12F, 0.045F, 0.025F, 0.01F, 0.035F, 0.045F, 0.02F));
        media.put(new ResourceLocation("minecraft", "lava"), new Medium(1.45F, 0.18F, 0.08F, 0.01F, 0.18F, 0.035F, 0.005F, 0.035F));
    }
    public void register(ResourceLocation id, Medium medium) { media.put(id, medium); }
    public Medium get(ResourceLocation id) { return media.get(id); }
}
