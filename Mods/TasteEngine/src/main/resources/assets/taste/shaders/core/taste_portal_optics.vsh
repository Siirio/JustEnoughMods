#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ViewProjection;
out vec2 planeUv;
flat out float endPortal;

void main() {
    gl_Position = ViewProjection * vec4(Position, 1.0);
    planeUv = UV0;
    endPortal = Color.r;
}
