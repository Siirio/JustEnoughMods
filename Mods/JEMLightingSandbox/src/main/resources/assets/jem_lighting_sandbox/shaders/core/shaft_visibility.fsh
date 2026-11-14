#version 150

#moj_import <jem_lighting_sandbox:cutout_shadow.glsl>
#moj_import <jem_lighting_sandbox:world_shadow.glsl>

uniform vec3 MapOrigin;
uniform vec3 MapDirection;
uniform float MapWidth;
uniform float MapReach;

in vec2 sceneUv;
out vec4 fragColor;

void main() {
    vec2 offset = (sceneUv - 0.5) * MapWidth;
    vec3 across = vec3(MapDirection.y, -MapDirection.x, 0.0);
    vec3 rayStart = MapOrigin + across * offset.x + vec3(0.0, 0.0, offset.y);
    vec3 trace = jemRaycast(rayStart, -MapDirection, MapReach, JEM_NO_SOURCE, false);
    fragColor = vec4(trace.yz, trace.x, 1.0);
}
