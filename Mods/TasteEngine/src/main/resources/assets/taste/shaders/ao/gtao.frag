#version 330 core

uniform sampler2D depthSampler;
uniform sampler2D normalSampler;
uniform vec2 inverseSize;
uniform float radius;
uniform float strength;
in vec2 texCoord;
out float ambientOcclusion;

void main() {
    float center = texture(depthSampler, texCoord).r;
    vec3 normal = texture(normalSampler, texCoord).xyz * 2.0 - 1.0;
    float occlusion = 0.0;
    const int directions = 8;
    for (int i = 0; i < directions; i++) {
        float angle = float(i) * 6.2831853 / float(directions);
        vec2 direction = vec2(cos(angle), sin(angle));
        for (int step = 1; step <= 4; step++) {
            vec2 offset = direction * inverseSize * radius * float(step);
            float sampleDepth = texture(depthSampler, texCoord + offset).r;
            float delta = center - sampleDepth;
            occlusion += smoothstep(0.0002, 0.012, delta) * max(0.0, 1.0 - abs(dot(normal.xy, direction)));
        }
    }
    ambientOcclusion = clamp(1.0 - occlusion / 32.0 * strength, 0.0, 1.0);
}
