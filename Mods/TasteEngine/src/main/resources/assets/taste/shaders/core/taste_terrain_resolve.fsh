#version 150

uniform sampler2D SceneSampler;
uniform sampler2D OpaqueDepth;
uniform sampler2D WorldDepth;
uniform sampler2D MaterialDepth;
uniform sampler2D AlbedoAo;
uniform sampler2D NormalSurface;
uniform sampler2D EmissionFlags;
uniform sampler2D MotionLight;
uniform sampler2D MaterialAux;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform vec3 LightDirection;
uniform vec3 LightColor;
uniform float Subsurface;
uniform float EmissiveStrength;
uniform float Wetness;
uniform float WetDarkening;
uniform float WetSmoothness;
uniform float PuddleCoverage;
uniform float PuddleDepth;
uniform float WorldSeed;
uniform vec2 PuddleOrigin;
in vec2 texCoord;
out vec4 fragColor;

vec3 decodeOct(vec2 oct) {
    vec3 normal = vec3(oct, 1.0 - abs(oct.x) - abs(oct.y));
    float fold = max(0.0, -normal.z);
    normal.xy -= mix(vec2(-fold), vec2(fold), step(vec2(0.0), normal.xy));
    return normalize(normal);
}

float hash(vec2 cell) {
    return fract(sin(dot(cell, vec2(127.1, 311.7)) + mod(WorldSeed, 65536.0) * 0.0002) * 43758.5453);
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    float depth = texture(OpaqueDepth, texCoord).r;
    float materialDepth = texture(MaterialDepth, texCoord).r;
    vec4 emission = texture(EmissionFlags, texCoord);
    int flags = int(emission.a + 0.5);
    if (depth >= 1.0 || (flags & 1) == 0 || abs(depth - materialDepth) > 0.00000024
            || abs(depth - texture(WorldDepth, texCoord).r) > 0.00000024) {
        fragColor = scene;
        return;
    }
    vec4 position = InverseProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 viewPosition = position.xyz / position.w;
    vec3 world = (InverseView * vec4(viewPosition, 1.0)).xyz;
    vec3 view = normalize(mat3(InverseView) * -viewPosition);
    vec4 surface = texture(NormalSurface, texCoord);
    vec4 albedo = texture(AlbedoAo, texCoord);
    vec4 aux = texture(MaterialAux, texCoord);
    vec2 lights = texture(MotionLight, texCoord).zw;
    vec3 normal = decodeOct(surface.xy);
    float noV = max(dot(normal, view), 0.001);
    float noL = max(dot(normal, LightDirection), 0.0);
    float roughness = surface.z;
    float wet = Wetness * aux.a;
    float puddle = step(1.0 - PuddleCoverage, hash(floor((world.xz + PuddleOrigin) * 0.3)))
                 * smoothstep(0.82, 0.98, normal.y) * wet * smoothstep(0.0, 0.03, PuddleDepth);
    roughness = mix(roughness, max(0.04, roughness * (1.0 - WetSmoothness)), wet);
    roughness = mix(roughness, 0.035, puddle);
    float metal = (flags & 128) != 0 ? surface.w / 255.0 : step(229.5, surface.w);
    vec3 f0 = (flags & 128) != 0 ? mix(vec3(0.04), albedo.rgb, metal)
            : mix(vec3(surface.w / 255.0), albedo.rgb, metal);
    vec3 halfway = normalize(view + LightDirection);
    float noH = max(dot(normal, halfway), 0.0);
    float voH = max(dot(view, halfway), 0.0);
    float alpha = roughness * roughness;
    float alpha2 = alpha * alpha;
    float den = noH * noH * (alpha2 - 1.0) + 1.0;
    float distribution = alpha2 / max(0.0001, 3.14159265 * den * den);
    float k = (roughness + 1.0) * (roughness + 1.0) / 8.0;
    float visibility = noL * noV / max(0.0001, (noL * (1.0 - k) + k) * (noV * (1.0 - k) + k));
    vec3 fresnel = f0 + (1.0 - f0) * pow(1.0 - voH, 5.0);
    vec3 highlight = fresnel * distribution * visibility * noL / max(0.01, 4.0 * noL * noV);
    vec3 color = scene.rgb * (1.0 - wet * WetDarkening * (1.0 - aux.r * 0.5));
    color *= mix(albedo.a * aux.b, 1.0, scene.a);
    color += min(highlight, vec3(5.0)) * LightColor * scene.a;
    float thin = Subsurface * aux.g * max(0.0, dot(-normal, LightDirection));
    color += albedo.rgb * LightColor * thin * max(scene.a, lights.y * 0.12);
    color += emission.rgb * EmissiveStrength;
    fragColor = vec4(max(color, vec3(0.0)), scene.a);
}
