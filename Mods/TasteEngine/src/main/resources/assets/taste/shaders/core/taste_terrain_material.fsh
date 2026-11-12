#version 330 core

uniform sampler2D BlockAtlas;
uniform sampler2D MaterialPalette;
uniform sampler2D NormalAtlas;
uniform sampler2D SpecularAtlas;
uniform sampler2D HeightAtlas;
uniform float NormalStrength;
uniform float GeneratedNormals;
uniform float ParallaxDepth;
uniform float ParallaxSteps;
uniform float AuxOnly;
uniform float RoofCheckRange;
uniform float NativeMipCutoff;
uniform float RoughnessScale;
uniform vec3 Eye;
in vec4 color;
in vec2 uv;
in vec3 world;
in vec3 normal;
in vec2 lights;
in float meshAo;
in vec4 currentClip;
in vec4 previousClip;
in float deformation;
flat in int materialId;
flat in int layer;
flat in int rainExposure;
flat in int roofDistance;
layout(location = 0) out vec4 albedoAo;
layout(location = 1) out vec4 normalSurface;
layout(location = 2) out vec4 emissionFlags;
layout(location = 3) out vec4 motionLight;

vec4 palette(int offset) {
    int address = materialId * 6 + offset;
    return texelFetch(MaterialPalette, ivec2(address & 255, address >> 8), 0);
}

vec3 decodeColor(vec3 rgb) {
    return mix(rgb / 12.92, pow((rgb + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), rgb));
}

vec2 encodeOct(vec3 n) {
    n /= abs(n.x) + abs(n.y) + abs(n.z);
    return n.z >= 0.0 ? n.xy : (1.0 - abs(n.yx)) * mix(vec2(-1.0), vec2(1.0), step(vec2(0.0), n.xy));
}

mat3 tangentBasis(vec3 n) {
    vec3 dx = dFdx(world);
    vec3 dy = dFdy(world);
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    float determinant = du.x * dv.y - du.y * dv.x;
    vec3 tangent = abs(determinant) > 1.0e-10 ? (dx * dv.y - dy * du.y) / determinant
        : cross(abs(n.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0), n);
    tangent = normalize(tangent - n * dot(n, tangent));
    vec3 bitangent = normalize(cross(n, tangent));
    if (determinant < 0.0) bitangent = -bitangent;
    return mat3(tangent, bitangent, n);
}

float heightAt(vec2 coord, vec2 uvDx, vec2 uvDy, bool separate) {
    return separate ? textureGrad(HeightAtlas, coord, uvDx, uvDy).r
                    : textureGrad(NormalAtlas, coord, uvDx, uvDy).a;
}

vec2 parallax(vec2 coord, vec3 localView, vec4 bounds, vec2 margin, vec2 uvDx, vec2 uvDy, bool separate) {
    if (ParallaxDepth <= 0.0 || localView.z <= 0.12) return coord;
    int count = int(clamp(ParallaxSteps, 2.0, 96.0));
    float grazing = smoothstep(0.12, 0.36, localView.z);
    vec2 delta = localView.xy / max(localView.z, 0.18) * ParallaxDepth * grazing
               * (bounds.zw - bounds.xy) / float(count);
    coord = clamp(coord, bounds.xy + margin, bounds.zw - margin);
    float previous = heightAt(coord, uvDx, uvDy, separate) - 1.0;
    if (previous >= 0.0) return coord;
    for (int stepIndex = 1; stepIndex <= count; ++stepIndex) {
        vec2 next = clamp(coord - delta, bounds.xy + margin, bounds.zw - margin);
        float crossing = float(stepIndex) / float(count) + heightAt(next, uvDx, uvDy, separate) - 1.0;
        if (crossing >= 0.0) return mix(coord, next, clamp(-previous / max(crossing - previous, 1.0e-6), 0.0, 1.0));
        coord = next;
        previous = crossing;
    }
    return coord;
}

void main() {
    vec4 material = palette(0);
    vec4 radiant = palette(1);
    vec4 properties = palette(2);
    vec4 companions = palette(3);
    vec4 bounds = palette(4);
    bool separateHeight = palette(5).x > 0.5;
    vec2 margin = min(0.51 / vec2(textureSize(BlockAtlas, 0)), (bounds.zw - bounds.xy) * 0.49);
    vec3 geometric = normalize(normal);
    vec3 deformedNormal = cross(dFdx(world), dFdy(world));
    if (deformation > 0.00001 && dot(deformedNormal, deformedNormal) > 1.0e-12) {
        geometric = normalize(dot(deformedNormal, geometric) < 0.0 ? -deformedNormal : deformedNormal);
    }
    mat3 tangent = tangentBasis(geometric);
    vec2 coord = uv;
    vec2 uvDx = dFdx(uv);
    vec2 uvDy = dFdy(uv);
    float cutoff = max(radiant.a, layer == 2 ? NativeMipCutoff : 0.1);
    if (layer != 1 && textureGrad(BlockAtlas, uv, uvDx, uvDy).a * color.a < cutoff) discard;
    if (separateHeight || companions.x > 0.5) {
        coord = parallax(coord, transpose(tangent) * normalize(Eye - world), bounds, margin, uvDx, uvDy, separateHeight);
    }
    vec4 sampleColor = textureGrad(BlockAtlas, coord, uvDx, uvDy) * color;
    vec3 albedo = decodeColor(sampleColor.rgb);
    float textureAo = 1.0;
    vec3 shading = geometric;
    if (companions.x > 0.5) {
        vec4 sampleNormal = textureGrad(NormalAtlas, coord, uvDx, uvDy);
        vec3 local = sampleNormal.xyz * 2.0 - 1.0;
        local.y *= companions.w;
        if (companions.z < 0.5) {
            local.z = sqrt(max(0.0, 1.0 - dot(local.xy, local.xy)));
            textureAo = sampleNormal.b;
        }
        local.xy *= NormalStrength;
        shading = normalize(tangent * normalize(local));
    } else if (GeneratedNormals > 0.0) {
        vec2 dx = vec2(margin.x * 2.0, 0.0);
        vec2 dy = vec2(0.0, margin.y * 2.0);
        vec3 weights = vec3(0.2126, 0.7152, 0.0722);
        float gx = dot(texture(BlockAtlas, clamp(coord + dx, bounds.xy + margin, bounds.zw - margin)).rgb
            - texture(BlockAtlas, clamp(coord - dx, bounds.xy + margin, bounds.zw - margin)).rgb, weights);
        float gy = dot(texture(BlockAtlas, clamp(coord + dy, bounds.xy + margin, bounds.zw - margin)).rgb
            - texture(BlockAtlas, clamp(coord - dy, bounds.xy + margin, bounds.zw - margin)).rgb, weights);
        shading = normalize(tangent * vec3(-gx * GeneratedNormals, -gy * GeneratedNormals, 1.0));
    }
    float roughness = material.x;
    float f0Byte = round(material.y * 255.0);
    float oldFormat = companions.z;
    if (properties.w >= 230.0) f0Byte = properties.w;
    else if (material.z > 0.0) { f0Byte = round(material.z * 255.0); oldFormat = 1.0; }
    float emission = material.w;
    float porosity = properties.x;
    float thin = properties.y;
    if (companions.y > 0.5) {
        vec4 spec = textureGrad(SpecularAtlas, coord, uvDx, uvDy);
        roughness = (1.0 - spec.r) * RoughnessScale;
        f0Byte = round(spec.g * 255.0);
        oldFormat = companions.z;
        if (companions.z > 0.5) emission += spec.b;
        else {
            float encoded = round(spec.a * 255.0);
            if (encoded < 255.0) emission += encoded / 254.0;
            float blue = round(spec.b * 255.0);
            if (blue <= 64.0) porosity = max(porosity, blue / 64.0);
            else thin = max(thin, (blue - 65.0) / 190.0);
        }
    }
    if (AuxOnly > 0.5) {
        float exposed = roofDistance == 0 || float(roofDistance) > RoofCheckRange ? float(rainExposure) : 0.0;
        albedoAo = vec4(porosity, thin, textureAo, properties.z * exposed);
        return;
    }
    int flags = 1 | (layer << 8);
    if (layer != 1) flags |= 2;
    if (emission > 0.0) flags |= 4;
    if (thin > 0.0) flags |= 8;
    if (properties.z > 0.0) flags |= 16;
    if (deformation > 0.00001) flags |= 32;
    if (oldFormat > 0.5) flags |= 128;
    vec2 motion = vec2(0.0);
    if (previousClip.w > 0.0) motion = (currentClip.xy / currentClip.w - previousClip.xy / previousClip.w) * 0.5;
    albedoAo = vec4(albedo, meshAo);
    normalSurface = vec4(encodeOct(shading), clamp(roughness, 0.02, 1.0), f0Byte);
    emissionFlags = vec4(radiant.rgb * albedo * emission, float(flags));
    motionLight = vec4(motion, clamp(lights, 0.0, 1.0));
}
