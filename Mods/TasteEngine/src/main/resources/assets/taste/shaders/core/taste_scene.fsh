#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 InSize;
uniform mat4 InverseProjection;
uniform float CascadeCount;
uniform float ShadowBias;
uniform float ShadowNormalBias;
uniform float ShadowSamples;
uniform float SunAngularRadius;
uniform float ShadowFilter;
uniform float VolumeSteps;
uniform float VolumeDensity;
uniform float VolumeDistance;
uniform float VolumeAnisotropy;
uniform float VolumeStrength;
uniform float VolumeHeightBase;
uniform float VolumeHeightFalloff;
uniform float VolumeOnly;
uniform float VolumeEnabled;
uniform vec3 TowardsSun;
uniform vec3 SunColor;
uniform vec3 FogColor;
uniform sampler2D VolumeSampler;
uniform vec2 CascadeRange0;
uniform vec2 CascadeRange1;
uniform vec2 CascadeRange2;
uniform vec2 CascadeRange3;
uniform mat4 InverseView;
uniform mat4 LightProjection0;
uniform mat4 LightProjection1;
uniform mat4 LightProjection2;
uniform mat4 LightProjection3;
uniform sampler2D ShadowMap0;
uniform sampler2D ShadowMap1;
uniform sampler2D ShadowMap2;
uniform sampler2D ShadowMap3;
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

vec3 viewPosition(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InverseProjection * clip;
    return view.xyz / max(view.w, 1.0e-6);
}

float linearDepth(vec2 uv) {
    return max(0.0, -viewPosition(uv, texture(DepthSampler, uv).r).z);
}

int cascadeIndex(float distance) {
    if (CascadeCount >= 1.0 && distance <= CascadeRange0.x) return 0;
    if (CascadeCount >= 2.0 && distance <= CascadeRange1.x) return 1;
    if (CascadeCount >= 3.0 && distance <= CascadeRange2.x) return 2;
    if (CascadeCount >= 4.0 && distance <= CascadeRange3.x) return 3;
    return -1;
}

vec3 lightCoordinate(int index, vec3 world) {
    vec4 point = vec4(world, 1.0);
    if (index == 0) return (LightProjection0 * point).xyz * 0.5 + 0.5;
    if (index == 1) return (LightProjection1 * point).xyz * 0.5 + 0.5;
    if (index == 2) return (LightProjection2 * point).xyz * 0.5 + 0.5;
    return (LightProjection3 * point).xyz * 0.5 + 0.5;
}

float shadowDepth(int index, vec2 uv) {
    if (index == 0) return texture(ShadowMap0, uv).r;
    if (index == 1) return texture(ShadowMap1, uv).r;
    if (index == 2) return texture(ShadowMap2, uv).r;
    return texture(ShadowMap3, uv).r;
}

bool insideMap(vec3 p) {
    return all(greaterThanEqual(p, vec3(0.0))) && all(lessThanEqual(p, vec3(1.0)));
}

vec2 diskSample(int index, int count) {
    float angle = float(index) * 2.39996323;
    return vec2(cos(angle), sin(angle)) * sqrt((float(index) + 0.5) / float(count));
}

float mapVisibility(vec3 world, int index, bool filtered) {
    vec3 p = lightCoordinate(index, world);
    if (!insideMap(p)) return 1.0;
    float bias = max(ShadowBias, 0.00001);
    if (!filtered || ShadowFilter < 0.5) return step(p.z - bias, shadowDepth(index, p.xy));
    float texel = 1.0 / float(textureSize(ShadowMap0, 0).x);
    float radius = 1.5;
    if (ShadowFilter > 1.5) {
        float sum = 0.0;
        float hits = 0.0;
        for (int i = 0; i < 8; ++i) {
            float blocker = shadowDepth(index, clamp(p.xy + diskSample(i, 8) * texel * 4.0, vec2(texel), vec2(1.0 - texel)));
            if (blocker < p.z - bias) {
                sum += blocker;
                hits += 1.0;
            }
        }
        if (hits > 0.0) {
            float depthSpan = index == 0 ? CascadeRange0.y : index == 1 ? CascadeRange1.y : index == 2 ? CascadeRange2.y : CascadeRange3.y;
            vec3 xBasis = index == 0 ? vec3(LightProjection0[0].x, LightProjection0[1].x, LightProjection0[2].x)
                         : index == 1 ? vec3(LightProjection1[0].x, LightProjection1[1].x, LightProjection1[2].x)
                         : index == 2 ? vec3(LightProjection2[0].x, LightProjection2[1].x, LightProjection2[2].x)
                                      : vec3(LightProjection3[0].x, LightProjection3[1].x, LightProjection3[2].x);
            float penumbra = (p.z - sum / hits) * depthSpan * tan(radians(SunAngularRadius));
            radius = clamp(penumbra * length(xBasis) * 0.5 / texel, 1.5, 24.0);
        }
    }
    int count = int(clamp(ShadowSamples, 1.0, 64.0));
    float visible = 0.0;
    for (int i = 0; i < count; ++i) {
        vec2 uv = clamp(p.xy + diskSample(i, count) * texel * radius, vec2(texel), vec2(1.0 - texel));
        visible += step(p.z - bias, shadowDepth(index, uv));
    }
    return visible / float(count);
}

float visibility(vec3 world, float distance, bool filtered) {
    int index = cascadeIndex(distance);
    if (index < 0) return 1.0;
    float current = mapVisibility(world, index, filtered);
    float split = index == 0 ? CascadeRange0.x : index == 1 ? CascadeRange1.x : index == 2 ? CascadeRange2.x : CascadeRange3.x;
    float blend = smoothstep(split * 0.92, split, distance);
    if (blend <= 0.0) return current;
    float next = index + 1 < int(CascadeCount) ? mapVisibility(world, index + 1, filtered) : 1.0;
    return mix(current, next, blend);
}

vec4 integrateAtmosphere(vec3 view) {
    vec3 ray = normalize(view);
    vec3 direction = mat3(InverseView) * ray;
    vec3 eye = InverseView[3].xyz;
    float limit = min(length(view), VolumeDistance);
    int count = int(clamp(VolumeSteps, 1.0, 128.0));
    float segmentLength = limit / float(count);
    float g = clamp(VolumeAnisotropy, -0.9, 0.9);
    float cosTheta = dot(direction, TowardsSun);
    float phase = (1.0 - g * g) / (12.566371 * pow(max(1.0 + g * g - 2.0 * g * cosTheta, 0.01), 1.5));
    float transmittance = 1.0;
    float sunScattering = 0.0;
    bool shafts = CascadeCount >= 1.0 && VolumeStrength > 0.0 && SunIntensity > 0.0;
    for (int i = 0; i < count; ++i) {
        float distance = (float(i) + 0.5) * segmentLength;
        vec3 world = eye + direction * distance;
        float density = VolumeDensity * exp(clamp(-(world.y - VolumeHeightBase) * VolumeHeightFalloff, -10.0, 3.0));
        float segmentT = exp(-density * segmentLength);
        if (shafts)
            sunScattering += transmittance * (1.0 - segmentT) * visibility(world, -ray.z * distance, false);
        transmittance *= segmentT;
        if (transmittance < 0.005) break;
    }
    vec3 light = SunColor * (sunScattering * phase * VolumeStrength * SunIntensity * (1.0 - Rain));
    return vec4(FogColor * (1.0 - transmittance) + light, transmittance);
}

vec4 atmosphereAt(vec2 uv, float depth) {
    ivec2 size = textureSize(VolumeSampler, 0);
    vec2 position = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(position));
    vec2 fraction = fract(position);
    vec4 accumulated = vec4(0.0);
    float weightSum = 0.0;
    for (int y = 0; y < 2; ++y) {
        for (int x = 0; x < 2; ++x) {
            ivec2 samplePosition = clamp(base + ivec2(x, y), ivec2(0), size - 1);
            vec2 sampleUv = (vec2(samplePosition) + 0.5) / vec2(size);
            float difference = abs(depth - linearDepth(sampleUv));
            float edgeWeight = 1.0 / (1.0 + difference * difference);
            float spatialWeight = (x == 0 ? 1.0 - fraction.x : fraction.x)
                                * (y == 0 ? 1.0 - fraction.y : fraction.y);
            float weight = edgeWeight * spatialWeight;
            accumulated += texelFetch(VolumeSampler, samplePosition, 0) * weight;
            weightSum += weight;
        }
    }
    if (weightSum <= 1.0e-6) return integrateAtmosphere(viewPosition(uv, texture(DepthSampler, uv).r));
    return accumulated / weightSum;
}

vec3 decodeDisplayColor(vec3 color) {
    vec3 linearLow = color / 12.92;
    vec3 linearHigh = pow((color + 0.055) / 1.055, vec3(2.4));
    return mix(linearLow, linearHigh, step(vec3(0.04045), color));
}

void main() {
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec3 color = decodeDisplayColor(max(texture(DiffuseSampler, texCoord).rgb, vec3(0.0)));
    float deviceDepth = texture(DepthSampler, texCoord).r;
    vec3 view = viewPosition(texCoord, deviceDepth);
    if (VolumeOnly > 0.5) {
        fragColor = integrateAtmosphere(view);
        return;
    }
    vec3 world = (InverseView * vec4(view, 1.0)).xyz;
    vec3 normal = cross(dFdx(world), dFdy(world));
    normal /= max(length(normal), 1.0e-6);
    float depth = max(0.0, -view.z);
    vec2 sampleStep = texel * max(AoRadius, 0.5);
    float dx = abs(depth - linearDepth(texCoord + vec2(sampleStep.x, 0.0)));
    float dy = abs(depth - linearDepth(texCoord + vec2(0.0, sampleStep.y)));
    float contact = smoothstep(0.04, 1.2, (dx + dy) / max(depth, 0.5));
    float horizon = smoothstep(0.2, 0.85, texCoord.y);
    float skyLight = mix(AmbientFloor, SkyIntensity, horizon) * 0.04;
    float direct = mix(0.94, 1.08, clamp(SunIntensity / 4.0, 0.0, 1.0));
    float shadow = 1.0 - contact * AoStrength * 0.12 - contact * ShadowStrength * min(ContactShadow, 32.0) * 0.002;
    vec3 temperature = mix(vec3(0.93, 0.97, 1.05), vec3(1.06, 0.98, 0.91), clamp(WarmColdBalance * 0.5 + 0.5, 0.0, 1.0));
    color *= direct * shadow * temperature;
    if (CascadeCount >= 1.0 && ShadowStrength > 0.5 && deviceDepth < 1.0) {
        float diffuse = max(0.0, dot(normal, TowardsSun));
        float sunlight = visibility(world + normal * ShadowNormalBias, depth, true);
        float intensity = clamp(SunIntensity * 0.20, 0.0, 0.85) * (1.0 - Rain);
        color *= (1.0 - intensity) + SunColor * diffuse * sunlight * intensity;
    }
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
    if (VolumeEnabled > 0.5) {
        vec4 atmosphere = atmosphereAt(texCoord, depth);
        color = color * atmosphere.a + atmosphere.rgb;
    }
    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
