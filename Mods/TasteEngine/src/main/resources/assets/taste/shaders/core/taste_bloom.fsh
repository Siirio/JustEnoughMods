#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D FineSampler;
uniform sampler2D DepthSampler;
uniform sampler2D TerrainEmission;
uniform sampler2D TerrainDepth;
uniform sampler2D SurfaceData;
uniform sampler2D SurfaceDepth;
uniform sampler2D NativeEmission;
uniform sampler2D ParticleEmission;
uniform sampler2D Atmosphere;
uniform vec2 SourceJitter;
uniform vec2 InSize;
uniform float Mode;
uniform float Threshold;
uniform float Knee;
uniform float SkyWeight;
uniform float Radius;
uniform float Anamorphic;
uniform float TerrainEmissionActive;
uniform float SurfaceEmissionActive;
uniform float EmissionScale;
uniform float EmissionWeight;
uniform float NativeEmissionActive;
uniform float ParticleEmissionActive;
uniform float HasAtmosphere;

in vec2 texCoord;
out vec4 fragColor;

vec3 sampleBloom(vec2 uv) {
    vec3 color = max(texture(DiffuseSampler, uv).rgb, vec3(0.0));
    if (Mode >= 0.5) return color;
    vec2 sourceUv = uv + SourceJitter;
    float worldDepth = texture(DepthSampler, sourceUv).r;
    vec3 emission = vec3(0.0);
    if (TerrainEmissionActive > 0.5 && abs(texture(TerrainDepth, sourceUv).r - worldDepth) < 0.00000024)
        emission += texture(TerrainEmission, sourceUv).rgb * EmissionScale;
    if (SurfaceEmissionActive > 0.5 && texture(SurfaceDepth, sourceUv).r <= worldDepth + 0.00000024) {
        vec4 data = texture(SurfaceData, sourceUv);
        int flags = int(data.z + 0.5);
        int family = flags & 15;
        if (family >= 5) {
            int tint = int(data.a);
            vec3 rgb = vec3((tint >> 16) & 255, (tint >> 8) & 255, tint & 255) / 255.0;
            vec3 linear = mix(rgb / 12.92, pow((rgb + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), rgb));
            emission = max(emission, linear * (float(flags >> 4) / 256.0) * EmissionScale);
        }
    }
    if (NativeEmissionActive > 0.5) {
        vec4 nativeEmission = texture(NativeEmission, sourceUv);
        float transmission = HasAtmosphere > 0.5 ? texture(Atmosphere, sourceUv).a : 1.0;
        if (nativeEmission.a <= worldDepth + 0.00000024) emission += nativeEmission.rgb * transmission;
    }
    if (ParticleEmissionActive > 0.5) emission += texture(ParticleEmission, sourceUv).rgb;
    vec3 reflected = max(color - emission, vec3(0.0));
    float luminance = dot(reflected, vec3(0.2126, 0.7152, 0.0722));
    float knee = Threshold * Knee;
    float soft = clamp(luminance - Threshold + knee, 0.0, 2.0 * knee);
    soft = soft * soft / max(4.0 * knee, 1.0e-5);
    float contribution = max(luminance - Threshold, soft) / max(luminance, 1.0e-5);
    float sky = step(1.0, worldDepth);
    return reflected * contribution * mix(1.0, SkyWeight, sky) + emission * EmissionWeight;
}

void main() {
    vec2 axis = vec2(exp2(Anamorphic), exp2(-Anamorphic));
    vec2 stepSize = axis * mix(0.5, 2.0, Radius) / InSize;
    vec3 filtered = vec3(0.0);
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            float weight = (x == 0 ? 2.0 : 1.0) * (y == 0 ? 2.0 : 1.0) / 16.0;
            filtered += sampleBloom(texCoord + vec2(x, y) * stepSize) * weight;
        }
    }
    if (Mode > 1.5) filtered = mix(texture(FineSampler, texCoord).rgb, filtered, 0.5);
    fragColor = vec4(filtered, 1.0);
}
