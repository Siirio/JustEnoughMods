#version 330 core

uniform sampler2D albedoSampler;
uniform sampler2D normalSampler;
uniform sampler2D specularSampler;

in vec4 vColor;
in vec2 vUv;
in vec2 vLightAo;
in vec3 vNormal;
flat in uint vMaterialId;

layout(location = 0) out vec4 gAlbedoAo;
layout(location = 1) out vec4 gNormalDepth;
layout(location = 2) out vec4 gMaterial;
layout(location = 3) out vec4 gLightEmission;

vec2 encodeOctahedral(vec3 n) {
    n /= abs(n.x) + abs(n.y) + abs(n.z);
    vec2 p = n.xy;
    if (n.z < 0.0) p = (1.0 - abs(p.yx)) * sign(p);
    return p * 0.5 + 0.5;
}

void main() {
    vec4 texel = texture(albedoSampler, vUv) * vColor;
    if (texel.a < 0.1) discard;
    vec4 specular = texture(specularSampler, vUv);
    int green = int(floor(clamp(specular.g, 0.0, 1.0) * 255.0 + 0.5));
    int alpha = int(floor(clamp(specular.a, 0.0, 1.0) * 255.0 + 0.5));
    float perceptualRoughness = 1.0 - specular.r;
    float metallic = green >= 230 ? 1.0 : 0.0;
    float emission = alpha == 255 ? 0.0 : float(alpha) / 254.0;
    gAlbedoAo = vec4(texel.rgb, vLightAo.y);
    gNormalDepth = vec4(encodeOctahedral(vNormal), gl_FragCoord.z, 1.0);
    gMaterial = vec4(perceptualRoughness, metallic, float(vMaterialId & 65535u) / 65535.0, texel.a);
    gLightEmission = vec4(vLightAo.x, emission, specular.b, 1.0);
}
