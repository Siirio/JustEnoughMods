#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 InverseFrameView;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform vec3 WorldLightView;

out vec2 texCoord;
out vec4 vertexColor;
out vec4 nativeColor;
out vec4 lightmap;
out vec4 overlay;
out vec3 viewPosition;
out vec3 viewNormal;
out vec3 viewLight;
out vec3 worldPosition;
out vec3 worldNormal;
out float skyVisibility;
out float fogDistance;
flat out ivec2 materialBits;
flat out ivec2 surfaceBits;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    viewPosition = view.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    viewLight = mat3(ModelViewMat) * WorldLightView;
    worldPosition = (InverseFrameView * vec4(Position, 1.0)).xyz;
    worldNormal = mat3(InverseFrameView) * Normal;
    fogDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
    texCoord = UV0;
    vertexColor = Color;
    nativeColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    materialBits = UV1;
    surfaceBits = UV2 & ivec2(15);
    ivec2 light = clamp(UV2 / 16, ivec2(0), ivec2(15));
    lightmap = texelFetch(Sampler2, light, 0);
    overlay = texelFetch(Sampler1, UV1 & ivec2(15), 0);
    skyVisibility = float(light.y) / 15.0;
}
