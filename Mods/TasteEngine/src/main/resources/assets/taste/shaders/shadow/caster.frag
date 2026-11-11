#version 330 core
uniform sampler2D albedoSampler;
uniform float cutout;
in vec2 texCoord;
void main() { if (texture(albedoSampler, texCoord).a < cutout) discard; }
