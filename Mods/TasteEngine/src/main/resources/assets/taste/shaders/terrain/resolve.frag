#version 330 core

uniform sampler2D gAlbedoAo;
uniform sampler2D gNormalDepth;
uniform sampler2D gMaterial;
uniform sampler2D gLightEmission;
uniform vec3 sunDirection;
uniform vec3 sunRadiance;
uniform vec3 skyRadiance;

in vec2 texCoord;
layout(location = 0) out vec4 resolvedColor;

vec3 decodeOctahedral(vec2 encoded) {
    vec2 f = encoded * 2.0 - 1.0;
    vec3 n = vec3(f, 1.0 - abs(f.x) - abs(f.y));
    if (n.z < 0.0) n.xy = (1.0 - abs(n.yx)) * sign(n.xy);
    return normalize(n);
}

void main() {
    vec4 albedoAo = texture(gAlbedoAo, texCoord);
    vec4 normalDepth = texture(gNormalDepth, texCoord);
    vec4 material = texture(gMaterial, texCoord);
    vec4 lightEmission = texture(gLightEmission, texCoord);
    vec3 normal = decodeOctahedral(normalDepth.xy);
    float noL = max(dot(normal, normalize(sunDirection)), 0.0);
    vec3 diffuse = albedoAo.rgb * (sunRadiance * noL + skyRadiance * albedoAo.a);
    vec3 emission = albedoAo.rgb * lightEmission.y * 8.0;
    resolvedColor = vec4(max(diffuse + emission, vec3(0.0)), material.a);
}
