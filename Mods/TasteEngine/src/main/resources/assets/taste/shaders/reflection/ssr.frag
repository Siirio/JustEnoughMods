#version 330 core
uniform sampler2D sceneSampler;
uniform sampler2D depthSampler;
uniform sampler2D normalSampler;
uniform mat4 projection;
uniform mat4 inverseProjection;
uniform int maximumSteps;
uniform float maximumDistance;
uniform float thickness;
in vec2 texCoord;
out vec4 reflection;
vec3 reconstruct(vec2 uv, float depth) { vec4 p = inverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0); return p.xyz / max(abs(p.w), 1.0e-5); }
void main() {
    float depth = texture(depthSampler, texCoord).r;
    vec3 origin = reconstruct(texCoord, depth);
    vec3 normal = normalize(texture(normalSampler, texCoord).xyz * 2.0 - 1.0);
    vec3 direction = reflect(normalize(origin), normal);
    vec3 point = origin;
    float confidence = 0.0;
    vec3 radiance = vec3(0.0);
    for (int step = 0; step < maximumSteps; step++) {
        point += direction * maximumDistance / float(maximumSteps);
        vec4 clip = projection * vec4(point, 1.0);
        vec3 ndc = clip.xyz / max(abs(clip.w), 1.0e-5);
        vec2 uv = ndc.xy * 0.5 + 0.5;
        if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) break;
        float sceneDepth = texture(depthSampler, uv).r;
        if (ndc.z * 0.5 + 0.5 > sceneDepth && ndc.z * 0.5 + 0.5 < sceneDepth + thickness) {
            radiance = texture(sceneSampler, uv).rgb;
            confidence = 1.0 - float(step) / float(maximumSteps);
            break;
        }
    }
    reflection = vec4(radiance, confidence);
}
