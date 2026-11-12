#version 330 core
#moj_import <taste:taste_wind.glsl>

layout(location = 0) in vec3 Position;
layout(location = 2) in vec2 UV0;
layout(location = 4) in vec4 Normal;
uniform mat4 LightProjection;
uniform vec3 SectionOrigin;
out vec2 texCoord;

void main() {
    gl_Position = LightProjection * vec4(taste_deform(Position + SectionOrigin, int(round(Normal.w * 127.0))), 1.0);
    texCoord = UV0;
}
