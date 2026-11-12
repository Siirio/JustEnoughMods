#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

uniform mat4 View;
uniform mat4 Projection;
uniform vec3 CameraOrigin;

out vec2 spriteUv;
out vec4 spriteColor;
out vec2 packedLight;
out vec3 particleWorld;
out float particleDepth;
flat out float softClass;
flat out float emissionClass;

void main() {
    vec4 view = View * vec4(Position, 1.0);
    gl_Position = Projection * view;
    spriteUv = UV0;
    spriteColor = Color;
    packedLight = vec2(UV2 & ivec2(255)) / 240.0;
    particleWorld = Position + CameraOrigin;
    particleDepth = -view.z;
    softClass = (UV2.x & 256) == 0 ? 0.0 : 1.0;
    emissionClass = float(UV2.y >> 8) * (4.0 / 127.0);
}
