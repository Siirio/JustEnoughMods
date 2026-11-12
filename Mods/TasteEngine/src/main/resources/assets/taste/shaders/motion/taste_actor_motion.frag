#version 330 core

uniform sampler2D WorldDepth;
uniform sampler2D OpaqueDepth;
uniform sampler2D BodyTexture;
uniform int Cutout;
uniform float Validity;

in vec2 texCoord;
in float authoredAlpha;
in vec4 currentClip;
in vec4 previousClip;
in float previousDepth;

out vec4 motionAndDepth;

void main() {
    if (authoredAlpha <= 0.001 || (Cutout != 0 && texture(BodyTexture, texCoord).a < 0.1)) discard;
    const float tolerance = 0.00000024;
    vec2 uv = gl_FragCoord.xy / vec2(textureSize(WorldDepth, 0));
    float depth = gl_FragCoord.z;
    float world = texture(WorldDepth, uv).r;
    float opaque = texture(OpaqueDepth, uv).r;
    if (abs(depth - world) > tolerance && (abs(depth - opaque) > tolerance || world < depth - tolerance))
        discard;
    if (Validity < 0.0 || currentClip.w <= 0.0 || previousClip.w <= 0.0 || previousDepth <= 0.0) {
        motionAndDepth = vec4(0.0, 0.0, 0.0, -1.0);
        return;
    }
    vec2 currentUv = currentClip.xy / currentClip.w * 0.5 + 0.5;
    vec2 oldUv = previousClip.xy / previousClip.w * 0.5 + 0.5;
    if (any(lessThan(oldUv, vec2(0.0))) || any(greaterThan(oldUv, vec2(1.0)))) {
        motionAndDepth = vec4(0.0, 0.0, 0.0, -1.0);
        return;
    }
    motionAndDepth = vec4(currentUv - oldUv, previousDepth, 1.0);
}
