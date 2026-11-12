#version 150
#moj_import <taste:taste_wind.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec4 Normal;

uniform mat4 CaptureProjection;
uniform vec3 SectionOrigin;
uniform sampler2D NativeLightmap;

out vec2 texCoord;
out vec3 shade;

void main() {
    gl_Position = CaptureProjection * vec4(taste_deform(Position + SectionOrigin, int(round(Normal.w * 127.0))), 1.0);
    texCoord = UV0;
    shade = Color.rgb * texelFetch(NativeLightmap, clamp(UV2 / 16, ivec2(0), ivec2(15)), 0).rgb;
}
