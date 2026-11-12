#version 150

#moj_import <fog.glsl>

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float EmissionStrength;

in float fogDistance;
in vec4 boltColor;
out vec4 emissionAndDepth;

vec3 decodeSrgb(vec3 value) {
    return mix(value / 12.92, pow((value + 0.055) / 1.055, vec3(2.4)), greaterThan(value, vec3(0.04045)));
}

void main() {
    float fog = linear_fog_fade(fogDistance, FogStart, FogEnd);
    vec4 authored = boltColor * ColorModulator;
    float visible = clamp(authored.a, 0.0, 1.0) * fog * fog;
    vec3 radiance = decodeSrgb(max(authored.rgb, vec3(0.0))) * visible * EmissionStrength;
    if (max(radiance.r, max(radiance.g, radiance.b)) <= 0.00001) discard;
    emissionAndDepth = vec4(radiance, gl_FragCoord.z);
}
