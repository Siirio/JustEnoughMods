#version 150

#moj_import <jem_lighting_sandbox:cutout_shadow.glsl>

uniform sampler3D JemVoxels;
uniform sampler2D JemShapes;
uniform vec3 VolumeSize;
uniform vec3 SunDirection;

in vec2 sceneUv;
out vec4 fragColor;

const int SUN_STEPS = 36;
const float SUN_REACH = 24.0;
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
        ivec4 voxel = ivec4(round(texelFetch(JemVoxels, cell, 0) * 255.0));
        int shape = voxel.y;
        vec2 cutout = shape >= 14 ? texelFetch(JemShapes, ivec2(63, shape), 0).xy : vec2(0.0);
        if (shape == 13) {
            visibility *= exp(-0.32 * max(segmentEnd - entering, 0.0));
            if (visibility < 0.02) {
                return 0.0;
            }
        } else if (cutout.x > 0.0) {
            if (jemHitsCutout(JemShapes, shape, int(cutout.x),
                    origin - vec3(cell) - jemCutoutOffset(voxel.zw), 1.0 / inverseRay, entering, segmentEnd)) {
                visibility *= 1.0 - cutout.y;
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

void main() {
    ivec3 grid = ivec3(VolumeSize);
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    ivec3 cell = ivec3(pixel.x % grid.x, pixel.y % grid.y,
            pixel.x / grid.x + 8 * (pixel.y / grid.y));
    if (cell.z >= grid.z) {
        fragColor = vec4(0.0);
        return;
    }
    vec3 center = vec3(cell) + 0.5;
    fragColor = vec4(lightVisibility(center, inverseDirection(SunDirection)), 0.0, 0.0, 1.0);
}
