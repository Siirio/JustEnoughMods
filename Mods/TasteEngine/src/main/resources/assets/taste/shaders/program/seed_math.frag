#version 330 core
// Adapter must expand these local includes before compiling; GLSL 330 has no standard #include directive.
#include "../include/brdf.glsl"
#include "../include/atmosphere.glsl"
#include "../include/normals_depth.glsl"
#include "../include/labpbr.glsl"
#include "../include/water_waves.glsl"
in vec2 uv;
layout(location = 0) out vec4 color;
uniform mat4 inverseProjection;
void main() {
    TasteMaterial m = tasteDecodeLabPbr(vec4(0.5, 10.0 / 255.0, 0.0, 1.0), vec3(0.4, 0.6, 0.8));
    vec2 directions[4] = vec2[4](vec2(1,0), vec2(0,1), vec2(0.6,0.8), vec2(-0.8,0.6));
    vec3 n;
    float height = tasteWaterHeight(uv, vec4(0.02), vec4(1,2,3,4), directions, vec4(0), n);
    n = tasteDecodeOctahedral(tasteEncodeOctahedral(n));
    vec3 radiance = tasteEvaluateBRDF(vec3(0.4,0.6,0.8), m.f0, m.metallic, m.linearRoughness,
        n, normalize(vec3(0,1,1)), normalize(vec3(1,1,0))) * m.reflectionTint;
    vec3 viewPosition;
    bool finiteDepth = tasteReconstructView(uv, 0.5, inverseProjection, viewPosition);
    float distanceBlocks = finiteDepth ? length(viewPosition) : 0.0;
    vec3 sigmaT = vec3(0.02);
    radiance = radiance * tasteBeerLambert(sigmaT, distanceBlocks)
        + vec3(0.01) * tasteHenyeyGreenstein(0.2, 0.4) * tasteFogIntegral(sigmaT, distanceBlocks);
    color = vec4(radiance + vec3(max(height, 0.0) * 0.01), 1.0);
}
