#version 150

uniform sampler2D ViewDepth;
uniform sampler2D HistoryColor;
uniform sampler2D HistoryDistance;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform mat4 PreviousClip;
uniform vec3 PreviousCamera;
uniform vec4 Layer1Shape;
uniform vec4 Layer2Shape;
uniform vec4 Layer3Shape;
uniform vec3 Layer1Tint;
uniform vec3 Layer2Tint;
uniform vec3 Layer3Tint;
uniform vec2 Layer1Offset;
uniform vec2 Layer2Offset;
uniform vec2 Layer3Offset;
uniform vec2 Layer1Motion;
uniform vec2 Layer2Motion;
uniform vec2 Layer3Motion;
uniform vec3 LightDirection;
uniform vec3 LightColor;
uniform vec3 ShadowCenter;
uniform float ShadowSpan;
uniform float CloudCoverage;
uniform float CloudDensity;
uniform float FieldFrequency;
uniform float RainCoverage;
uniform float Storm;
uniform float SilverLining;
uniform float LightIntensity;
uniform float Lightning;
uniform float MediumExtinction;
uniform float TemporalWeight;
uniform int CloudMode;
uniform int RaySteps;
uniform int LightSteps;
uniform int ShadowPass;
uniform int JitterIndex;

in vec2 texCoord;
out vec4 fragColor;

const float MAX_DISTANCE = 4096.0;
const float FIELD_PERIOD = 4096.0;
const float CELL_SIZE = 0.0625;
const float EPSILON = 0.001;

vec4 shape(int index) {
    if (index == 0) return Layer1Shape;
    if (index == 1) return Layer2Shape;
    return Layer3Shape;
}

vec3 tint(int index) {
    if (index == 0) return Layer1Tint;
    if (index == 1) return Layer2Tint;
    return Layer3Tint;
}

vec2 offset(int index) {
    if (index == 0) return Layer1Offset;
    if (index == 1) return Layer2Offset;
    return Layer3Offset;
}

vec2 motion(int index) {
    if (index == 0) return Layer1Motion;
    if (index == 1) return Layer2Motion;
    return Layer3Motion;
}

float hashCell(ivec2 p) {
    uvec2 cell = uvec2(p & ivec2(4095));
    uint n = cell.x * 1597334677u ^ cell.y * 3812015801u;
    n = (n ^ (n >> 16u)) * 2246822519u;
    n = (n ^ (n >> 13u)) * 3266489917u;
    return float((n ^ (n >> 16u)) & 16777215u) / 16777215.0;
}

float noise2(vec2 p) {
    ivec2 cell = ivec2(floor(p));
    vec2 f = fract(p);
    vec2 blend = f * f * (3.0 - 2.0 * f);
    return mix(mix(hashCell(cell), hashCell(cell + ivec2(1, 0)), blend.x),
            mix(hashCell(cell + ivec2(0, 1)), hashCell(cell + ivec2(1, 1)), blend.x), blend.y);
}

float detailNoise(vec2 q, float y) {
    float lower = floor(y);
    vec2 shift = vec2(13.0, 27.0) * lower;
    return mix(noise2(q + shift), noise2(q + shift + vec2(13.0, 27.0)), smoothstep(0.0, 1.0, fract(y)));
}

vec2 fieldCoordinate(vec3 position, int index) {
    return mod(position.xz * FieldFrequency + offset(index), FIELD_PERIOD);
}

vec2 blockCoordinate(vec3 position, int index) {
    return position.xz * FieldFrequency / CELL_SIZE + fract(offset(index) / CELL_SIZE);
}

float densityAt(vec3 position, int index, bool detail) {
    vec4 layer = shape(index);
    if (layer.w <= 0.0 || layer.z <= 0.0 || CloudDensity <= 0.0) return 0.0;
    float height = (position.y - layer.x) / layer.y;
    if (height < 0.0 || height > 1.0) return 0.0;
    float coverage = clamp(CloudCoverage * (0.45 + 1.30 * layer.z) + RainCoverage, 0.0, 1.0);
    if (coverage <= 0.0) return 0.0;
    vec2 q = fieldCoordinate(position, index);
    if (CloudMode == 0) {
        vec2 cell = floor(offset(index) / CELL_SIZE) + floor(blockCoordinate(position, index));
        q = (cell + 0.5) * CELL_SIZE;
    }
    float broad = noise2(q) * 0.76 + noise2(q * 2.0 + vec2(73.0, 127.0)) * 0.24;
    float threshold = mix(0.86, 0.19, coverage);
    if (CloudMode == 0) return step(threshold, broad) * CloudDensity * layer.w * 0.18;
    if (broad + 0.08 < threshold) return 0.0;
    float envelope = smoothstep(0.0, 0.18, height) * (1.0 - smoothstep(0.62, 1.0, height));
    float base = broad - threshold - (1.0 - envelope) * 0.16;
    if (detail && CloudMode == 2 && base > -0.04) {
        base += (detailNoise(q * 8.0 + vec2(13.0, 39.0), height * 4.0) - 0.5) * 0.10;
    }
    return smoothstep(-0.035, 0.19, base) * envelope * CloudDensity * layer.w * 0.16;
}

vec2 slabRange(vec3 start, vec3 direction, int index, float limit) {
    vec4 layer = shape(index);
    if (layer.w <= 0.0 || layer.z <= 0.0 || limit <= 0.0) return vec2(limit, limit);
    if (abs(direction.y) < 0.000001) {
        return start.y > layer.x && start.y < layer.x + layer.y ? vec2(0.0, limit) : vec2(limit, limit);
    }
    vec2 intersections = (vec2(layer.x, layer.x + layer.y) - start.y) / direction.y;
    return clamp(vec2(min(intersections.x, intersections.y), max(intersections.x, intersections.y)), 0.0, limit);
}

float traceLayer(vec3 start, vec3 direction, int index) {
    vec2 interval = slabRange(start, direction, index, MAX_DISTANCE);
    float travel = interval.y - interval.x;
    if (travel < EPSILON) return 0.0;
    int count = clamp(LightSteps, 2, 24);
    float spacing = travel / float(count);
    float opticalDepth = 0.0;
    for (int stepIndex = 0; stepIndex < count && opticalDepth < 8.0; ++stepIndex) {
        float t = interval.x + (float(stepIndex) + 0.5) * spacing;
        opticalDepth += densityAt(start + direction * t, index, false) * spacing;
    }
    return opticalDepth;
}

float lightTransmission(vec3 position) {
    float opticalDepth = 0.0;
    vec3 start = position + LightDirection * 0.3;
    for (int index = 0; index < 3 && opticalDepth < 8.0; ++index) {
        opticalDepth += traceLayer(start, LightDirection, index);
    }
    return exp(-opticalDepth);
}

vec3 shadowCenterAt(float base) {
    float rise = max(0.0, base - ShadowCenter.y);
    vec2 center = ShadowCenter.xz + LightDirection.xz * (rise / max(0.06, LightDirection.y));
    return vec3(center.x, base, center.y);
}

vec3 groundTransmission() {
    vec3 result = vec3(1.0);
    for (int index = 0; index < 3; ++index) {
        vec3 start = shadowCenterAt(shape(index).x);
        start.xz += (texCoord - 0.5) * ShadowSpan;
        result[index] = exp(-traceLayer(start, LightDirection, index));
    }
    return result;
}

float blockCellExit(vec3 position, vec3 direction, int index) {
    vec2 cell = fract(blockCoordinate(position, index));
    vec2 relativeEdge = mix(cell, 1.0 - cell, step(vec2(0.0), direction.xz));
    vec2 distanceToEdge = relativeEdge * CELL_SIZE / max(vec2(0.000001), abs(direction.xz) * FieldFrequency);
    if (abs(direction.x) < 0.000001) distanceToEdge.x = MAX_DISTANCE;
    if (abs(direction.z) < 0.000001) distanceToEdge.y = MAX_DISTANCE;
    return max(EPSILON, min(distanceToEdge.x, distanceToEdge.y));
}

vec3 blockNormal(vec3 front, vec3 ray, vec4 layer, int index) {
    if (abs(front.y - layer.x) < 0.025) return vec3(0.0, -1.0, 0.0);
    if (abs(front.y - layer.x - layer.y) < 0.025) return vec3(0.0, 1.0, 0.0);
    vec2 cell = fract(blockCoordinate(front, index));
    vec2 fromEdge = mix(1.0 - cell, cell, step(vec2(0.0), ray.xz));
    if (fromEdge.x < fromEdge.y) return vec3(-sign(ray.x), 0.0, 0.0);
    return vec3(0.0, 0.0, -sign(ray.z));
}

vec4 accumulateHistory(vec4 color, vec3 representative, float distanceToCloud, int layer) {
    if (TemporalWeight <= 0.0 || color.a <= 0.015 || distanceToCloud >= MAX_DISTANCE) return color;
    vec2 drift = motion(layer);
    vec3 oldPosition = representative - vec3(drift.x, 0.0, drift.y);
    vec4 oldClip = PreviousClip * vec4(oldPosition, 1.0);
    if (oldClip.w <= 0.0) return color;
    vec2 uv = oldClip.xy / oldClip.w * 0.5 + 0.5;
    if (any(lessThanEqual(uv, vec2(0.0))) || any(greaterThanEqual(uv, vec2(1.0)))) return color;
    float expectedDistance = length(oldPosition - PreviousCamera);
    float oldDistance = texture(HistoryDistance, uv).r * MAX_DISTANCE;
    if (oldDistance >= MAX_DISTANCE - 1.0
            || abs(expectedDistance - oldDistance) > max(5.0, expectedDistance * 0.045)) return color;
    vec4 history = texture(HistoryColor, uv);
    float alphaError = abs(history.a - color.a);
    float colorError = length(history.rgb - color.rgb);
    float confidence = (1.0 - smoothstep(0.06, 0.24, alphaError))
            * (1.0 - smoothstep(0.09, 0.34, colorError));
    float weight = TemporalWeight * confidence;
    if (CloudMode == 0) weight *= 0.45;
    vec4 deviation = vec4(0.11, 0.11, 0.11, 0.10);
    return mix(color, clamp(history, color - deviation, color + deviation), weight);
}

void main() {
    gl_FragDepth = 1.0;
    if (ShadowPass != 0) {
        fragColor = vec4(groundTransmission(), 1.0);
        return;
    }

    vec3 camera = InverseView[3].xyz;
    vec2 ndc = texCoord * 2.0 - 1.0;
    float sceneDepth = texture(ViewDepth, texCoord).r;
    vec4 view = InverseProjection * vec4(ndc, sceneDepth * 2.0 - 1.0, 1.0);
    vec3 ray = normalize(mat3(InverseView) * view.xyz);
    float limit = sceneDepth >= 0.999999 ? MAX_DISTANCE
            : min(MAX_DISTANCE, length(view.xyz) / max(0.000001, view.w));

    vec2 intervals[3];
    float occupiedLength = 0.0;
    float cursor = limit;
    for (int index = 0; index < 3; ++index) {
        intervals[index] = slabRange(camera, ray, index, limit);
        if (intervals[index].y > intervals[index].x + EPSILON) {
            occupiedLength += intervals[index].y - intervals[index].x;
            cursor = min(cursor, intervals[index].x);
        }
    }
    if (occupiedLength <= EPSILON || CloudDensity <= 0.0 || CloudCoverage + RainCoverage <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    int sampleCount = CloudMode == 1 ? clamp(RaySteps / 4, 4, 32) : RaySteps;
    int iterations = CloudMode == 0 ? RaySteps * 3 + 12 : sampleCount + 12;
    float nominalSpacing = max(0.15, occupiedLength / float(sampleCount));
    float jitter = CloudMode == 2 ? 0.15 + 0.7 * hashCell(ivec2(gl_FragCoord.xy) + JitterIndex * ivec2(71, 97)) : 0.5;
    float transmittance = 1.0;
    vec3 scattering = vec3(0.0);
    float referenceDistance = MAX_DISTANCE;
    float firstDistance = MAX_DISTANCE;
    int referenceLayer = 0;
    int firstLayer = 0;
    float cachedSun = 1.0;
    float lastLightSample = -MAX_DISTANCE;
    float forward = pow(max(0.0, dot(ray, LightDirection)), 12.0);
    vec3 ambient = mix(vec3(0.17, 0.21, 0.31), vec3(0.19, 0.21, 0.24), Storm);
    ambient *= 0.42 + 0.58 * clamp(LightIntensity * 1.7 + max(0.0, LightDirection.y) * 0.12, 0.0, 1.0);
    ambient += vec3(0.50, 0.57, 0.68) * Lightning;
    vec3 direct = LightColor * min(2.0, LightIntensity) * (1.0 - Storm * 0.58);

    for (int i = 0; i < iterations && cursor + EPSILON < limit && transmittance > 0.008; ++i) {
        float front = cursor + EPSILON;
        float next = limit;
        bool inside[3];
        bool anyInside = false;
        for (int index = 0; index < 3; ++index) {
            vec2 interval = intervals[index];
            inside[index] = front >= interval.x && front < interval.y;
            if (inside[index]) {
                anyInside = true;
                next = min(next, interval.y);
                if (CloudMode == 0) {
                    vec3 position = camera + ray * front;
                    next = min(next, front + blockCellExit(position, ray, index));
                }
            } else if (interval.x > front && interval.y > interval.x + EPSILON) {
                next = min(next, interval.x);
            }
        }
        if (!anyInside) {
            cursor = next;
            continue;
        }
        if (CloudMode != 0) next = min(next, cursor + nominalSpacing);
        float spacing = max(EPSILON, next - cursor);
        float t = cursor + spacing * jitter;
        vec3 position = camera + ray * t;
        vec3 radiance = vec3(0.0);
        float extinction = 0.0;
        float greatest = 0.0;
        int dominant = 0;
        for (int index = 0; index < 3; ++index) {
            if (!inside[index]) continue;
            float density = densityAt(position, index, true);
            if (density <= 0.0) continue;
            float facing = 1.0;
            float fill = 1.0;
            if (CloudMode == 0) {
                vec3 normal = blockNormal(camera + ray * front, ray, shape(index), index);
                facing = 0.45 + 0.55 * max(0.0, dot(normal, LightDirection));
                fill = 0.80 + 0.20 * max(0.0, normal.y);
            }
            if (LightIntensity > 0.0 && t - lastLightSample > clamp(nominalSpacing * 2.5, 4.0, 22.0)) {
                cachedSun = lightTransmission(position);
                lastLightSample = t;
            }
            float phase = 0.74 + SilverLining * 0.29 * forward * cachedSun;
            vec3 lighting = ambient * fill + direct * facing * phase * mix(0.10, 1.0, cachedSun);
            radiance += density * tint(index) * lighting;
            extinction += density;
            if (density > greatest) {
                greatest = density;
                dominant = index;
            }
        }
        if (extinction > 0.0) {
            float fade = (1.0 - smoothstep(MAX_DISTANCE * 0.68, MAX_DISTANCE, t)) * exp(-MediumExtinction * t);
            float nextTransmission = exp(-extinction * spacing * fade);
            scattering += transmittance * (1.0 - nextTransmission) * (radiance / extinction);
            transmittance *= nextTransmission;
            if (firstDistance >= MAX_DISTANCE) {
                firstDistance = t;
                firstLayer = dominant;
            }
            if (referenceDistance >= MAX_DISTANCE && transmittance < 0.95) {
                referenceDistance = t;
                referenceLayer = dominant;
            }
        }
        cursor = next;
    }
    if (referenceDistance >= MAX_DISTANCE) {
        referenceDistance = firstDistance;
        referenceLayer = firstLayer;
    }
    gl_FragDepth = referenceDistance / MAX_DISTANCE;
    vec4 cloud = vec4(scattering, 1.0 - transmittance);
    fragColor = accumulateHistory(cloud, camera + ray * referenceDistance, referenceDistance, referenceLayer);
}
