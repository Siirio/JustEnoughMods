#version 150

#moj_import <fog.glsl>
#moj_import <taste:brdf.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec3 WorldLightColor;
uniform vec3 WorldSkyColor;
uniform float WorldLightStrength;
uniform float WorldSkyStrength;
uniform float SkinScatter;
uniform float RimStrength;
uniform float RimWidth;
uniform float FlashStrength;
uniform vec3 SurfaceTint;
uniform vec3 EmissionTint;
uniform sampler2D EntityShadow0;
uniform sampler2D EntityShadow1;
uniform sampler2D EntityShadow2;
uniform sampler2D EntityShadow3;
uniform mat4 EntityShadowProjection0;
uniform mat4 EntityShadowProjection1;
uniform mat4 EntityShadowProjection2;
uniform mat4 EntityShadowProjection3;
uniform float EntityShadowFar0;
uniform float EntityShadowFar1;
uniform float EntityShadowFar2;
uniform float EntityShadowFar3;
uniform int EntityCascadeCount;
uniform float EntityShadowBias;
uniform float EntityShadowNormalBias;
uniform int EntityShadowSamples;

in vec2 texCoord;
in vec4 vertexColor;
in vec4 nativeColor;
in vec4 lightmap;
in vec4 overlay;
in vec3 viewPosition;
in vec3 viewNormal;
in vec3 viewLight;
in vec3 worldPosition;
in vec3 worldNormal;
in float skyVisibility;
in float fogDistance;
flat in ivec2 materialBits;
flat in ivec2 surfaceBits;

out vec4 fragColor;

vec3 decodeSrgb(vec3 value) {
    return mix(value / 12.92, pow((value + 0.055) / 1.055, vec3(2.4)), greaterThan(value, vec3(0.04045)));
}

vec3 encodeSrgb(vec3 value) {
    return mix(value * 12.92, 1.055 * pow(value, vec3(1.0 / 2.4)) - 0.055, greaterThan(value, vec3(0.0031308)));
}

float scalarField(int bits, int shift) {
    return float((bits >> shift) & 15) / 15.0;
}

float cascadeFar(int index) {
    if (index == 0) return EntityShadowFar0;
    if (index == 1) return EntityShadowFar1;
    if (index == 2) return EntityShadowFar2;
    return EntityShadowFar3;
}

vec3 shadowPoint(int index, vec3 position) {
    vec4 world = vec4(position, 1.0);
    if (index == 0) return (EntityShadowProjection0 * world).xyz * 0.5 + 0.5;
    if (index == 1) return (EntityShadowProjection1 * world).xyz * 0.5 + 0.5;
    if (index == 2) return (EntityShadowProjection2 * world).xyz * 0.5 + 0.5;
    return (EntityShadowProjection3 * world).xyz * 0.5 + 0.5;
}

float storedDepth(int index, vec2 uv) {
    if (index == 0) return texture(EntityShadow0, uv).r;
    if (index == 1) return texture(EntityShadow1, uv).r;
    if (index == 2) return texture(EntityShadow2, uv).r;
    return texture(EntityShadow3, uv).r;
}

vec2 shadowTexel(int index) {
    if (index == 0) return 1.0 / vec2(textureSize(EntityShadow0, 0));
    if (index == 1) return 1.0 / vec2(textureSize(EntityShadow1, 0));
    if (index == 2) return 1.0 / vec2(textureSize(EntityShadow2, 0));
    return 1.0 / vec2(textureSize(EntityShadow3, 0));
}

const vec2 SHADOW_OFFSETS[9] = vec2[](
        vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(-1.0, 0.0), vec2(0.0, 1.0), vec2(0.0, -1.0),
        vec2(1.0, 1.0), vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(-1.0, 1.0));

float cascadeVisibility(int index, vec3 position) {
    vec3 p = shadowPoint(index, position);
    if (any(lessThanEqual(p, vec3(0.0))) || any(greaterThanEqual(p, vec3(1.0)))) return -1.0;
    vec2 texel = shadowTexel(index);
    float visible = 0.0;
    float z = p.z - max(EntityShadowBias, 0.00001);
    int samples = clamp(EntityShadowSamples, 1, 9);
    for (int sampleIndex = 0; sampleIndex < samples; ++sampleIndex) {
        vec2 uv = clamp(p.xy + SHADOW_OFFSETS[sampleIndex] * texel, texel * 0.5, 1.0 - texel * 0.5);
        visible += step(z, storedDepth(index, uv));
    }
    return visible / float(samples);
}

float actorVisibility(vec3 normal) {
    if (EntityCascadeCount <= 0) return 1.0;
    float depth = max(0.0, -viewPosition.z);
    vec3 position = worldPosition + normal * EntityShadowNormalBias;
    for (int index = 0; index < EntityCascadeCount; ++index) {
        float split = cascadeFar(index);
        if (depth > split) continue;
        float visible = cascadeVisibility(index, position);
        if (visible < 0.0) continue;
        if (index + 1 < EntityCascadeCount && depth >= split * 0.88) {
            float adjacent = cascadeVisibility(index + 1, position);
            if (adjacent >= 0.0) visible = mix(visible, adjacent, smoothstep(split * 0.88, split, depth));
        }
        return visible;
    }
    return 1.0;
}

void main() {
    vec4 texel = texture(Sampler0, texCoord);
    if ((materialBits.x & 8192) != 0 && texel.a < 0.1) discard;
    vec3 tint = max(vertexColor.rgb * ColorModulator.rgb, vec3(0.0));
    vec3 albedo = decodeSrgb(texel.rgb * tint) * SurfaceTint;
    vec3 bakedLight = decodeSrgb(lightmap.rgb);
    vec3 nativeLit = decodeSrgb(texel.rgb * nativeColor.rgb * ColorModulator.rgb) * bakedLight * SurfaceTint;
    float alpha = texel.a * nativeColor.a * ColorModulator.a * lightmap.a;
    vec3 radiance = nativeLit;

    float nLength = dot(viewNormal, viewNormal);
    float vLength = dot(viewPosition, viewPosition);
    if (nLength > 1e-6 && vLength > 1e-6) {
        vec3 n = viewNormal * inversesqrt(nLength);
        vec3 v = -viewPosition * inversesqrt(vLength);
        float side = dot(n, v) < 0.0 ? -1.0 : 1.0;
        n *= side;
        float worldLength = dot(worldNormal, worldNormal);
        vec3 normalForShadow = worldLength > 1e-6 ? worldNormal * (side * inversesqrt(worldLength)) : vec3(0.0);
        vec3 l = normalize(viewLight);
        float noV = clamp(dot(n, v), 0.0, 1.0);
        float metal = scalarField(materialBits.x, 8);
        float wet = scalarField(materialBits.y, 8);
        float rough = max(0.06, scalarField(materialBits.x, 4));
        bool skin = (materialBits.x & 4096) != 0;
        bool armorReflection = (materialBits.y & 8192) != 0;
        rough = mix(rough, max(0.07, rough * 0.46), wet * mix(0.56, 0.88, metal));
        float wetColor = 1.0 - wet * mix(skin ? 0.10 : 0.20, 0.04, metal);
        vec3 f0 = mix(vec3(scalarField(materialBits.y, 4) * 0.25), albedo, metal);
        float visibility = actorVisibility(normalForShadow);
        vec3 incident = WorldLightColor * min(3.0, WorldLightStrength)
                * pow(clamp(skyVisibility, 0.0, 1.0), 2.0) * visibility;
        vec3 direct = tasteEvaluateBRDF(albedo * wetColor, f0, metal, rough * rough, n, v, l) * incident;
        radiance = nativeLit * (0.84 * wetColor) + min(vec3(1.3), direct * 0.60);
        if (skin) {
            float wrap = clamp((dot(n, l) + 0.38) / 1.38, 0.0, 1.0) - max(dot(n, l), 0.0);
            float scatter = clamp(SkinScatter * (0.45 + 0.55 * float(surfaceBits.x) / 15.0), 0.0, 1.0);
            radiance += albedo * incident * max(0.0, wrap) * scatter * 0.27;
        }
        vec3 sky = WorldSkyColor * WorldSkyStrength * skyVisibility;
        if (armorReflection) {
            vec3 reflected = reflect(-v, n);
            float glow = 0.32 + 0.68 * (0.5 + reflected.y * 0.5);
            float blur = (1.0 - rough) * (1.0 - rough);
            vec3 environment = sky * glow + incident * blur * pow(max(0.0, dot(reflected, l)), 5.0);
            vec3 reflectedColor = tasteFresnelSchlick(f0, noV) * environment;
            radiance += min(vec3(0.55), reflectedColor * (0.08 + 0.42 * blur));
        }
        if ((materialBits.y & 4096) != 0) {
            float rim = pow(1.0 - noV, max(0.5, RimWidth));
            vec3 rimLight = sky * 0.54 + incident * 0.22 + bakedLight * 0.06;
            radiance += min(vec3(0.55), rimLight * rim * RimStrength * 0.56);
        }
    }

    vec3 emissionColor = EmissionTint.r >= 0.0 ? EmissionTint : albedo;
    radiance += emissionColor * (float(surfaceBits.y) / 5.0);
    vec3 nativeOverlay = decodeSrgb(overlay.rgb) * bakedLight;
    radiance = mix(radiance, nativeOverlay, clamp((1.0 - overlay.a) * FlashStrength, 0.0, 1.0));
    fragColor = linear_fog(vec4(encodeSrgb(max(vec3(0.0), radiance)), alpha),
            fogDistance, FogStart, FogEnd, FogColor);
}
