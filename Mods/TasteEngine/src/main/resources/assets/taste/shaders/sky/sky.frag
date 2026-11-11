#version 330 core
uniform vec3 sunDirection;
uniform vec3 sunColor;
uniform vec3 zenithColor;
uniform vec3 horizonColor;
uniform float starStrength;
uniform float time;
in vec3 viewDirection;
out vec4 color;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
void main() {
    vec3 direction = normalize(viewDirection);
    float horizon = pow(clamp(direction.y * 0.5 + 0.5, 0.0, 1.0), 0.45);
    vec3 sky = mix(horizonColor, zenithColor, horizon);
    float sun = pow(max(dot(direction, normalize(sunDirection)), 0.0), 900.0);
    vec2 starCell = floor(direction.xz / max(abs(direction.y), 0.05) * 900.0);
    float star = step(0.9975, hash(starCell)) * starStrength * smoothstep(0.1, -0.15, sunDirection.y);
    color = vec4(max(sky + sunColor * sun + star, vec3(0.0)), 1.0);
}
