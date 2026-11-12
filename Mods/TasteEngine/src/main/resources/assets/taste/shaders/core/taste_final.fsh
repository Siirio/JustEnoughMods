#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D BloomSampler;
uniform sampler2D ExposureSampler;
uniform vec2 InSize;
uniform mat4 InverseProjection;
uniform float Time;
uniform float Rain;
uniform float Exposure;
uniform float AutoExposure;
uniform vec3 ShadowTint;
uniform vec3 HighlightTint;
uniform float Saturation;
uniform float Contrast;
uniform float Temperature;
uniform float Tint;
uniform float Gamma;
uniform float BloomStrength;
uniform float FogStrength;
uniform float FogDistance;
uniform vec3 FogColor;
uniform float Vignette;
uniform float Grain;
uniform float Sharpen;
uniform float DofEnabled;
uniform float FocusDistance;
uniform float Aperture;
uniform float DofRadius;
uniform float Outline;
uniform float OutlineWidth;
uniform float ToonBands;
uniform float WetLens;
uniform float Underwater;
uniform float UnderwaterDensity;
uniform float Thunder;
uniform float ReducedMotion;
uniform float MinimumContrast;
uniform float ToneMapper;

in vec2 texCoord;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 filmic(vec3 x) {
    x = max(x, vec3(0.0));
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

vec3 neutral(vec3 color) {
    color = max(color, vec3(0.0));
    float x = min(color.r, min(color.g, color.b));
    float offset = x < 0.08 ? x - 6.25 * x * x : 0.04;
    color -= offset;
    float peak = max(color.r, max(color.g, color.b));
    if (peak < 0.76) return color;
    float q = 1.0 - 0.0576 / (peak + 0.24 - 0.76);
    color *= q / max(peak, 1.0e-5);
    return mix(color, vec3(q), 1.0 - 1.0 / (0.15 * (peak - q) + 1.0));
}

vec3 encodeDisplayColor(vec3 color) {
    vec3 displayLow = color * 12.92;
    vec3 displayHigh = 1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055;
    return mix(displayLow, displayHigh, step(vec3(0.0031308), color));
}

vec2 wetLensOffset(vec2 uv) {
    if (WetLens < 0.5 || Rain <= 0.001) return vec2(0.0);
    vec2 cells = uv * vec2(23.0, 13.0);
    vec2 id = floor(cells);
    vec2 local = fract(cells) - 0.5;
    float seed = hash12(id);
    float motion = mix(Time * 0.18, 0.0, ReducedMotion);
    local.y += fract(seed + motion * (0.35 + seed)) - 0.5;
    float drop = smoothstep(0.28, 0.04, length(local * vec2(0.65, 1.4)));
    return local * drop * Rain * 0.004;
}

float linearDepth(vec2 uv) {
    vec4 clip = vec4(uv * 2.0 - 1.0, texture(DepthSampler, uv).r * 2.0 - 1.0, 1.0);
    vec4 view = InverseProjection * clip;
    return max(0.0, -view.z / max(view.w, 1.0e-6));
}

vec3 depthFocus(vec2 uv, vec2 texel, vec3 center, float depthBlocks) {
    if (DofEnabled < 0.5) return center;
    float coc = clamp(abs(depthBlocks - FocusDistance) / max(depthBlocks, 0.25) * Aperture * 0.22, 0.0, 1.0);
    vec2 radius = texel * min(DofRadius, 14.0) * coc;
    vec3 blurred = center;
    blurred += texture(DiffuseSampler, uv + vec2(radius.x, 0.0)).rgb;
    blurred += texture(DiffuseSampler, uv - vec2(radius.x, 0.0)).rgb;
    blurred += texture(DiffuseSampler, uv + vec2(0.0, radius.y)).rgb;
    blurred += texture(DiffuseSampler, uv - vec2(0.0, radius.y)).rgb;
    return mix(center, blurred * 0.2, coc);
}

void main() {
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 uv = clamp(texCoord + wetLensOffset(texCoord), texel, vec2(1.0) - texel);
    vec3 center = texture(DiffuseSampler, uv).rgb;
    float centerDepth = linearDepth(uv);
    center = depthFocus(uv, texel, center, centerDepth);
    vec3 cross = texture(DiffuseSampler, uv + vec2(texel.x, 0.0)).rgb
               + texture(DiffuseSampler, uv - vec2(texel.x, 0.0)).rgb
               + texture(DiffuseSampler, uv + vec2(0.0, texel.y)).rgb
               + texture(DiffuseSampler, uv - vec2(0.0, texel.y)).rgb;
    vec3 color = max(center + (center * 4.0 - cross) * Sharpen * 0.18, vec3(0.0));
    color += texture(BloomSampler, uv).rgb * BloomStrength;

    float fogAmount = 1.0 - exp(-FogStrength * min(centerDepth, FogDistance));
    color = mix(color, FogColor, fogAmount);

    vec2 depthStep = texel * max(OutlineWidth, 1.0);
    float depthEdge = abs(centerDepth - linearDepth(uv + vec2(depthStep.x, 0.0)));
    depthEdge += abs(centerDepth - linearDepth(uv + vec2(0.0, depthStep.y)));
    float outlineMask = smoothstep(0.08, 0.7, depthEdge / max(centerDepth, 0.5)) * clamp(Outline, 0.0, 1.0);
    color *= 1.0 - outlineMask * 0.72;

    if (Underwater > 0.5) {
        float waterFog = 1.0 - exp(-max(UnderwaterDensity, 0.0) * centerDepth);
        color = mix(color, vec3(0.055, 0.31, 0.39), clamp(waterFog, 0.0, 0.94));
    }
    color += vec3(0.72, 0.82, 1.0) * Thunder * 0.16;

    float warmth = clamp((6500.0 - Temperature) / 4500.0, -1.0, 1.0);
    color *= vec3(1.0 + warmth * 0.10 + Tint * 0.02, 1.0 - abs(warmth) * 0.025 - Tint * 0.035, 1.0 - warmth * 0.10 + Tint * 0.02);
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luminance), color, Saturation);
    if (ToonBands >= 2.0) {
        float banded = floor(luminance * ToonBands + 0.5) / ToonBands;
        color *= banded / max(luminance, 0.025);
    }
    color = (color - 0.18) * Contrast + 0.18;
    color = mix(color, smoothstep(vec3(0.0), vec3(1.0), color), clamp(MinimumContrast, 0.0, 1.0) * 0.25);
    float ev = AutoExposure > 0.5 ? texelFetch(ExposureSampler, ivec2(0), 0).r : 0.0;
    color = max(color * (Exposure * exp2(ev)), vec3(0.0));
    float tone = dot(color, vec3(0.2126, 0.7152, 0.0722));
    vec3 gradeTint = mix(ShadowTint, HighlightTint, smoothstep(0.04, 0.75, tone));
    color *= gradeTint;
    color = ToneMapper > 0.5 ? neutral(color) : filmic(color);
    color = pow(max(color, vec3(0.0)), vec3(1.0 / max(Gamma, 0.01)));
    color = encodeDisplayColor(color);

    vec2 centered = texCoord * 2.0 - 1.0;
    float edge = smoothstep(0.30, 1.35, dot(centered, centered));
    color *= 1.0 - edge * Vignette;
    float noise = hash12(gl_FragCoord.xy + floor(Time * 60.0)) - 0.5;
    color += noise * Grain * 0.035;
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
