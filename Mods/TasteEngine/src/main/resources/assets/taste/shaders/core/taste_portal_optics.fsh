#version 150

uniform sampler2D PortalScene;
uniform sampler2D PortalDepth;
uniform float VisualTime;
uniform float Emission;
uniform float Distortion;

in vec2 planeUv;
flat in float endPortal;
out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / vec2(textureSize(PortalScene, 0));
    float time = VisualTime;
    vec2 phase = planeUv * vec2(0.78539816, 0.98174770);
    vec2 swirl = vec2(sin(phase.y + cos(phase.x + time) - time * 1.35),
                      cos(phase.x + sin(phase.y - time * 0.72) + time));
    float field = 0.5 + 0.5 * sin(phase.x + phase.y + swirl.x - time);
    vec2 shifted = uv + swirl * Distortion * (endPortal > 0.5 ? 0.24 : 0.38);
    bool valid = all(greaterThanEqual(shifted, vec2(0.0))) && all(lessThanEqual(shifted, vec2(1.0)));
    if (valid) valid = texture(PortalDepth, shifted).r + 0.00012 >= gl_FragCoord.z;
    vec3 base = texture(PortalScene, uv).rgb;
    vec3 result = valid ? mix(base, texture(PortalScene, shifted).rgb, 0.33) : base;
    vec3 tint = mix(vec3(0.34, 0.07, 0.63), vec3(0.10, 0.27, 0.30), endPortal);
    result += tint * Emission * mix(0.06, 0.25, field) * mix(1.0, 0.32, endPortal);
    fragColor = vec4(result, 1.0);
}
