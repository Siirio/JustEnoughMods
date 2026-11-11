#version 330 core
uniform sampler2D opaqueScene;
uniform sampler2D opaqueDepth;
uniform sampler2D reflectionSampler;
uniform vec3 shallowColor;
uniform vec3 deepColor;
uniform float ior;
uniform float absorption;
uniform float clarity;
in vec2 texCoord;
in vec3 viewNormal;
in float waterDepth;
out vec4 color;
void main() {
    vec3 normal = normalize(viewNormal);
    float fresnel = 0.02 + 0.98 * pow(1.0 - abs(normal.z), 5.0);
    vec2 refractedUv = texCoord + normal.xy * (ior - 1.0) * 0.03;
    vec3 refracted = texture(opaqueScene, refractedUv).rgb;
    vec3 reflected = texture(reflectionSampler, texCoord).rgb;
    float transmittance = exp(-absorption * waterDepth / max(clarity, 0.01));
    vec3 medium = mix(shallowColor, deepColor, clamp(waterDepth * 0.08, 0.0, 1.0));
    vec3 transmitted = refracted * transmittance + medium * (1.0 - transmittance);
    color = vec4(mix(transmitted, reflected, fresnel), clamp(0.35 + waterDepth * 0.04, 0.35, 0.92));
}
