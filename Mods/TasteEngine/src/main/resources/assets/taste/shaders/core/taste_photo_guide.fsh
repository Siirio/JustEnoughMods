#version 150

uniform sampler2D PhotoColor;
uniform sampler2D CurrentDepth;
uniform sampler2D FocusSampler;
uniform mat4 InverseProjection;
uniform vec2 WorldJitter;
uniform vec2 OutSize;
uniform float AutoFocus;
uniform float FocusDistance;
uniform float FocalLength;
uniform float Aperture;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

void main() {
    vec3 color = texture(PhotoColor, texCoord).rgb;
    fragColor = vec4(color, 1.0);
    vec2 depthUv = texCoord + WorldJitter;
    float raw = texture(CurrentDepth, depthUv).r;
    if (raw >= 0.999999) return;
    vec4 view = InverseProjection * vec4(depthUv * 2.0 - 1.0, raw * 2.0 - 1.0, 1.0);
    float depth = max(0.05, -view.z / max(view.w, 1.0e-6));
    float focus = max(AutoFocus > 0.5 ? texelFetch(FocusSampler, ivec2(0), 0).r : FocusDistance, FocalLength + 0.0001);
    float coc = FocalLength * FocalLength * abs(depth - focus)
            / max(Aperture * depth * (focus - FocalLength), 1.0e-8) * OutSize.y / 0.048;
    if (coc > 0.85) return;

    vec2 stepUv = 1.0 / OutSize;
    float horizontal = dot(texture(PhotoColor, texCoord + vec2(stepUv.x, 0.0)).rgb
            - texture(PhotoColor, texCoord - vec2(stepUv.x, 0.0)).rgb, LUMA);
    float vertical = dot(texture(PhotoColor, texCoord + vec2(0.0, stepUv.y)).rgb
            - texture(PhotoColor, texCoord - vec2(0.0, stepUv.y)).rgb, LUMA);
    float edge = smoothstep(0.09, 0.21, length(vec2(horizontal, vertical)));
    fragColor.rgb = mix(color, vec3(0.19, 1.0, 0.49), edge * (1.0 - smoothstep(0.25, 0.85, coc)) * 0.78);
}
