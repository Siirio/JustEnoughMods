#version 330 core

uniform sampler2D BlockAtlas;
uniform float AlphaCutoff;
in vec2 texCoord;

void main() {
    if (AlphaCutoff > 0.0 && texture(BlockAtlas, texCoord).a < AlphaCutoff) discard;
}
