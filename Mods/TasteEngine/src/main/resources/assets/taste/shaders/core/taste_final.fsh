#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D BloomSampler;
uniform sampler2D ExposureSampler;
uniform sampler2D OpticsSampler;
uniform sampler2D LutSampler;
uniform sampler2D RawWorld;
uniform sampler2D OpaqueWorld;
uniform vec2 InSize;
uniform vec2 OutputSize;
uniform mat4 InverseProjection;
uniform mat4 PreviousClipFromView;
uniform vec2 WorldJitter;
uniform vec2 SunUv;
uniform float HasMotionHistory;
uniform float Time;
uniform float Rain;
uniform sampler2D TerrainDepth;
uniform sampler2D TerrainMotion;
uniform sampler2D ActorMotion;
uniform float HasTerrainMotion;
uniform float HasActorMotion;
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
uniform float AutoFocus;
uniform float FocusDistance;
uniform float Aperture;
uniform float FocalLength;
uniform float DofRadius;
uniform float Shutter;
uniform float MotionSamples;
uniform float ChromaticAberration;
uniform float FlareStrength;
uniform float ProtectUnknown;
uniform float Outline;
uniform float OutlineWidth;
uniform vec3 OutlineColor;
uniform float PaletteStrength;
uniform float WetLens;
uniform float Underwater;
uniform float UnderwaterDensity;
uniform float Thunder;
uniform float FlashLimit;
uniform float ReducedMotion;
uniform float MinimumContrast;
uniform float ColorVision;
uniform float ToneMapper;
uniform float LutSize;
uniform float LutStrength;
uniform float DeferDisplayEncoding;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

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
    if (WetLens <= 0.001 || ReducedMotion > 0.5) return vec2(0.0);
    vec2 cells = uv * vec2(23.0, 13.0);
    vec2 id = floor(cells);
    vec2 local = fract(cells) - 0.5;
    float seed = hash12(id);
    local.y += fract(seed + Time * 0.18 * (0.35 + seed)) - 0.5;
    float drop = 1.0 - smoothstep(0.04, 0.28, length(local * vec2(0.65, 1.4)));
    return local * drop * WetLens * 0.004;
}

vec3 sampleScene(vec2 uv) {
    if (all(equal(InSize, OutputSize))) return texture(DiffuseSampler, uv).rgb;
    vec2 pixel = uv * InSize - 0.5;
    ivec2 base = ivec2(floor(pixel));
    ivec2 limit = ivec2(InSize) - 1;
    vec2 f = fract(pixel);
    vec3 a = texelFetch(DiffuseSampler, clamp(base, ivec2(0), limit), 0).rgb;
    vec3 b = texelFetch(DiffuseSampler, clamp(base + ivec2(1, 0), ivec2(0), limit), 0).rgb;
    vec3 c = texelFetch(DiffuseSampler, clamp(base + ivec2(0, 1), ivec2(0), limit), 0).rgb;
    vec3 d = texelFetch(DiffuseSampler, clamp(base + ivec2(1), ivec2(0), limit), 0).rgb;
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

vec3 viewPosition(vec2 uv, out float rawDepth) {
    vec2 depthUv = clamp(uv + WorldJitter, vec2(0.0), vec2(1.0));
    rawDepth = texture(DepthSampler, depthUv).r;
    vec4 view = InverseProjection * vec4(depthUv * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
    return view.xyz / max(view.w, 1.0e-6);
}

float linearDepth(vec2 uv) {
    float raw;
    return max(0.0, -viewPosition(uv, raw).z);
}

bool protectedCoverage(vec2 uv) {
    if (ProtectUnknown < 0.5) return false;
    vec2 source = uv + WorldJitter;
    vec3 raw = texture(RawWorld, source).rgb;
    vec3 opaque = texture(OpaqueWorld, source).rgb;
    vec3 change = abs(raw - opaque) / (1.0 + max(raw, opaque));
    return max(change.r, max(change.g, change.b)) > 0.055;
}

float cocRadius(float depth, float focus) {
    float objectDepth = max(0.05, depth);
    float safeFocus = max(focus, FocalLength + 0.0001);
    float sensorDiameter = FocalLength * FocalLength * (objectDepth - safeFocus)
            / max(Aperture * objectDepth * (safeFocus - FocalLength), 1.0e-8);
    return clamp(sensorDiameter * OutputSize.y / 0.048, -DofRadius, DofRadius);
}

vec3 depthFocus(vec2 uv, vec3 center, float depth, float focus) {
    if (DofEnabled < 0.5) return center;
    vec2 pixel = 1.0 / OutputSize;
    float centerCoc = cocRadius(depth, focus);
    float farRadius = max(centerCoc, 0.0);
    vec3 background = center;
    float backgroundWeight = 1.0;
    vec3 foreground = vec3(0.0);
    float foregroundWeight = 0.0;
    float separation = max(0.05, depth * 0.025);
    for (int i = 0; i < 16; i++) {
        float angle = float(i) * 2.3999632;
        float distance = sqrt((float(i) + 0.5) / 16.0);
        vec2 direction = vec2(cos(angle), sin(angle)) * distance * pixel;
        if (farRadius > 0.5) {
            vec2 tap = uv + direction * farRadius;
            float otherDepth = linearDepth(tap);
            if (otherDepth >= depth - separation && !protectedCoverage(tap)) {
                background += texture(DiffuseSampler, tap).rgb;
                backgroundWeight += 1.0;
            }
        }

        float nearRadius = max(-centerCoc, DofRadius * 0.6);
        vec2 nearTap = uv + direction * nearRadius;
        float nearDepth = linearDepth(nearTap);
        float nearCoc = -cocRadius(nearDepth, focus);
        if (nearCoc > 0.5 && nearDepth <= depth + separation && !protectedCoverage(nearTap)) {
            float weight = 1.0 - smoothstep(max(0.0, nearCoc - 1.0), nearCoc + 1.0, distance * nearRadius);
            foreground += texture(DiffuseSampler, nearTap).rgb * weight;
            foregroundWeight += weight;
        }
    }
    if (centerCoc < -0.5) {
        foreground += center;
        foregroundWeight += 1.0;
    }
    vec3 result = mix(center, background / backgroundWeight, smoothstep(0.5, 2.0, farRadius));
    float nearCoverage = max(smoothstep(0.5, 2.0, -centerCoc), clamp(foregroundWeight / 9.0, 0.0, 1.0));
    return mix(result, foreground / max(foregroundWeight, 1.0e-4), nearCoverage);
}

vec3 motionBlur(vec2 uv, vec3 center, vec3 view, float rawDepth) {
    if (Shutter <= 0.0 || HasMotionHistory < 0.5) return center;
    bool sky = rawDepth >= 0.999999;
    vec4 old = PreviousClipFromView * vec4(view, sky ? 0.0 : 1.0);
    if (old.w <= 0.0) return center;
    vec2 velocity = uv - (old.xy / old.w * 0.5 + 0.5);
    vec2 depthUv = uv + WorldJitter;
    if (!sky && HasTerrainMotion > 0.5 && abs(texture(TerrainDepth, depthUv).r - rawDepth) < 0.00000024)
        velocity = texture(TerrainMotion, depthUv).xy;
    if (HasActorMotion > 0.5) {
        vec4 actor = texture(ActorMotion, depthUv);
        if (actor.a < -0.5) return center;
        if (actor.a > 0.5) velocity = actor.xy;
    }
    vec2 travel = velocity * Shutter;
    float lengthPx = length(travel * OutputSize);
    if (lengthPx < 0.5) return center;
    travel *= min(1.0, 32.0 / lengthPx);
    int count = int(clamp(MotionSamples, 2.0, 32.0));
    float depth = -view.z;
    vec3 filtered = center;
    float weight = 1.0;
    for (int i = 1; i < count; i++) {
        vec2 tap = uv - travel * (float(i) / float(count - 1));
        if (any(lessThan(tap, vec2(0.0))) || any(greaterThan(tap, vec2(1.0))) || protectedCoverage(tap)) continue;
        float tapRaw;
        vec3 tapView = viewPosition(tap, tapRaw);
        if ((tapRaw >= 0.999999) != sky || (!sky && abs(-tapView.z - depth) > max(0.07, depth * 0.045))) continue;
        filtered += texture(DiffuseSampler, tap).rgb;
        weight += 1.0;
    }
    return filtered / weight;
}

vec3 lensFlare(vec2 uv, float visibility, float aspect) {
    if (FlareStrength <= 0.0 || visibility <= 0.0) return vec3(0.0);
    vec2 scale = vec2(aspect, 1.0);
    vec2 axis = vec2(0.5) - SunUv;
    float glow = exp(-length((uv - SunUv) * scale) * 22.0);
    float first = 1.0 - smoothstep(0.012, 0.042, length((uv - SunUv - axis * 1.27) * scale));
    float secondDistance = length((uv - SunUv - axis * 1.73) * scale);
    float second = smoothstep(0.026, 0.04, secondDistance) * (1.0 - smoothstep(0.041, 0.062, secondDistance));
    return (vec3(1.0, 0.72, 0.39) * glow + vec3(0.29, 0.56, 0.48) * first * 0.17
            + vec3(0.73, 0.38, 0.25) * second * 0.10) * FlareStrength * visibility;
}

vec3 fetchLut(ivec3 cell, int edge) {
    return texelFetch(LutSampler, ivec2(cell.x + cell.y * edge, cell.z), 0).rgb;
}

vec3 displayLut(vec3 color) {
    if (LutStrength <= 0.0 || LutSize < 2.0) return color;
    int edge = int(LutSize);
    vec3 cell = clamp(color, 0.0, 1.0) * float(edge - 1);
    ivec3 lo = ivec3(floor(cell));
    ivec3 hi = min(lo + 1, ivec3(edge - 1));
    vec3 f = fract(cell);
    vec3 aa = mix(fetchLut(lo, edge), fetchLut(ivec3(hi.x, lo.yz), edge), f.x);
    vec3 ab = mix(fetchLut(ivec3(lo.x, hi.y, lo.z), edge), fetchLut(ivec3(hi.xy, lo.z), edge), f.x);
    vec3 ba = mix(fetchLut(ivec3(lo.xy, hi.z), edge), fetchLut(ivec3(hi.x, lo.y, hi.z), edge), f.x);
    vec3 bb = mix(fetchLut(ivec3(lo.x, hi.yz), edge), fetchLut(hi, edge), f.x);
    return mix(color, mix(mix(aa, ab, f.y), mix(ba, bb, f.y), f.z), LutStrength);
}

vec3 assistColorVision(vec3 color) {
    if (ColorVision < 0.5) return color;
    float redGreen = color.r - color.g;
    float blueYellow = color.b - dot(color.rg, vec2(0.23, 0.77));
    vec3 correction;
    if (ColorVision < 1.5) correction = vec3(0.0, redGreen * 0.10, redGreen * 0.52);
    else if (ColorVision < 2.5) correction = vec3(redGreen * 0.15, 0.0, -redGreen * 0.48);
    else correction = vec3(blueYellow * 0.54, -blueYellow * 0.11, 0.0);
    correction -= dot(correction, LUMA);
    float headroom = 1.0;
    for (int channel = 0; channel < 3; channel++) {
        float d = correction[channel];
        if (d > 0.0) headroom = min(headroom, (1.0 - color[channel]) / d);
        if (d < 0.0) headroom = min(headroom, -color[channel] / d);
    }
    return color + correction * clamp(headroom, 0.0, 1.0);
}

void main() {
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 outputPixel = 1.0 / OutputSize;
    float aspect = OutputSize.x / OutputSize.y;
    bool protectedPixel = protectedCoverage(texCoord);
    vec2 uv = clamp(texCoord + (protectedPixel ? vec2(0.0) : wetLensOffset(texCoord)), texel * 0.5, vec2(1.0) - texel * 0.5);
    vec3 center = sampleScene(uv);
    vec2 optics = AutoFocus > 0.5 || FlareStrength > 0.0 ? texelFetch(OpticsSampler, ivec2(0), 0).rg : vec2(FocusDistance, 0.0);
    float centerRaw;
    vec3 view = viewPosition(uv, centerRaw);
    float centerDepth = max(0.0, -view.z);
    vec3 color = center;
    if (!protectedPixel) {
        if (ChromaticAberration > 0.0) {
            vec2 offset = (uv - 0.5) * 2.0 * outputPixel * ChromaticAberration;
            vec3 red = texture(DiffuseSampler, clamp(uv + offset, texel * 0.5, 1.0 - texel * 0.5)).rgb;
            vec3 blue = texture(DiffuseSampler, clamp(uv - offset, texel * 0.5, 1.0 - texel * 0.5)).rgb;
            color = vec3(red.r, center.g, blue.b);
        }
        vec3 motion = motionBlur(uv, center, view, centerRaw);
        vec3 focused = depthFocus(uv, center, centerDepth, AutoFocus > 0.5 ? optics.x : FocusDistance);
        color = max(color + (motion - center) + (focused - center), vec3(0.0));
    }
    color += texture(BloomSampler, uv).rgb * BloomStrength;
    color += lensFlare(uv, optics.y, aspect);

    float fogAmount = 1.0 - exp(-FogStrength * min(centerDepth, FogDistance));
    color = mix(color, FogColor, fogAmount);

    if (Outline > 0.0 && !protectedPixel) {
        vec2 depthStep = texel * max(OutlineWidth, 1.0);
        float depthEdge = abs(centerDepth - linearDepth(uv + vec2(depthStep.x, 0.0)));
        depthEdge += abs(centerDepth - linearDepth(uv + vec2(0.0, depthStep.y)));
        float outlineMask = smoothstep(0.08, 0.7, depthEdge / max(centerDepth, 0.5)) * clamp(Outline, 0.0, 1.0);
        color = mix(color, OutlineColor, outlineMask);
    }

    if (Underwater > 0.5) {
        float waterFog = 1.0 - exp(-max(UnderwaterDensity, 0.0) * centerDepth);
        color = mix(color, vec3(0.055, 0.31, 0.39), clamp(waterFog, 0.0, 0.94));
    }
    color += vec3(0.72, 0.82, 1.0) * Thunder * FlashLimit * 0.16;

    float ev = AutoExposure > 0.5 ? texelFetch(ExposureSampler, ivec2(0), 0).r : 0.0;
    color = max(color * (Exposure * exp2(ev)), vec3(0.0));
    float warmth = clamp((6500.0 - Temperature) / 4500.0, -1.0, 1.0);
    color *= vec3(1.0 + warmth * 0.10 + Tint * 0.02, 1.0 - abs(warmth) * 0.025 - Tint * 0.035, 1.0 - warmth * 0.10 + Tint * 0.02);
    float luminance = dot(color, LUMA);
    color = mix(vec3(luminance), color, Saturation);
    if (PaletteStrength > 0.0 && !protectedPixel && luminance > 0.001) {
        vec3 chroma = round(color / luminance * 7.0) / 7.0;
        vec3 palette = chroma * (luminance / max(dot(chroma, LUMA), 0.001));
        color = mix(color, palette, PaletteStrength);
    }
    color = (color - 0.18) * Contrast + 0.18;
    float tone = dot(color, LUMA);
    color *= mix(ShadowTint, HighlightTint, smoothstep(0.04, 0.75, tone));

    if (Sharpen > 0.0 && !protectedPixel && DofEnabled < 0.5 && Shutter <= 0.0) {
        vec3 l = texture(DiffuseSampler, uv - vec2(texel.x, 0.0)).rgb;
        vec3 r = texture(DiffuseSampler, uv + vec2(texel.x, 0.0)).rgb;
        vec3 b = texture(DiffuseSampler, uv - vec2(0.0, texel.y)).rgb;
        vec3 t = texture(DiffuseSampler, uv + vec2(0.0, texel.y)).rgb;
        vec3 low = min(center, min(min(l, r), min(b, t)));
        vec3 high = max(center, max(max(l, r), max(b, t)));
        vec3 detail = clamp((center * 4.0 - l - r - b - t) * Sharpen * 0.18, low - center, high - center);
        color += detail * (Exposure * exp2(ev));
    }

    color = ToneMapper > 0.5 ? neutral(color) : filmic(color);
    color = displayLut(color);
    color = pow(max(color, vec3(0.0)), vec3(1.0 / max(Gamma, 0.01)));
    float gray = dot(color, LUMA);
    color = mix(color, color * (1.0 - 0.25 * MinimumContrast)
            + vec3(0.25 + 0.25 * gray) * MinimumContrast, (1.0 - smoothstep(0.0, 0.45, gray)));
    color = assistColorVision(clamp(color, 0.0, 1.0));

    vec2 centered = (texCoord * 2.0 - 1.0) * vec2(aspect, 1.0);
    float edge = smoothstep(0.3, 1.0, dot(centered, centered) / (aspect * aspect + 1.0));
    color *= 1.0 - edge * Vignette;
    float noise = hash12(gl_FragCoord.xy + (ReducedMotion > 0.5 ? 0.0 : floor(Time * 60.0))) - 0.5;
    color = clamp(color + noise * Grain * 0.035, 0.0, 1.0);
    fragColor = vec4(DeferDisplayEncoding > 0.5 ? color : encodeDisplayColor(color), 1.0);
}
