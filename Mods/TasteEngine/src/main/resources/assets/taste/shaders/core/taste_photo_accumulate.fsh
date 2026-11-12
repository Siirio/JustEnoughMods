#version 150

uniform sampler2D CurrentColor;
uniform sampler2D CurrentDepth;
uniform sampler2D HistoryColor;
uniform sampler2D HistoryDepth;
uniform mat4 InverseProjection;
uniform vec2 WorldJitter;
uniform float HasHistory;

in vec2 texCoord;
out vec4 fragColor;

float linearDepth(float depth) {
    vec4 view = InverseProjection * vec4(0.0, 0.0, depth * 2.0 - 1.0, 1.0);
    return -view.z / max(view.w, 1.0e-6);
}

void main() {
    vec2 pixel = 1.0 / vec2(textureSize(CurrentColor, 0));
    vec2 uv = clamp(texCoord + WorldJitter, pixel * 0.5, 1.0 - pixel * 0.5);
    vec3 current = texture(CurrentColor, uv).rgb;
    float depth = texture(CurrentDepth, uv).r;
    gl_FragDepth = depth;
    fragColor = vec4(current, 1.0 / 256.0);
    if (HasHistory < 0.5) return;

    float oldDepth = texture(HistoryDepth, texCoord).r;
    if ((depth >= 0.999999) != (oldDepth >= 0.999999)) return;
    if (depth < 0.999999) {
        float blocks = linearDepth(depth);
        if (abs(blocks - linearDepth(oldDepth)) > max(0.025, blocks * 0.012)) return;
    }
    vec4 old = texture(HistoryColor, texCoord);
    vec3 difference = abs(current - old.rgb) / (0.25 + max(current, old.rgb));
    if (max(difference.r, max(difference.g, difference.b)) > 0.3) return;
    float count = min(old.a * 256.0 + 1.0, 256.0);
    fragColor = vec4(mix(old.rgb, current, 1.0 / count), count / 256.0);
}
