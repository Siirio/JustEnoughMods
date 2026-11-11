#version 330 core

uniform sampler2D sceneSampler;
uniform sampler2D depthSampler;
uniform vec2 inverseSize;
uniform float intensity;
in vec2 texCoord;
out vec4 indirectLight;

void main() {
    float centerDepth = texture(depthSampler, texCoord).r;
    vec3 sum = vec3(0.0);
    float weight = 0.0;
    const int samples = 16;
    for (int i = 0; i < samples; i++) {
        float angle = 2.39996323 * float(i);
        float radius = sqrt((float(i) + 0.5) / float(samples)) * 24.0;
        vec2 uv = texCoord + vec2(cos(angle), sin(angle)) * radius * inverseSize;
        float sampleDepth = texture(depthSampler, uv).r;
        float valid = step(centerDepth, sampleDepth + 0.003);
        float sampleWeight = valid / (1.0 + radius * 0.12);
        sum += texture(sceneSampler, uv).rgb * sampleWeight;
        weight += sampleWeight;
    }
    indirectLight = vec4(max(sum / max(weight, 1.0e-4) * intensity, vec3(0.0)), 1.0);
}
