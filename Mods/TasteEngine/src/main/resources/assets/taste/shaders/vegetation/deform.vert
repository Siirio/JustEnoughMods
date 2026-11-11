#version 330 core
layout(location = 0) in vec3 position;
layout(location = 1) in float anchor;
uniform mat4 modelViewProjection;
uniform vec3 worldOrigin;
uniform vec3 wind;
uniform float time;
uniform float strength;
void main() {
    vec3 world = position + worldOrigin;
    float gust = sin(world.x * 0.41 + world.z * 0.37 + time * 1.7) * 0.5 + 0.5;
    vec3 offset = wind * strength * gust * (1.0 - anchor);
    gl_Position = modelViewProjection * vec4(position + offset, 1.0);
}
