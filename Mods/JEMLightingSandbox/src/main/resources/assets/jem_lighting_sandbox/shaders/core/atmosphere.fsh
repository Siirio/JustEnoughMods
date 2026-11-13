#version 150

uniform sampler2D SceneDepth;
uniform sampler3D JemVoxels;
uniform sampler2D JemShapes;
uniform mat4 InverseViewProjection;
uniform vec3 VolumeOrigin;
uniform vec3 VolumeSize;
uniform vec3 SunDirection;
uniform float SunStrength;
uniform vec3 SunColor;
uniform vec3 AirColor;
uniform float CameraAltitude;
uniform float Weather;
uniform float OpenSky;
uniform float Underwater;
uniform vec3 WaterColor;
uniform float WaterClarity;
uniform float Aurora;
uniform float Rainbow;
uniform float DistantFlash;
uniform float AtmosphereTime;
uniform vec4 LocalLight0;
uniform vec4 LocalLight1;
uniform vec4 LocalColor0;
uniform vec4 LocalColor1;

in vec2 sceneUv;
out vec4 fragColor;

const int VIEW_SAMPLES = 7;
const int SUN_STEPS = 36;
const int LOCAL_VIEW_SAMPLES = 5;
const int LOCAL_STEPS = 20;
const float SUN_REACH = 24.0;
const float LOCAL_REACH = 10.0;
const float VIEW_REACH = 36.0;
const float RAY_EPSILON = 0.0001;

vec3 inverseDirection(vec3 direction) {
    vec3 directionSign = 2.0 * vec3(greaterThanEqual(direction, vec3(0.0))) - 1.0;
    return directionSign / max(abs(direction), vec3(0.000001));
}

vec2 boxInterval(vec3 origin, vec3 inverseRay, vec3 low, vec3 high) {
    vec3 a = (low - origin) * inverseRay;
    vec3 b = (high - origin) * inverseRay;
    vec3 nearPlane = min(a, b);
    vec3 farPlane = max(a, b);
    return vec2(max(nearPlane.x, max(nearPlane.y, nearPlane.z)),
            min(farPlane.x, min(farPlane.y, farPlane.z)));
}

bool hitsShape(int shape, vec3 localOrigin, vec3 inverseRay, float entering, float leaving) {
    if (shape == 0 || leaving - entering <= RAY_EPSILON) {
        return false;
    }
    if (shape == 1) {
        return true;
    }

    if (shape >= 14) {
        for (int box = 0; box < 8; ++box) {
            vec4 low = texelFetch(JemShapes, ivec2(box * 2, shape), 0);
            if (low.w < 0.5) {
                break;
            }
            vec3 high = texelFetch(JemShapes, ivec2(box * 2 + 1, shape), 0).xyz;
            vec2 interval = boxInterval(localOrigin, inverseRay, low.xyz, high);
            if (min(interval.y, leaving) - max(interval.x, entering) > RAY_EPSILON) {
                return true;
            }
        }
    }
    return false;
}

float lightVisibility(vec3 origin, vec3 inverseRay, float reach, int steps, ivec3 sourceCell) {
    ivec3 cell = ivec3(floor(origin));
    ivec3 stepDirection = ivec3(sign(inverseRay));
    vec3 cellEdge = vec3(cell) + max(vec3(stepDirection), vec3(0.0));
    vec3 nextCrossing = (cellEdge - origin) * inverseRay;
    vec3 cellDistance = abs(inverseRay);
    ivec3 volumeEnd = ivec3(VolumeSize);
    float entering = 0.0;
    float visibility = 1.0;

    for (int index = 0; index < SUN_STEPS; ++index) {
        if (index >= steps || all(equal(cell, sourceCell))
                || any(lessThan(cell, ivec3(0))) || any(greaterThanEqual(cell, volumeEnd))) {
            break;
        }
        float leaving = min(nextCrossing.x, min(nextCrossing.y, nextCrossing.z));
        float segmentEnd = min(leaving, reach);
        int shape = int(texelFetch(JemVoxels, cell, 0).g * 255.0 + 0.5);
        if (shape == 13) {
            visibility *= exp(-0.32 * max(segmentEnd - entering, 0.0));
            if (visibility < 0.02) {
                return 0.0;
            }
        } else if (hitsShape(shape, origin - vec3(cell), inverseRay, entering, segmentEnd)) {
            return 0.0;
        }
        if (leaving >= reach) {
            break;
        }
        bvec3 crossed = lessThanEqual(nextCrossing, vec3(leaving + 0.00001));
        cell += ivec3(crossed) * stepDirection;
        nextCrossing += vec3(crossed) * cellDistance;
        entering = leaving;
    }
    return visibility;
}

float atmosphereDensity(float altitude) {
    return exp(-max(altitude, 0.0) * 0.018);
}

float sunlightPhase(vec3 ray) {
    const float anisotropy = 0.68;
    float denominator = 1.0 + anisotropy * anisotropy - 2.0 * anisotropy * dot(ray, SunDirection);
    float peak = (1.0 - anisotropy) * (1.0 - anisotropy);
    return 0.30 + 0.70 * pow(peak / max(denominator, peak), 1.5);
}

float litPath(vec3 ray, float sceneDistance) {
    vec3 cameraInVolume = -VolumeOrigin;
    vec2 interval = boxInterval(cameraInVolume, inverseDirection(ray), vec3(0.0), VolumeSize);
    float first = max(0.35, interval.x);
    float last = min(min(sceneDistance, VIEW_REACH), interval.y);
    if (last <= first || SunStrength <= 0.0 || min(VolumeSize.x, min(VolumeSize.y, VolumeSize.z)) <= 0.0) {
        return 0.0;
    }

    float stride = (last - first) / float(VIEW_SAMPLES);
    vec3 inverseSun = inverseDirection(SunDirection);
    float path = 0.0;
    for (int index = 0; index < VIEW_SAMPLES; ++index) {
        float distance = first + (float(index) + 0.5) * stride;
        vec3 point = cameraInVolume + ray * distance;
        vec3 edges = min(point, VolumeSize - point);
        float edge = smoothstep(0.0, 2.5, min(edges.x, min(edges.y, edges.z)));
        float density = atmosphereDensity(CameraAltitude + ray.y * distance);
        if (Underwater > 0.5) {
            density = exp(-distance * mix(0.17, 0.045, WaterClarity));
        }
        path += edge * density * lightVisibility(point, inverseSun, SUN_REACH, SUN_STEPS, ivec3(-1));
    }
    return path * stride;
}

vec4 localScatter(vec3 ray, float sceneDistance, vec4 light, vec4 color) {
    if (light.w <= 0.0 || color.a <= 0.0) {
        return vec4(0.0);
    }

    float reach = min(light.w, LOCAL_REACH);
    float projected = dot(ray, light.xyz);
    vec3 perpendicular = light.xyz - ray * projected;
    float radialSquared = reach * reach - dot(perpendicular, perpendicular);
    if (radialSquared <= 0.0) {
        return vec4(0.0);
    }

    float halfLength = sqrt(radialSquared);
    vec3 cameraInVolume = -VolumeOrigin;
    vec2 interval = boxInterval(cameraInVolume, inverseDirection(ray), vec3(0.0), VolumeSize);
    float first = max(max(0.25, projected - halfLength), interval.x);
    float last = min(min(sceneDistance, projected + halfLength), interval.y);
    if (last <= first) {
        return vec4(0.0);
    }

    vec3 source = cameraInVolume + light.xyz;
    if (any(lessThan(source, vec3(0.0))) || any(greaterThanEqual(source, VolumeSize))) {
        return vec4(0.0);
    }

    ivec3 sourceCell = ivec3(floor(source));
    float stride = (last - first) / float(LOCAL_VIEW_SAMPLES);
    float path = 0.0;
    for (int index = 0; index < LOCAL_VIEW_SAMPLES; ++index) {
        float viewDistance = first + (float(index) + 0.5) * stride;
        vec3 point = cameraInVolume + ray * viewDistance;
        vec3 toSource = source - point;
        float distance = length(toSource);
        vec3 direction = toSource / max(distance, 0.0001);
        float visibility = lightVisibility(point, inverseDirection(direction), distance, LOCAL_STEPS, sourceCell);
        float falloff = 1.0 - smoothstep(0.0, reach, distance);
        float density = mix(0.64, 1.0 + 0.40 * Weather, OpenSky);
        if (Underwater > 0.5) {
            density = exp(-viewDistance * mix(0.18, 0.055, WaterClarity));
        }
        path += density * visibility * falloff / (1.0 + 0.15 * distance * distance);
    }

    float strength = (1.0 - exp(-0.080 * path * stride)) * color.a;
    vec3 tint = pow(clamp(color.rgb, vec3(0.0), vec3(1.0)), vec3(0.4545));
    if (Underwater > 0.5) {
        tint *= mix(vec3(1.0), max(WaterColor, vec3(0.18, 0.30, 0.40)), 0.52);
    }
    return vec4(tint * strength, strength);
}

vec3 rainbowColor(float angle) {
    vec3 wavelength = (vec3(angle) - vec3(42.0, 41.0, 40.15)) / vec3(0.43, 0.52, 0.58);
    return exp(-wavelength * wavelength);
}

vec4 drawRainbow(vec3 ray) {
    float angle = degrees(acos(clamp(dot(ray, -SunDirection), -1.0, 1.0)));
    float aboveHorizon = smoothstep(0.005, 0.045, ray.y);
    float power = min(0.28, Rainbow * SunStrength * OpenSky * aboveHorizon * 0.28);
    vec3 spectrum = rainbowColor(angle);
    return vec4(spectrum * power, max(spectrum.r, max(spectrum.g, spectrum.b)) * power);
}

vec4 drawAurora(vec3 ray) {
    // Model a folded curtain above the north horizon. Vertical fade keeps the upper edge diffuse.
    float north = smoothstep(0.08, 0.75, -ray.z);
    float arc = atan(ray.x, max(0.02, -ray.z));
    float height = asin(clamp(ray.y, -1.0, 1.0));
    float time = AtmosphereTime * (6.2831853 / 4096.0);
    float fold = sin(arc * 4.2 + time * 36.0) * 0.050;
    fold += sin(arc * 10.6 - time * 18.0) * 0.030;
    float altitude = height - (0.31 + 0.08 * cos(arc * 2.4) + fold);
    float curtain = smoothstep(-0.030, 0.016, altitude) * (1.0 - smoothstep(0.018, 0.23, altitude));
    float striations = 0.70 + 0.30 * sin(arc * 64.0 + 5.0 * sin(arc * 15.0) + time * 28.0);
    float drift = 0.76 + 0.24 * sin(arc * 7.0 - time * 22.0);
    float intensity = curtain * striations * drift * north * Aurora * OpenSky * 0.32;
    vec3 color = mix(vec3(0.22, 0.89, 0.51), vec3(0.48, 0.36, 0.79), smoothstep(0.05, 0.24, altitude));
    return vec4(color * intensity, intensity);
}

void main() {
    float depth = texture(SceneDepth, sceneUv).r;
    vec4 target = InverseViewProjection * vec4(sceneUv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    float targetLength = length(target.xyz);
    vec3 ray = target.xyz / max(targetLength, 0.000001);
    bool sky = depth >= 1.0;
    float sceneDistance = sky ? 144.0 : min(144.0, targetLength / max(abs(target.w), 0.000001));

    bool submerged = Underwater > 0.5;
    float hazeDistance = max(sceneDistance - 6.0, 0.0);
    float horizon = 1.0 - smoothstep(0.02, 0.68, abs(ray.y));
    float hazeDensity = atmosphereDensity(CameraAltitude + ray.y * min(sceneDistance * 0.5, 52.0));
    float haze = (1.0 - exp(-hazeDistance * 0.012)) * hazeDensity;
    haze *= mix(0.07, 0.18, horizon) * mix(0.3, 1.0, max(SunStrength, Weather)) * OpenSky;

    vec3 mediumColor = AirColor;
    if (submerged) {
        float waterDensity = mix(0.10, 0.021, WaterClarity);
        haze = (1.0 - exp(-max(0.0, sceneDistance - 1.0) * waterDensity)) * 0.28;
        mediumColor = WaterColor;
    }

    float path = litPath(ray, sceneDistance);
    float beam = (1.0 - exp(-0.030 * path)) * sunlightPhase(ray) * SunStrength * 0.78;
    vec3 beamColor = submerged ? mix(SunColor, max(WaterColor, vec3(0.12, 0.26, 0.30)), 0.53) : SunColor;
    float opacity = haze + beam * (1.0 - haze);
    vec4 scatter = vec4(mediumColor * haze + beamColor * beam * (1.0 - haze), opacity);

    vec4 local0 = localScatter(ray, sceneDistance, LocalLight0, LocalColor0);
    vec4 local1 = localScatter(ray, sceneDistance, LocalLight1, LocalColor1);
    float localOpacity = 1.0 - (1.0 - local0.a) * (1.0 - local1.a);
    vec3 localTint = (local0.rgb + local1.rgb) / max(0.0001, local0.a + local1.a);
    vec4 local = vec4(localTint * localOpacity, localOpacity);
    scatter += local * (1.0 - scatter.a);

    if (!submerged && OpenSky > 0.0) {
        if (sky && ray.y > 0.0) {
            if (Rainbow > 0.0 && SunStrength > 0.0) {
                vec4 rainbow = drawRainbow(ray);
                scatter += rainbow * (1.0 - scatter.a);
            }
            if (Aurora > 0.0) {
                vec4 aurora = drawAurora(ray);
                scatter += aurora * (1.0 - scatter.a);
            }
        }
        if (DistantFlash > 0.0) {
            float cloudBand = 1.0 - smoothstep(0.12, 0.80, abs(ray.y - 0.18));
            float distanceFactor = sky ? 1.0 : smoothstep(6.0, 70.0, sceneDistance);
            float flash = DistantFlash * OpenSky * cloudBand * distanceFactor * 0.24;
            scatter += vec4(vec3(0.67, 0.74, 0.93) * flash, flash) * (1.0 - scatter.a);
        }
    }
    fragColor = scatter;
}
