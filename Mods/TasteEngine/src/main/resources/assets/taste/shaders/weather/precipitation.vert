#version 330 core
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec3 velocity;
uniform mat4 viewProjection;
uniform float time;
out vec2 texCoord;
void main() { gl_Position = viewProjection * vec4(position + velocity * time, 1.0); texCoord = uv; }
