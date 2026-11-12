#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PreviousSampler;
uniform float HasHistory;
uniform float DeltaSeconds;
uniform float MinEV;
uniform float MaxEV;
uniform float LightSeconds;
uniform float DarkSeconds;

out vec4 fragColor;

void main() {
    float logSum = 0.0;
    for (int y = 0; y < 16; ++y) {
        for (int x = 0; x < 16; ++x) {
            vec2 uv = (vec2(x, y) + 0.5) / 16.0;
            vec3 color = texture(DiffuseSampler, uv).rgb;
            float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
            logSum += log2(max(luminance, 0.0001));
        }
    }
    float target = clamp(log2(0.18) - logSum / 256.0, min(MinEV, MaxEV), max(MinEV, MaxEV));
    float exposure = target;
    if (HasHistory > 0.5) {
        float previous = texelFetch(PreviousSampler, ivec2(0), 0).r;
        float seconds = target < previous ? LightSeconds : DarkSeconds;
        float weight = 1.0 - exp(-max(DeltaSeconds, 0.0) / max(seconds, 0.01));
        exposure = mix(previous, target, weight);
    }
    fragColor = vec4(exposure, 0.0, 0.0, 1.0);
}
