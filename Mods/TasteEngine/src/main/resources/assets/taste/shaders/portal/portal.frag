#version 330 core
uniform float time;
uniform float emission;
uniform float distortion;
in vec2 texCoord;
out vec4 color;
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
void main() {
    vec2 centered = texCoord - 0.5;
    float angle = atan(centered.y, centered.x);
    float radius = length(centered);
    float bands = sin(angle * 6.0 - radius * 32.0 + time * 2.0) * 0.5 + 0.5;
    float stars = step(0.985, hash(floor(texCoord * 96.0 + time * 0.1)));
    vec3 body = mix(vec3(0.08, 0.01, 0.2), vec3(0.42, 0.08, 0.82), bands);
    color = vec4(body * emission + stars * vec3(0.45, 0.85, 1.0), 0.82);
}
