#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 TextureMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
uniform int ArmorFog;

out float fogDistance;
out vec2 patternUv;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec3 fogPosition = ArmorFog != 0 ? Position : IViewRotMat * Position;
    fogDistance = fog_distance(ModelViewMat, fogPosition, FogShape);
    patternUv = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
}
