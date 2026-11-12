#version 150

uniform sampler2D CurrentColor;
uniform sampler2D CurrentDepth;
uniform sampler2D History;
uniform sampler2D RawWorld;
uniform sampler2D OpaqueWorld;
uniform sampler2D OpaqueDepth;
uniform sampler2D TerrainDepth;
uniform sampler2D TerrainMotion;
uniform sampler2D ActorMotion;
uniform sampler2D NativeEmission;
uniform sampler2D ParticleCoverage;
uniform mat4 CurrentInverseProjection;
uniform mat4 PreviousClipFromView;
uniform vec2 WorldJitter;
uniform float HistoryWeight;
uniform float Disocclusion;
uniform float VarianceGamma;
uniform float ReactiveStrength;
uniform float ProtectUnknown;
uniform float HasTerrainMotion;
uniform float HasActorMotion;
uniform float HasNativeEmission;
uniform float ParticleCoverageMode;

in vec2 texCoord;
out vec4 fragColor;

float change(vec3 a, vec3 b) {
    vec3 difference = abs(a - b) / (1.0 + max(a, b));
    return max(difference.r, max(difference.g, difference.b));
}

vec3 sampleColor(vec2 uv) {
    ivec2 dimensions = textureSize(CurrentColor, 0);
    vec2 pixel = uv * vec2(dimensions) - 0.5;
    ivec2 base = ivec2(floor(pixel));
    ivec2 limit = dimensions - 1;
    vec2 f = fract(pixel);
    vec3 a = texelFetch(CurrentColor, clamp(base, ivec2(0), limit), 0).rgb;
    vec3 b = texelFetch(CurrentColor, clamp(base + ivec2(1, 0), ivec2(0), limit), 0).rgb;
    vec3 c = texelFetch(CurrentColor, clamp(base + ivec2(0, 1), ivec2(0), limit), 0).rgb;
    vec3 d = texelFetch(CurrentColor, clamp(base + ivec2(1), ivec2(0), limit), 0).rgb;
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

vec3 sampleHistory(vec2 uv) {
    ivec2 dimensions = textureSize(History, 0);
    vec2 pixel = uv * vec2(dimensions) - 0.5;
    ivec2 base = ivec2(floor(pixel));
    ivec2 limit = dimensions - 1;
    vec2 f = fract(pixel);
    vec3 a = texelFetch(History, clamp(base, ivec2(0), limit), 0).rgb;
    vec3 b = texelFetch(History, clamp(base + ivec2(1, 0), ivec2(0), limit), 0).rgb;
    vec3 c = texelFetch(History, clamp(base + ivec2(0, 1), ivec2(0), limit), 0).rgb;
    vec3 d = texelFetch(History, clamp(base + ivec2(1), ivec2(0), limit), 0).rgb;
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    vec2 pixel = 1.0 / vec2(textureSize(CurrentColor, 0));
    vec2 currentUv = clamp(texCoord + WorldJitter, pixel * 0.5, 1.0 - pixel * 0.5);
    float currentDepth = texture(CurrentDepth, currentUv).r;
    vec4 unprojected = CurrentInverseProjection * vec4(currentUv * 2.0 - 1.0, currentDepth * 2.0 - 1.0, 1.0);
    vec3 view = unprojected.xyz / max(unprojected.w, 1.0e-6);
    bool sky = currentDepth >= 0.999999;
    float depth = sky ? 0.0 : clamp(-view.z, 0.0, 65000.0);
    vec3 current = sampleColor(currentUv);
    fragColor = vec4(current, depth);
    if (HistoryWeight <= 0.0) return;

    vec4 previous = PreviousClipFromView * vec4(view, sky ? 0.0 : 1.0);
    if (previous.w <= 0.0) return;
    vec2 historyUv = previous.xy / previous.w * 0.5 + 0.5;
    float previousDepth = previous.w;
    bool trustedMotion = !sky && HasTerrainMotion > 0.5 && abs(texture(TerrainDepth, currentUv).r - currentDepth) < 0.00000024;
    if (trustedMotion)
        historyUv = texCoord - texture(TerrainMotion, currentUv).xy;
    if (HasActorMotion > 0.5) {
        vec4 actor = texture(ActorMotion, currentUv);
        if (actor.a < -0.5) return;
        if (actor.a > 0.5) {
            trustedMotion = true;
            historyUv = texCoord - actor.xy;
            previousDepth = actor.z;
        }
    }
    if (ProtectUnknown > 0.5 && HasTerrainMotion > 0.5 && !sky && !trustedMotion) return;
    if (any(lessThan(historyUv, pixel * 0.5)) || any(greaterThan(historyUv, 1.0 - pixel * 0.5))) return;
    float oldDepth = texture(History, historyUv).a;
    if (sky != (oldDepth <= 0.0)) return;
    if (!sky && abs(oldDepth - previousDepth) > max(0.02, previousDepth * Disocclusion)) return;

    float rawDifference = change(texture(RawWorld, currentUv).rgb, texture(OpaqueWorld, currentUv).rgb);
    float opaqueDepth = texture(OpaqueDepth, currentUv).r;
    float coverage = rawDifference * (abs(opaqueDepth - currentDepth) > 0.0001 ? 3.0 : 1.0);
    if (HasNativeEmission > 0.5) {
        vec4 emission = texture(NativeEmission, currentUv);
        if (emission.a <= currentDepth + 0.00000024 && max(max(emission.r, emission.g), emission.b) > 0.0001) return;
    }
    if (ParticleCoverageMode > 0.5) {
        float particle = texture(ParticleCoverage, currentUv).a;
        coverage = max(coverage, ParticleCoverageMode > 1.5 ? 1.0 - particle : particle);
    }
    if (ProtectUnknown > 0.5 && coverage > 0.055) return;

    vec3 mean = vec3(0.0);
    vec3 secondMoment = vec3(0.0);
    vec3 minimum = current;
    vec3 maximum = current;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec3 sampleValue = texture(CurrentColor, currentUv + vec2(x, y) * pixel).rgb;
            mean += sampleValue;
            secondMoment += sampleValue * sampleValue;
            minimum = min(minimum, sampleValue);
            maximum = max(maximum, sampleValue);
        }
    }
    mean /= 9.0;
    vec3 sigma = sqrt(max(secondMoment / 9.0 - mean * mean, vec3(0.0)));
    vec3 old = sampleHistory(historyUv);
    vec3 clipped = clamp(old, max(minimum, mean - sigma * VarianceGamma), min(maximum, mean + sigma * VarianceGamma));
    float reactive = smoothstep(0.025, 0.3, max(coverage, change(old, current)));
    float weight = HistoryWeight * (1.0 - reactive * ReactiveStrength);
    fragColor.rgb = mix(current, clipped, weight);
}
