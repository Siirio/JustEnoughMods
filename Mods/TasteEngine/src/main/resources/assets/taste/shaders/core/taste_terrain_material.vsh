#version 330 core

#moj_import <taste:taste_wind.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform mat4 ViewProjection;
uniform mat4 MotionProjection;
uniform mat4 PreviousViewProjection;
uniform vec3 SectionOrigin;
out vec4 color;
out vec2 uv;
out vec3 world;
out vec3 normal;
out vec2 lights;
out float meshAo;
out vec4 currentClip;
out vec4 previousClip;
out float deformation;
flat out int materialId;
flat out int layer;
flat out int rainExposure;
flat out int roofDistance;

void main() {
    vec3 undeformed = Position + SectionOrigin;
    int wind = (UV1.y >> 11) & 31;
    world = taste_deform(undeformed, wind);
    deformation = length(world - undeformed);
    currentClip = MotionProjection * vec4(world, 1.0);
    previousClip = PreviousViewProjection * vec4(taste_deform_previous(undeformed, wind), 1.0);
    gl_Position = ViewProjection * vec4(world, 1.0);
    materialId = UV1.x & 65535;
    layer = (UV1.y >> 9) & 3;
    rainExposure = (UV1.y >> 8) & 1;
    meshAo = float(UV1.y & 255) / 255.0;
    normal = Normal;
    lights = vec2(UV2.x, UV2.y & 255) / 240.0;
    roofDistance = (UV2.y >> 8) & 255;
    uv = UV0;
    color = Color;
}
