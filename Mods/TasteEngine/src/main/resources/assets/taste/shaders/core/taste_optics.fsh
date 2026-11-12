#version 150

uniform sampler2D DepthSampler;
uniform sampler2D PreviousOptics;
uniform sampler2D CloudSampler;
uniform mat4 InverseProjection;
uniform vec2 WorldJitter;
uniform vec2 SourceUv;
uniform float SourceVisibility;
uniform float HasCloudCoverage;
uniform float CrosshairFocus;
uniform float ManualFocus;
uniform float HistoryBlend;

in vec2 texCoord;
out vec4 fragColor;

float focusDepth(vec2 uv) {
    float raw = texture(DepthSampler, uv).r;
    if (raw >= 0.999999) return 0.0;
    vec4 view = InverseProjection * vec4(uv * 2.0 - 1.0, raw * 2.0 - 1.0, 1.0);
    return clamp(-view.z / max(view.w, 1.0e-6), 0.1, 512.0);
}

float medianDepth() {
    float samples[9];
    int count = 0;
    vec2 pixel = 1.0 / vec2(textureSize(DepthSampler, 0));
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float depth = focusDepth(texCoord + WorldJitter + vec2(x, y) * pixel);
            if (depth > 0.0) {
                int position = count;
                while (position > 0) {
                    if (samples[position - 1] <= depth) break;
                    samples[position] = samples[position - 1];
                    position--;
                }
                samples[position] = depth;
                count++;
            }
        }
    }
    if (count < 3) return 0.0;
    return samples[count / 2];
}

float flareVisibility() {
    if (SourceVisibility <= 0.0 || any(lessThanEqual(SourceUv, vec2(0.0)))
            || any(greaterThanEqual(SourceUv, vec2(1.0)))) return 0.0;
    vec2 dimensions = vec2(textureSize(DepthSampler, 0));
    vec2 radius = vec2(dimensions.y / dimensions.x, 1.0) * 0.005;
    float coverage = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 ray = SourceUv + WorldJitter + vec2(x, y) * radius;
            float depth = texture(DepthSampler, ray).r;
            float transmission = HasCloudCoverage > 0.5 ? 1.0 - texture(CloudSampler, ray).a : 1.0;
            coverage += step(0.999999, depth) * clamp(transmission, 0.0, 1.0);
        }
    }
    vec2 edge = min(SourceUv, 1.0 - SourceUv);
    return coverage / 9.0 * SourceVisibility * smoothstep(0.0, 0.055, min(edge.x, edge.y));
}

void main() {
    vec2 previous = HistoryBlend > 0.0 ? texelFetch(PreviousOptics, ivec2(0), 0).rg : vec2(ManualFocus, 0.0);
    float focus = CrosshairFocus > 0.5 ? medianDepth() : ManualFocus;
    if (focus <= 0.0) focus = previous.x;
    fragColor = vec4(mix(focus, previous.x, CrosshairFocus > 0.5 ? HistoryBlend : 0.0),
            mix(flareVisibility(), previous.y, HistoryBlend), 0.0, 1.0);
}
