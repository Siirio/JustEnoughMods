#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 2) in vec2 UV0;
uniform mat4 LightProjection;
uniform vec3 SectionOrigin;
out vec2 texCoord;

void main() {
    gl_Position = LightProjection * vec4(Position + SectionOrigin, 1.0);
    texCoord = UV0;
}
