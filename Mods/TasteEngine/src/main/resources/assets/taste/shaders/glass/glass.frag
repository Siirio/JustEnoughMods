#version 330 core
uniform sampler2D opaqueScene;
uniform sampler2D reflectionSampler;
uniform vec3 transmissionColor;
uniform float ior;
uniform float thickness;
uniform float roughness;
in vec2 texCoord;
in vec3 viewNormal;
out vec4 color;
void main() {
    vec3 normal = normalize(viewNormal);
    float fresnel = pow((ior - 1.0) / (ior + 1.0), 2.0);
    fresnel += (1.0 - fresnel) * pow(1.0 - abs(normal.z), 5.0);
    vec2 offset = normal.xy * thickness * (ior - 1.0) * 0.025;
    vec3 transmitted = texture(opaqueScene, texCoord + offset).rgb * transmissionColor;
    vec3 reflected = texture(reflectionSampler, texCoord).rgb;
    color = vec4(mix(transmitted, reflected, clamp(fresnel + roughness * 0.08, 0.0, 1.0)), 0.22 + fresnel * 0.55);
}
