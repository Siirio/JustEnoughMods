#version 330 core
uniform sampler2D decalSampler;
uniform vec4 tint;
uniform float age;
in vec2 texCoord;
out vec4 color;
void main() {
    vec4 decal = texture(decalSampler, texCoord) * tint;
    decal.a *= 1.0 - smoothstep(0.72, 1.0, age);
    color = vec4(decal.rgb * decal.a, decal.a);
}
