#version 330 core
uniform sampler2D sceneSampler;
uniform sampler2D dropletState;
uniform float distortion;
in vec2 patchUv;
out vec4 color;
void main() {
    vec4 state = texture(dropletState, patchUv);
    vec2 gradient = vec2(dFdx(state.r), dFdy(state.r));
    vec3 scene = texture(sceneSampler, patchUv + gradient * distortion).rgb;
    float highlight = pow(max(dot(normalize(vec3(-gradient, 1.0)), normalize(vec3(-0.4, 0.7, 0.6))), 0.0), 24.0);
    color = vec4(scene + highlight * state.a, state.a);
}
