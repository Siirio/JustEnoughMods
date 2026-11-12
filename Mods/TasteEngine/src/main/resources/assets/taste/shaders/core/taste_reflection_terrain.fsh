#version 150

uniform sampler2D BlockAtlas;
uniform float AlphaCutoff;
in vec2 texCoord;
in vec3 shade;
out vec4 fragColor;

vec3 linearFromSrgb(vec3 value) {
    return mix(value / 12.92, pow((value + 0.055) / 1.055, vec3(2.4)), greaterThan(value, vec3(0.04045)));
}

void main() {
    vec4 block = texture(BlockAtlas, texCoord);
    if (AlphaCutoff > 0.0 && block.a < AlphaCutoff) discard;
    fragColor = vec4(linearFromSrgb(clamp(block.rgb * shade, 0.0, 1.0)), 1.0);
}
