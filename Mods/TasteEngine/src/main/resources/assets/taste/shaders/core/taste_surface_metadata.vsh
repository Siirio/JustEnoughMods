#version 330 core

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform mat4 ViewProjection;
uniform vec3 SectionOrigin;
out vec2 texCoord;
out vec3 normal;
out vec4 color;
flat out ivec2 material;
flat out int alphaCutoff;
flat out int optics;

void main() {
    gl_Position = ViewProjection * vec4(Position + SectionOrigin, 1.0);
    texCoord = UV0;
    normal = Normal;
    color = Color;
    material = UV1;
    alphaCutoff = UV2.x & 65535;
    optics = UV2.y & 65535;
}
