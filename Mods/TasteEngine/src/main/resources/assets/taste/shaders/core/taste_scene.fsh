#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 InSize;
uniform float FarPlane;
uniform float Time;
uniform float Rain;
uniform float SunIntensity;
uniform float SkyIntensity;
uniform float AmbientFloor;
uniform float BlockIntensity;
uniform float WarmColdBalance;
uniform float ShadowStrength;
uniform float ContactShadow;
uniform float AoStrength;
uniform float AoRadius;
uniform float GiStrength;
uniform float EmissionStrength;
uniform float ReflectionStrength;
uniform float CloudCoverage;
uniform float CloudDensity;
uniform float Wetness;
uniform float WaterStrength;
uniform float LavaStrength;
uniform float ReducedMotion;

in vec2 texCoord;
out vec4 fragColor;

float linearDepth(float deviceDepth) {
    float nearPlane = 0.05;
    float z = deviceDepth * 2.0 - 1.0;
    return (2.0 * nearPlane * FarPlane) / max(FarPlane + nearPlane - z * (FarPlane - nearPlane), 0.00001);
}

void main() {
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec3 color = texture(DiffuseSampler, texCoord).rgb;
    float depth = linearDepth(texture(DepthSampler, texCoord).r);
    float dx = abs(depth - linearDepth(texture(DepthSampler, texCoord + vec2(texel.x * max(AoRadius, 0.5), 0.0)).r));
    float dy = abs(depth - linearDepth(texture(DepthSampler, texCoord + vec2(0.0, texel.y * max(AoRadius, 0.5))).r));
    float contact = smoothstep(0.04, 1.2, (dx + dy) / max(depth, 0.5));
    float horizon = smoothstep(0.2, 0.85, texCoord.y);
    float skyLight = mix(AmbientFloor, SkyIntensity, horizon) * 0.04;
    float direct = mix(0.94, 1.08, clamp(SunIntensity / 4.0, 0.0, 1.0));
    float shadow = 1.0 - contact * AoStrength * 0.12 - contact * ShadowStrength * min(ContactShadow, 32.0) * 0.002;
    vec3 temperature = mix(vec3(0.93, 0.97, 1.05), vec3(1.06, 0.98, 0.91), clamp(WarmColdBalance * 0.5 + 0.5, 0.0, 1.0));
    color *= direct * shadow * temperature;
    color += color * skyLight + color * GiStrength * (1.0 - contact) * 0.018;
    float peak = max(color.r, max(color.g, color.b));
    color += color * smoothstep(0.65, 1.0, peak) * EmissionStrength * 0.025;
    float wet = clamp(Wetness * Rain, 0.0, 1.0);
    float reflectedSky = (1.0 - abs(texCoord.y * 2.0 - 1.0)) * ReflectionStrength;
    color = mix(color, color * 0.82 + vec3(0.10, 0.17, 0.24) * reflectedSky, wet * 0.13);
    float cloudShade = CloudCoverage * CloudDensity * horizon * 0.025;
    color *= 1.0 - cloudShade;
    float liquidMotion = mix(sin((texCoord.x + texCoord.y) * 48.0 + Time * 1.4), 0.0, ReducedMotion);
    color += vec3(0.02, 0.07, 0.08) * WaterStrength * wet * liquidMotion;
    color += vec3(0.15, 0.045, 0.005) * LavaStrength * smoothstep(0.8, 1.0, color.r) * 0.035;
    color *= mix(1.0, BlockIntensity, 0.04);
    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
