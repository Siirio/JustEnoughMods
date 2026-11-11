#version 330 core
uniform sampler2D spriteSampler;
uniform vec4 tint;
in vec2 texCoord;
out vec4 color;
void main() { vec4 sprite = texture(spriteSampler, texCoord) * tint; if (sprite.a < 0.02) discard; color = sprite; }
