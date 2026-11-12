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
uniform sampler2D OpaqueDepth;
uniform sampler2D IndirectSampler;
uniform sampler2D IndirectDepth;
uniform sampler2D LocalLights;
uniform sampler2D PointShadowAtlas;
uniform float IndirectEnabled;
uniform float LocalLightCount;
uniform float LightTileColumns;
uniform vec2 LightViewport;
uniform float LocalVolumes;
uniform float PointShadowCount;
uniform vec2 PointShadowGrid;
uniform float SkyMode;
uniform float SkySunShape;
uniform float SkySunSize;
uniform float SkyMoonSize;
uniform float SkyMoonPhase;
uniform float SkySaturation;
uniform float SkyStarDensity;
uniform float SkyStarBrightness;
uniform float SkyAurora;
uniform float SkyMilkyWay;
uniform float SkyRainbow;
uniform float SkyTurbidity;
uniform vec3 SkySolarDirection;
uniform vec3 SkyZenith;
uniform vec3 SkyHorizon;
uniform vec3 SkyMoonColor;
uniform vec3 SkyDiscColor;
uniform vec2 CascadeRange0;
uniform vec2 CascadeRange1;
uniform vec2 CascadeRange2;
uniform vec2 CascadeRange3;
uniform mat4 InverseView;
uniform mat4 Projection;
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

vec4 lightData(int address) {
    int width = textureSize(LocalLights, 0).x;
    return texelFetch(LocalLights, ivec2(address % width, address / width), 0);
}

float localVisibility(int index, vec3 ray, float range, bool volume) {
    if (float(index) >= PointShadowCount) return 1.0;
    vec3 axes = abs(ray);
    float dominant = max(axes.x, max(axes.y, axes.z));
    const float nearDistance = 0.72;
    if (dominant <= nearDistance) return 1.0;
    int face;
    vec2 plane;
    if (axes.x >= axes.y && axes.x >= axes.z) {
        face = ray.x > 0.0 ? 0 : 1;
        plane = vec2(ray.x > 0.0 ? -ray.z : ray.z, -ray.y);
    } else if (axes.y >= axes.z) {
        face = ray.y > 0.0 ? 2 : 3;
        plane = vec2(ray.x, ray.y > 0.0 ? ray.z : -ray.z);
    } else {
        face = ray.z > 0.0 ? 4 : 5;
        plane = vec2(ray.z > 0.0 ? ray.x : -ray.x, -ray.y);
    }
    float depth = range * (dominant - nearDistance) / (dominant * (range - nearDistance));
    float compareDepth = depth - max(ShadowBias, 0.00002);
    int cell = index * 6 + face;
    int columns = int(PointShadowGrid.x);
    vec2 first = vec2(cell % columns, cell / columns) / PointShadowGrid;
    vec2 texel = 1.0 / vec2(textureSize(PointShadowAtlas, 0));
    vec2 last = first + 1.0 / PointShadowGrid;
    vec2 uv = first + (plane / dominant * 0.5 + 0.5) / PointShadowGrid;
    vec2 minimum = first + texel * 0.5;
    vec2 maximum = last - texel * 0.5;
    if (volume || ShadowFilter < 0.5)
        return step(compareDepth, texture(PointShadowAtlas, clamp(uv, minimum, maximum)).r);
    float coverage = 0.0;
    for (int i = 0; i < 4; ++i) {
        vec2 offset = vec2(i % 2, i / 2) - 0.5;
        coverage += step(compareDepth, texture(PointShadowAtlas, clamp(uv + offset * texel * 2.0, minimum, maximum)).r);
    }
    return coverage * 0.25;
}

vec3 localIrradiance(vec3 world, vec3 normal, bool volume) {
    if (LocalLightCount < 1.0 || (volume && LocalVolumes < 0.5)) return vec3(0.0);
    ivec2 tile = ivec2(clamp(texCoord * LightViewport, vec2(0.0), LightViewport - 1.0) / 32.0);
    int tileAddress = int(LocalLightCount) * 2 + (tile.y * int(LightTileColumns) + tile.x) * 5;
    int count = min(int(lightData(tileAddress).r), volume ? 4 : 16);
    vec3 irradiance = vec3(0.0);
    for (int i = 0; i < count; ++i) {
        vec4 indices = lightData(tileAddress + 1 + i / 4);
        int index = int(indices[i % 4]);
        vec4 source = lightData(index * 2);
        vec3 delta = source.xyz - world;
        float distance = length(delta);
        if (distance >= source.w || distance <= 0.001) continue;
        float facing = volume ? 1.0 : max(0.0, dot(normal, delta / distance));
        if (facing < 0.001) continue;
        float edge = 1.0 - smoothstep(0.0, source.w, distance);
        float visible = localVisibility(index, -delta, source.w, volume);
        vec4 color = lightData(index * 2 + 1);
        irradiance += color.rgb * (color.a * edge * facing * visible / (1.0 + 0.12 * distance * distance));
    }
    return irradiance * BlockIntensity;
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
    vec3 localScattering = vec3(0.0);
    bool shafts = CascadeCount >= 1.0 && VolumeStrength > 0.0 && SunIntensity > 0.0;
    for (int i = 0; i < count; ++i) {
        float distance = (float(i) + 0.5) * segmentLength;
        vec3 world = eye + direction * distance;
        float density = VolumeDensity * exp(clamp(-(world.y - VolumeHeightBase) * VolumeHeightFalloff, -10.0, 3.0));
        float segmentT = exp(-density * segmentLength);
        if (shafts)
            sunScattering += transmittance * (1.0 - segmentT) * visibility(world, -ray.z * distance, false);
        localScattering += transmittance * (1.0 - segmentT) * localIrradiance(world, vec3(0.0), true);
        transmittance *= segmentT;
        if (transmittance < 0.005) break;
    }
    vec3 light = SunColor * (sunScattering * phase * VolumeStrength * SunIntensity * (1.0 - Rain));
    return vec4(FogColor * (1.0 - transmittance) + light + localScattering * 0.07957747, transmittance);
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

vec4 indirectAt(vec2 uv, float depth) {
    ivec2 size = textureSize(IndirectSampler, 0);
    vec2 cell = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(cell));
    vec2 fraction = fract(cell);
    vec4 sum = vec4(0.0);
    float total = 0.0;
    for (int y = 0; y < 2; ++y) {
        for (int x = 0; x < 2; ++x) {
            ivec2 sampleCell = clamp(base + ivec2(x, y), ivec2(0), size - 1);
            vec2 sampleUv = (vec2(sampleCell) + 0.5) / vec2(size);
            float sampleDepth = texelFetch(IndirectDepth, sampleCell, 0).r;
            if (sampleDepth >= 1.0) continue;
            float error = abs(depth + viewPosition(sampleUv, sampleDepth).z);
            float weight = (x == 0 ? 1.0 - fraction.x : fraction.x) * (y == 0 ? 1.0 - fraction.y : fraction.y);
            weight *= 1.0 - smoothstep(0.03, max(0.16, depth * 0.035), error);
            sum += texelFetch(IndirectSampler, sampleCell, 0) * weight;
            total += weight;
        }
    }
    return total > 1.0e-5 ? sum / total : vec4(0.0, 0.0, 0.0, 1.0);
}

float contactVisibility(vec3 view) {
    if (ContactShadow <= 0.0) return 1.0;
    vec3 light = transpose(mat3(InverseView)) * TowardsSun;
    for (int i = 1; i <= 8; ++i) {
        float distance = ContactShadow * float(i) / 8.0;
        vec3 sampleView = view + light * distance;
        vec4 clip = Projection * vec4(sampleView, 1.0);
        if (clip.w <= 0.0) break;
        vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
        if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) break;
        float opaqueZ = viewPosition(uv, texture(OpaqueDepth, uv).r).z;
        float occluder = opaqueZ - sampleView.z;
        if (occluder > 0.025 && occluder < max(0.1, distance * 0.25))
            return smoothstep(0.65 * ContactShadow, ContactShadow, distance);
    }
    return 1.0;
}

float skyHash(vec2 cell) {
    vec3 p = fract(vec3(cell.xyx) * 0.1031);
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

vec2 discCoordinate(vec3 direction, vec3 center) {
    vec3 horizontal = vec3(0.0, 0.0, 1.0);
    vec3 vertical = normalize(cross(horizontal, center));
    float facing = max(dot(direction, center), 0.0001);
    return vec2(dot(direction, horizontal), dot(direction, vertical)) / facing;
}

vec3 skyRadiance(vec3 direction) {
    float height = max(0.0, direction.y);
    float daylight = smoothstep(-0.14, 0.13, SkySolarDirection.y);
    float alignment = dot(direction, SkySolarDirection);
    float haze = 1.0 - pow(height, 0.45);
    float dusk = exp(-abs(SkySolarDirection.y) * 6.0);
    float horizonLuma = dot(SkyHorizon, vec3(0.2126, 0.7152, 0.0722));
    vec3 horizon = mix(vec3(horizonLuma), SkyHorizon, mix(1.0, SkySaturation, dusk));
    vec3 day = mix(SkyZenith, max(horizon, vec3(0.0)), haze * mix(0.45, 1.0, dusk));
    if (SkyMode > 1.5) {
        float airMass = 1.0 / (0.14 + height);
        float rayleigh = 0.75 * (1.0 + alignment * alignment);
        float g = 0.78;
        float mie = (1.0 - g * g) / pow(max(1.0 + g * g - 2.0 * g * alignment, 0.02), 1.5);
        float aerosol = clamp(SkyTurbidity * 0.025, 0.025, 0.4);
        vec3 extinction = exp(-airMass * vec3(0.09, 0.18, 0.35));
        day = SkyZenith * (1.0 - extinction) * rayleigh
            + horizon * (1.0 - exp(-airMass * aerosol)) * (0.33 + mie * 0.035);
    }
    vec3 night = SkyMoonColor * (0.005 + 0.01 * haze);
    vec3 sky = mix(night, day, daylight);
    float wet = clamp(Rain, 0.0, 1.0);
    float celestial = (1.0 - wet) * smoothstep(-0.035, 0.015, direction.y);
    if (alignment > 0.0) {
        vec2 sun = discCoordinate(direction, SkySolarDirection);
        float edge = SkySunShape > 0.5 ? max(abs(sun.x), abs(sun.y)) : length(sun);
        float radius = max(0.001, SkySunSize * 0.025);
        float aa = max(0.0001, fwidth(edge));
        sky += SkyDiscColor * (1.0 - smoothstep(radius - aa, radius + aa, edge)) * celestial * 5.0;
    } else {
        vec2 moon = discCoordinate(direction, -SkySolarDirection) / max(0.001, SkyMoonSize * 0.022);
        float edge = length(moon);
        float aa = max(0.001, fwidth(edge));
        float fraction = (1.0 - smoothstep(1.0 - aa, 1.0 + aa, edge));
        float phase = SkyMoonPhase * 0.78539816;
        vec3 surface = vec3(moon, sqrt(max(0.0, 1.0 - dot(moon, moon))));
        float lit = smoothstep(-aa, aa, dot(surface, vec3(sin(phase), 0.0, cos(phase))));
        sky += SkyMoonColor * fraction * lit * celestial;
    }
    vec2 angles = vec2(atan(direction.z, direction.x), asin(clamp(direction.y, -1.0, 1.0)));
    vec2 starPosition = angles * vec2(190.0, 190.0);
    vec2 cell = floor(starPosition);
    float identity = skyHash(cell);
    vec2 center = vec2(skyHash(cell + 11.7), skyHash(cell - 17.0)) * 0.55 + 0.225;
    float star = step(1.0 - min(0.45, SkyStarDensity * 0.021), identity)
        * (1.0 - smoothstep(0.035, 0.19, length(fract(starPosition) - center)));
    float dark = (1.0 - daylight) * celestial;
    sky += vec3(0.78, 0.87, 1.0) * star * SkyStarBrightness * dark;
    float galaxy = exp(-abs(dot(direction, normalize(vec3(0.31, 0.76, 0.57)))) * 18.0);
    sky += vec3(0.25, 0.31, 0.49) * galaxy * SkyMilkyWay * dark * (0.05 + identity * 0.025);
    float time = ReducedMotion > 0.5 ? 0.0 : Time;
    float curtain = sin(angles.x * 7.0 + time * 0.06 + sin(angles.x * 3.0) * 1.8) * 0.075 + 0.31;
    float aurora = exp(-abs(direction.y - curtain) * 20.0)
                 * smoothstep(-0.3, 0.7, sin(angles.x * 24.0 + time * 0.13));
    sky += mix(vec3(0.15, 0.85, 0.47), vec3(0.34, 0.32, 0.9), smoothstep(0.3, 0.48, height))
         * aurora * SkyAurora * dark * 0.16;
    float rainbowRadius = acos(clamp(-alignment, -1.0, 1.0));
    float rainbowCenter = 0.733;
    vec3 rainbow = exp(-pow(abs(vec3(rainbowRadius) - vec3(rainbowCenter + 0.016, rainbowCenter, rainbowCenter - 0.021)) / 0.012, vec3(2.0)));
    sky += rainbow * SkyRainbow * daylight * sin(wet * 3.14159265) * 0.15;
    return mix(max(sky, vec3(0.0)), max(sky, vec3(0.0)) * vec3(0.5, 0.58, 0.7), wet * 0.65);
}

void main() {
    vec3 color = decodeDisplayColor(max(texture(DiffuseSampler, texCoord).rgb, vec3(0.0)));
    float deviceDepth = texture(DepthSampler, texCoord).r;
    vec3 view = viewPosition(texCoord, deviceDepth);
    if (VolumeOnly > 0.5) {
        fragColor = integrateAtmosphere(view);
        return;
    }
    bool sky = SkyMode > 0.5 && deviceDepth >= 1.0;
    if (sky) color = skyRadiance(normalize(mat3(InverseView) * view));
    vec3 world = (InverseView * vec4(view, 1.0)).xyz;
    vec3 normal = cross(dFdx(world), dFdy(world));
    normal /= max(length(normal), 1.0e-6);
    float depth = max(0.0, -view.z);
    float horizon = smoothstep(0.2, 0.85, texCoord.y);
    float skyLight = mix(AmbientFloor, SkyIntensity, horizon) * 0.04;
    float direct = mix(0.94, 1.08, clamp(SunIntensity / 4.0, 0.0, 1.0));
    vec3 temperature = mix(vec3(0.93, 0.97, 1.05), vec3(1.06, 0.98, 0.91), clamp(WarmColdBalance * 0.5 + 0.5, 0.0, 1.0));
    vec4 indirect = vec4(0.0, 0.0, 0.0, 1.0);
    float opaqueDepth = texture(OpaqueDepth, texCoord).r;
    if (IndirectEnabled > 0.5 && deviceDepth < 1.0 && abs(opaqueDepth - deviceDepth) < 0.0000002)
        indirect = indirectAt(texCoord, depth);
    if (!sky) color *= direct * temperature;
    if (CascadeCount >= 1.0 && ShadowStrength > 0.5 && deviceDepth < 1.0) {
        float diffuse = max(0.0, dot(normal, TowardsSun));
        float sunlight = visibility(world + normal * ShadowNormalBias, depth, true) * contactVisibility(view);
        float intensity = clamp(SunIntensity * 0.20, 0.0, 0.85) * (1.0 - Rain);
        color *= (1.0 - intensity) + SunColor * diffuse * sunlight * intensity;
    }
    if (!sky) color += (color * skyLight + color * indirect.rgb) * indirect.a;
    if (deviceDepth < 1.0) color += color * localIrradiance(world, normal, false);
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
    if (VolumeEnabled > 0.5) {
        vec4 atmosphere = atmosphereAt(texCoord, depth);
        color = color * atmosphere.a + atmosphere.rgb;
    }
    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
