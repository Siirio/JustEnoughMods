#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 uv;
layout(location = 3) in vec2 lightAo;
layout(location = 4) in vec3 normal;
layout(location = 5) in uint materialId;

uniform mat4 modelViewProjection;

out vec4 vColor;
out vec2 vUv;
out vec2 vLightAo;
out vec3 vNormal;
flat out uint vMaterialId;

void main() {
    gl_Position = modelViewProjection * vec4(position, 1.0);
    vColor = color;
    vUv = uv;
    vLightAo = lightAo;
    vNormal = normalize(normal);
    vMaterialId = materialId;
}
