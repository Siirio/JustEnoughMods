#version 150

uniform sampler2D SceneDepth;
uniform sampler3D JemVoxels;
uniform mat4 InverseViewProjection;
uniform vec3 VolumeOrigin;
uniform vec3 VolumeSize;
uniform vec3 SunDirection;
uniform float SunStrength;
uniform vec3 SunColor;
uniform vec3 AirColor;
uniform float CameraAltitude;
uniform float Weather;

in vec2 sceneUv;
out vec4 fragColor;

const int VIEW_SAMPLES = 6;
const int SUN_STEPS = 28;
const float SUN_REACH = 19.0;
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

    // The terrain texture stores snow and door bounds in its green channel.
    vec3 low = vec3(0.0);
    vec3 high = vec3(1.0);
    if (shape <= 8) {
        high.y = float(shape - 1) * 0.125;
    } else if (shape == 9) {
        high.x = 0.1875;
    } else if (shape == 10) {
        low.x = 0.8125;
    } else if (shape == 11) {
        high.z = 0.1875;
    } else {
        low.z = 0.8125;
    }
    vec2 interval = boxInterval(localOrigin, inverseRay, low, high);
    return min(interval.y, leaving) - max(interval.x, entering) > RAY_EPSILON;
}

float sunVisibility(vec3 origin, vec3 inverseRay) {
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
            visibility *= exp(-0.85 * max(segmentEnd - entering, 0.0));
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

float sunlightPhase(vec3 ray) {
    const float anisotropy = 0.68;
    float denominator = 1.0 + anisotropy * anisotropy - 2.0 * anisotropy * dot(ray, SunDirection);
    float peak = (1.0 - anisotropy) * (1.0 - anisotropy);
    return 0.18 + 0.82 * pow(peak / max(denominator, peak), 1.5);
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
        path += edge * density * sunVisibility(point, inverseSun);
    }
    return path * stride;
}

void main() {
    float depth = texture(SceneDepth, sceneUv).r;
    vec4 target = InverseViewProjection * vec4(sceneUv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    float targetLength = length(target.xyz);
    vec3 ray = target.xyz / max(targetLength, 0.000001);
    bool sky = depth >= 1.0;
    float sceneDistance = sky ? 144.0 : min(144.0, targetLength / max(abs(target.w), 0.000001));

    float hazeDistance = max(sceneDistance - 6.0, 0.0);
    float horizon = 1.0 - smoothstep(0.02, 0.68, abs(ray.y));
    float hazeDensity = atmosphereDensity(CameraAltitude + ray.y * min(sceneDistance * 0.5, 52.0));
    float haze = (1.0 - exp(-hazeDistance * 0.012)) * hazeDensity;
    haze *= mix(0.07, 0.18, horizon) * mix(0.3, 1.0, max(SunStrength, Weather));

    float path = litPath(ray, sceneDistance);
    float beam = (1.0 - exp(-0.024 * path)) * sunlightPhase(ray) * SunStrength * 0.78;
    float opacity = haze + beam * (1.0 - haze);
    vec3 scattered = AirColor * haze + SunColor * beam * (1.0 - haze);
    fragColor = vec4(scattered, opacity);
}
