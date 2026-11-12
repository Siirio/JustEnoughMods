#version 330 core

layout(location = 0) in vec3 CurrentPosition;
layout(location = 1) in vec3 PreviousPosition;
layout(location = 2) in vec2 BodyUv;
layout(location = 3) in float VertexAlpha;

uniform mat4 CurrentProjection;
uniform mat4 CurrentUnjittered;
uniform mat4 PreviousUnjittered;

out vec2 texCoord;
out float authoredAlpha;
out vec4 currentClip;
out vec4 previousClip;
out float previousDepth;

void main() {
    vec4 current = vec4(CurrentPosition, 1.0);
    vec4 previous = vec4(PreviousPosition, 1.0);
    gl_Position = CurrentProjection * current;
    currentClip = CurrentUnjittered * current;
    previousClip = PreviousUnjittered * previous;
    previousDepth = -PreviousPosition.z;
    texCoord = BodyUv;
    authoredAlpha = VertexAlpha;
}
