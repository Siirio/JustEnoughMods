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
uniform float CameraAltitude;
uniform float OpenSky;
uniform float Underwater;
uniform vec3 WaterColor;
uniform float WaterClarity;
uniform float Aurora;
uniform float Rainbow;
uniform float DistantFlash;
uniform float AtmosphereTime;

in vec2 sceneUv;
out vec4 fragColor;

const int MAX_VIEW_SAMPLES = 16;
const int SUN_STEPS = 36;
const float SUN_REACH = 24.0;
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

float lightVisibility(vec3 origin, vec3 inverseRay) {
    ivec3 cell = ivec3(floor(origin));
    ivec3 stepDirection = ivec3(sign(inverseRay));
    vec3 cellEdge = vec3(cell) + max(vec3(stepDirection), vec3(0.0));
    vec3 nextCrossing = (cellEdge - origin) * inverseRay;
    vec3 cellDistance = abs(inverseRay);
    ivec3 volumeEnd = ivec3(VolumeSize);
    float entering = 0.0;
    float visibility = 1.0;

    for (int index = 0; index < SUN_STEPS; ++index) {
        if (any(lessThan(cell, ivec3(0))) || any(greaterThanEqual(cell, volumeEnd))) {
            break;
        }
        float leaving = min(nextCrossing.x, min(nextCrossing.y, nextCrossing.z));
        float segmentEnd = min(leaving, SUN_REACH);
        int shape = int(texelFetch(JemVoxels, cell, 0).g * 255.0 + 0.5);
        if (shape == 13) {
            visibility *= exp(-0.32 * max(segmentEnd - entering, 0.0));
            if (visibility < 0.02) {
                return 0.0;
            }
        } else if (hitsShape(shape, origin - vec3(cell), inverseRay, entering, segmentEnd)) {
            return 0.0;
        }
        if (leaving >= SUN_REACH) {
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

float celestialShafts(vec3 ray, float sceneDistance) {
    vec3 cameraInVolume = -VolumeOrigin;
    vec2 interval = boxInterval(cameraInVolume, inverseDirection(ray), vec3(0.0), VolumeSize);
    float first = max(0.35, interval.x);
    float last = min(min(sceneDistance, VIEW_REACH), interval.y);
    if (last <= first || SunStrength <= 0.0 || min(VolumeSize.x, min(VolumeSize.y, VolumeSize.z)) <= 0.0) {
        return 0.0;
    }

    // Fixed subpixel phases break up aligned sample planes without temporal flicker.
    float phase = fract(dot(mod(gl_FragCoord.xy, 8.0), vec2(0.75487767, 0.56984029)));
    float lengthInVolume = last - first;
    float stride = max(0.40, lengthInVolume / float(MAX_VIEW_SAMPLES));
    vec3 inverseSun = inverseDirection(SunDirection);
    // Test the beam across its axis. The offset is in world space and follows the light, not the camera.
    vec3 acrossLight = normalize(vec3(SunDirection.y, -SunDirection.x, 0.0));
    float shaft = 0.0;
    for (int index = 0; index < MAX_VIEW_SAMPLES; ++index) {
        float segment = first + float(index) * stride;
        float span = min(stride, last - segment);
        if (span <= 0.0) break;
        float distance = segment + (0.14 + 0.72 * phase) * span;
        vec3 point = cameraInVolume + ray * distance;
        vec3 edges = min(point, VolumeSize - point);
        float edge = smoothstep(0.95, 4.0, min(edges.x, min(edges.y, edges.z)));
        if (edge <= 0.0) continue;
        float visible = lightVisibility(point, inverseSun);
        if (visible < 0.025) continue;
        vec3 lateral = (index % 2 == 0 ? acrossLight : vec3(0.0, 0.0, 1.0)) * 0.74;
        float sideA = lightVisibility(point + lateral, inverseSun);
        float sideB = lightVisibility(point - lateral, inverseSun);
        float separation = visible - min(sideA, sideB);
        float contrast = smoothstep(0.12, 0.75, separation);
        float density = atmosphereDensity(CameraAltitude + ray.y * distance);
        if (Underwater > 0.5) {
            density = exp(-distance * mix(0.17, 0.045, WaterClarity));
        }
        shaft += span * edge * density * exp(-distance * 0.023) * visible * contrast;
    }
    float forward = 0.30 + 0.70 * pow(max(dot(ray, SunDirection), 0.0), 3.0);
    return 0.70 * (1.0 - exp(-0.26 * shaft)) * forward;
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
    float haze = 0.0;
    if (submerged) {
        float waterDensity = mix(0.10, 0.021, WaterClarity);
        haze = (1.0 - exp(-max(0.0, sceneDistance - 1.0) * waterDensity)) * 0.28;
    }

    float beam = celestialShafts(ray, sceneDistance) * SunStrength;
    vec3 beamColor = submerged ? mix(SunColor, max(WaterColor, vec3(0.12, 0.26, 0.30)), 0.53) : SunColor;
    float opacity = haze + 0.30 * beam * (1.0 - haze);
    vec4 scatter = vec4(WaterColor * haze + beamColor * beam * (1.0 - haze), opacity);

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
