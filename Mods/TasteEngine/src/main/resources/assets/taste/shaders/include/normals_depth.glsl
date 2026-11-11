#ifndef TASTE_NORMALS_DEPTH_GLSL
#define TASTE_NORMALS_DEPTH_GLSL
vec2 tasteSignNotZero(vec2 v) { return vec2(v.x >= 0.0 ? 1.0 : -1.0, v.y >= 0.0 ? 1.0 : -1.0); }
vec2 tasteEncodeOctahedral(vec3 n) {
    n /= max(abs(n.x) + abs(n.y) + abs(n.z), 1e-20);
    vec2 p = n.xy;
    if (n.z < 0.0) p = (1.0 - abs(p.yx)) * tasteSignNotZero(p);
    return p * 0.5 + 0.5;
}
vec3 tasteDecodeOctahedral(vec2 encoded) {
    vec2 p = encoded * 2.0 - 1.0;
    vec3 n = vec3(p, 1.0 - abs(p.x) - abs(p.y));
    if (n.z < 0.0) n.xy = (1.0 - abs(n.yx)) * tasteSignNotZero(n.xy);
    return normalize(n);
}
// Standard OpenGL depth: [0,1] -> NDC [-1,1]. UV is lower-left origin. NOT reversed-Z.
// inverseProjection must invert the actual (including jitter) projection that wrote this depth.
bool tasteReconstructView(vec2 uv, float deviceDepth, mat4 inverseProjection, out vec3 viewPosition) {
    vec4 h = inverseProjection * vec4(uv * 2.0 - 1.0, deviceDepth * 2.0 - 1.0, 1.0);
    if (abs(h.w) < 1e-8) { viewPosition = vec3(0.0); return false; }
    viewPosition = h.xyz / h.w;
    return true;
}
#endif
