#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float GlintAlpha;
uniform float EmissionStrength;

in float fogDistance;
in vec2 patternUv;
out vec4 emissionAndDepth;

vec3 decodeSrgb(vec3 value) {
    return mix(value / 12.92, pow((value + 0.055) / 1.055, vec3(2.4)), greaterThan(value, vec3(0.04045)));
}

void main() {
    vec4 mask = texture(Sampler0, patternUv) * ColorModulator;
    if (mask.a < 0.1) discard;
    float visible = linear_fog_fade(fogDistance, FogStart, FogEnd) * GlintAlpha;
    vec3 source = max(mask.rgb * visible, vec3(0.0));
    // The native source-color blend multiplies the pattern by itself before adding it.
    vec3 radiance = min(decodeSrgb(source * source) * EmissionStrength, vec3(8.0));
    if (max(radiance.r, max(radiance.g, radiance.b)) <= 0.00001) discard;
    emissionAndDepth = vec4(radiance, gl_FragCoord.z);
}
