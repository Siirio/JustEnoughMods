#version 430 core

out vec2 texCoord;
void main() {
    vec2 uv = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    texCoord = uv;
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
}
