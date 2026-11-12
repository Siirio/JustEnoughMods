#version 150

uniform sampler2D ParticleAtlas;
uniform sampler2D SceneDepth;
uniform sampler2D LocalLights;
uniform mat4 InverseProjection;
uniform float LocalLightCount;
uniform float LightTileColumns;
uniform float SoftDistance;
uniform float Lighting;
uniform float Ambient;
uniform float SkyIntensity;
uniform float BlockIntensity;
uniform float EmissionStrength;
uniform float Mode;
uniform float FogDensity;
uniform float FogDistance;
uniform vec3 FogColor;

in vec2 spriteUv;
in vec4 spriteColor;
in vec2 packedLight;
in vec3 particleWorld;
in float particleDepth;
flat in float softClass;
flat in float emissionClass;
out vec4 fragColor;

vec3 decodeDisplay(vec3 value) {
    vec3 low = value / 12.92;
    vec3 high = pow((value + 0.055) / 1.055, vec3(2.4));
    return mix(low, high, step(vec3(0.04045), value));
}

float coverage(vec2 uv) {
    float raw = texture(SceneDepth, uv).r;
    if (raw >= 0.999999) return 1.0;
    vec4 view = InverseProjection * vec4(uv * 2.0 - 1.0, raw * 2.0 - 1.0, 1.0);
    float separation = -view.z / max(view.w, 1.0e-6) - particleDepth;
    if (separation <= 0.002) return 0.0;
    return softClass > 0.5 && SoftDistance > 0.0 ? smoothstep(0.0, SoftDistance, separation) : 1.0;
}

vec4 lightData(int index) {
    int width = textureSize(LocalLights, 0).x;
    return texelFetch(LocalLights, ivec2(index % width, index / width), 0);
}

vec3 localIrradiance(vec2 viewport) {
    if (LocalLightCount < 1.0) return vec3(0.0);
    ivec2 tile = ivec2(clamp(gl_FragCoord.xy, vec2(0.0), viewport - 1.0) / 32.0);
    int address = int(LocalLightCount) * 2 + (tile.y * int(LightTileColumns) + tile.x) * 5;
    int count = min(8, int(lightData(address).r));
    vec3 irradiance = vec3(0.0);
    for (int i = 0; i < count; i++) {
        vec4 indices = lightData(address + 1 + i / 4);
        int index = int(indices[i % 4]);
        if (index < 0 || index >= int(LocalLightCount)) continue;
        vec4 source = lightData(index * 2);
        float distance = length(source.xyz - particleWorld);
        if (distance >= source.w) continue;
        vec4 light = lightData(index * 2 + 1);
        float edge = 1.0 - smoothstep(0.0, max(source.w, 0.01), distance);
        irradiance += light.rgb * light.a * edge / (1.0 + distance * distance * 0.12);
    }
    return min(irradiance * BlockIntensity * 0.28, vec3(1.8));
}

void main() {
    vec4 sprite = texture(ParticleAtlas, spriteUv) * spriteColor;
    if (sprite.a <= 0.004) discard;
    vec2 viewport = vec2(textureSize(SceneDepth, 0));
    float alpha = clamp(sprite.a * coverage(gl_FragCoord.xy / viewport), 0.0, 1.0);
    if (alpha <= 0.004) discard;

    if (Mode > 1.5 && Mode < 2.5) {
        fragColor = vec4(0.0, 0.0, 0.0, alpha);
        return;
    }

    vec3 base = decodeDisplay(clamp(sprite.rgb, 0.0, 1.0));
    float transmittance = exp(-FogDensity * clamp(particleDepth, 0.0, FogDistance));
    vec3 emissive = base * emissionClass * EmissionStrength;
    if (Mode > 2.5) {
        fragColor = vec4(emissive * alpha * transmittance, alpha);
        return;
    }

    vec3 radiance = base;
    if (Lighting > 0.5 && emissionClass < 0.01) {
        float sky = clamp(packedLight.y, 0.0, 1.0) * SkyIntensity;
        float block = clamp(packedLight.x, 0.0, 1.0);
        vec3 nativeLight = vec3(Ambient) + vec3(0.68, 0.78, 0.91) * sky
                + vec3(0.65, 0.41, 0.22) * block * block * BlockIntensity;
        radiance *= nativeLight + localIrradiance(viewport);
    }
    radiance = mix(FogColor, radiance + emissive, transmittance);
    if (Mode > 0.5) {
        float weight = clamp((0.18 + alpha * 0.82) * 3.0 / (0.3 + particleDepth * 0.028), 0.12, 4.0);
        fragColor = vec4(radiance * alpha * weight, alpha * weight);
    } else fragColor = vec4(radiance * alpha, alpha);
}
