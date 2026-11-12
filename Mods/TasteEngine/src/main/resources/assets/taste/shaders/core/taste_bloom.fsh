#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D FineSampler;
uniform sampler2D DepthSampler;
uniform vec2 InSize;
uniform float Mode;
uniform float Threshold;
uniform float Knee;
uniform float SkyWeight;
uniform float Radius;
uniform float Anamorphic;

in vec2 texCoord;
out vec4 fragColor;

vec3 sampleBloom(vec2 uv) {
    vec3 color = max(texture(DiffuseSampler, uv).rgb, vec3(0.0));
    if (Mode >= 0.5) return color;
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float knee = Threshold * Knee;
    float soft = clamp(luminance - Threshold + knee, 0.0, 2.0 * knee);
    soft = soft * soft / max(4.0 * knee, 1.0e-5);
    float contribution = max(luminance - Threshold, soft) / max(luminance, 1.0e-5);
    float sky = step(1.0, texture(DepthSampler, uv).r);
    return color * contribution * mix(1.0, SkyWeight, sky);
}

void main() {
    vec2 axis = vec2(exp2(Anamorphic), exp2(-Anamorphic));
    vec2 stepSize = axis * mix(0.5, 2.0, Radius) / InSize;
    vec3 filtered = vec3(0.0);
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            float weight = (x == 0 ? 2.0 : 1.0) * (y == 0 ? 2.0 : 1.0) / 16.0;
            filtered += sampleBloom(texCoord + vec2(x, y) * stepSize) * weight;
        }
    }
    if (Mode > 1.5) filtered = mix(texture(FineSampler, texCoord).rgb, filtered, 0.5);
    fragColor = vec4(filtered, 1.0);
}
