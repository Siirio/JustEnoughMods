#version 330 core
uniform sampler2D lavaSampler;
uniform float time;
uniform float crust;
uniform float emission;
in vec2 texCoord;
out vec4 color;
float noise(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    vec2 flow = texCoord + vec2(time * 0.012, -time * 0.008);
    vec3 base = texture(lavaSampler, flow).rgb;
    float cracks = smoothstep(crust, 1.0, noise(floor(flow * 48.0)));
    vec3 radiance = mix(base * 0.22, base * emission, cracks);
    color = vec4(max(radiance, vec3(0.0)), 1.0);
}
