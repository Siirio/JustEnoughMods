#version 430 core

uniform sampler2D ViewDepth;
uniform sampler2D Ambient;
uniform sampler2D HistoryRadiance;
uniform sampler2D HistoryDepth;
uniform sampler3D Clip0;
uniform sampler3D Clip1;
uniform sampler3D Clip2;
uniform sampler3D Clip3;
uniform sampler3D SurfaceSky;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform mat4 PreviousClip;
uniform mat4 PreviousInverseProjection;
uniform mat4 PreviousInverseView;
uniform vec4 ClipBounds[4];
uniform ivec3 NearClipCell;
uniform vec3 SkyRadiance;
uniform int Resolution;
uniform int ClipCount;
uniform int ConeCount;
uniform int HasAmbient;
uniform float DistanceLimit;
uniform float OpacityBias;
uniform float Intensity;
uniform float TemporalWeight;

in vec2 texCoord;
out vec4 fragColor;

const float SKY_DEPTH = 0.999999;
const float GOLDEN_ANGLE = 2.39996323;

vec3 reconstruct(vec2 uv, float depth, mat4 inverseProjection) {
    vec4 position = inverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return position.xyz / max(1e-6, position.w);
}

vec3 normalAt(sampler2D depthBuffer, vec2 uv, vec3 center, mat4 inverseProjection) {
    vec2 px = 1.0 / vec2(textureSize(depthBuffer, 0));
    vec2 leftUv = max(vec2(0.0), uv - vec2(px.x, 0.0));
    vec2 rightUv = min(vec2(1.0), uv + vec2(px.x, 0.0));
    vec2 downUv = max(vec2(0.0), uv - vec2(0.0, px.y));
    vec2 upUv = min(vec2(1.0), uv + vec2(0.0, px.y));
    vec3 left = reconstruct(leftUv, texture(depthBuffer, leftUv).r, inverseProjection);
    vec3 right = reconstruct(rightUv, texture(depthBuffer, rightUv).r, inverseProjection);
    vec3 down = reconstruct(downUv, texture(depthBuffer, downUv).r, inverseProjection);
    vec3 up = reconstruct(upUv, texture(depthBuffer, upUv).r, inverseProjection);
    vec3 dx = abs(right.z - center.z) <= abs(center.z - left.z) ? right - center : center - left;
    vec3 dy = abs(up.z - center.z) <= abs(center.z - down.z) ? up - center : center - down;
    vec3 normal = cross(dx, dy);
    if (dot(normal, normal) < 1e-12) return vec3(0.0, 0.0, 1.0);
    return normalize(normal) * (dot(normal, center) > 0.0 ? -1.0 : 1.0);
}

vec3 tile(int face) {
    return vec3(face & 1, (face >> 1) & 1, (face >> 2) & 1);
}

vec4 fetchClip(int level, vec3 position, float mip) {
    if (level == 0) return textureLod(Clip0, position, mip);
    if (level == 1) return textureLod(Clip1, position, mip);
    if (level == 2) return textureLod(Clip2, position, mip);
    return textureLod(Clip3, position, mip);
}

vec4 fetchLobe(int level, vec3 position, int face, float lod) {
    float coarseMip = min(ceil(lod), log2(float(Resolution)));
    float edge = exp2(coarseMip) * 0.5 / float(Resolution);
    vec3 coordinate = (clamp(position, vec3(edge), vec3(1.0 - edge)) + tile(face)) * 0.5;
    return fetchClip(level, coordinate, lod);
}

vec4 sampleLevel(int level, vec3 relative, vec3 outgoing, float width) {
    vec4 clip = ClipBounds[level];
    vec3 local = (relative - clip.xyz) / (clip.w * float(Resolution));
    float lod = clamp(log2(max(1.0, width / clip.w)), 0.0, log2(float(Resolution)));
    vec3 weight = outgoing * outgoing;
    return fetchLobe(level, local, outgoing.x >= 0.0 ? 0 : 1, lod) * weight.x
            + fetchLobe(level, local, outgoing.y >= 0.0 ? 2 : 3, lod) * weight.y
            + fetchLobe(level, local, outgoing.z >= 0.0 ? 4 : 5, lod) * weight.z;
}

float borderDistance(int level, vec3 relative) {
    vec4 clip = ClipBounds[level];
    vec3 local = relative - clip.xyz;
    vec3 border = min(local, clip.w * float(Resolution) - local);
    return min(border.x, min(border.y, border.z));
}

vec4 sampleVolume(vec3 relative, vec3 outgoing, float width) {
    for (int level = 0; level < ClipCount; ++level) {
        float border = borderDistance(level, relative);
        float transition = max(width, ClipBounds[level].w * 2.0);
        if (border <= width * 0.5) continue;
        vec4 value = sampleLevel(level, relative, outgoing, width);
        if (level + 1 < ClipCount && border < transition * 2.0
                && borderDistance(level + 1, relative) > width * 0.5) {
            vec4 coarse = sampleLevel(level + 1, relative, outgoing, width);
            return mix(coarse, value, smoothstep(width * 0.5, transition * 2.0, border));
        }
        return value;
    }
    return vec4(-1.0);
}

float nativeSky(vec3 surface) {
    vec4 clip = ClipBounds[0];
    ivec3 logical = ivec3(floor((surface - clip.xyz) / clip.w));
    if (any(lessThan(logical, ivec3(0))) || any(greaterThanEqual(logical, ivec3(Resolution)))) return 0.0;
    ivec3 physical = ((logical + NearClipCell) % Resolution + Resolution) % Resolution;
    return texelFetch(SurfaceSky, physical, 0).a;
}

vec3 traceCone(vec3 point, vec3 direction, float sky) {
    float stepSize = ClipBounds[0].w;
    float travel = stepSize * 1.15;
    float transmission = 1.0;
    vec3 radiance = vec3(0.0);
    for (int stepIndex = 0; stepIndex < 24 && travel < DistanceLimit && transmission > 0.025; ++stepIndex) {
        float width = max(stepSize, travel * 0.52);
        vec4 sampleValue = sampleVolume(point + direction * travel, -direction, width);
        if (sampleValue.a < 0.0) {
            radiance += SkyRadiance * transmission * sky * max(0.0, direction.y) * 0.08;
            break;
        }
        radiance += sampleValue.rgb * transmission / (1.0 + travel * 0.055);
        float opacity = 1.0 - pow(max(0.0, 1.0 - sampleValue.a), 1.0 + OpacityBias);
        transmission *= 1.0 - opacity;
        travel += max(stepSize * 0.85, width * 0.64);
    }
    return radiance;
}

vec3 history(vec3 current, vec3 point, vec3 normal) {
    if (TemporalWeight <= 0.0) return current;
    vec4 old = PreviousClip * vec4(point, 1.0);
    if (old.w <= 0.0) return current;
    vec3 ndc = old.xyz / old.w;
    vec2 uv = ndc.xy * 0.5 + 0.5;
    if (any(lessThanEqual(uv, vec2(0.0))) || any(greaterThanEqual(uv, vec2(1.0)))) return current;
    float d = texture(HistoryDepth, uv).r;
    if (d >= SKY_DEPTH) return current;
    vec3 previous = reconstruct(uv, d, PreviousInverseProjection);
    vec3 expected = reconstruct(uv, ndc.z * 0.5 + 0.5, PreviousInverseProjection);
    if (abs(previous.z - expected.z) > max(0.13, abs(expected.z) * 0.012)) return current;
    vec3 oldNormal = normalize(mat3(PreviousInverseView) * normalAt(HistoryDepth, uv, previous, PreviousInverseProjection));
    if (dot(oldNormal, normal) < 0.9) return current;
    vec3 previousColor = texture(HistoryRadiance, uv).rgb;
    vec3 tolerance = max(vec3(0.025), current * 0.35);
    return mix(current, clamp(previousColor, current - tolerance, current + tolerance), TemporalWeight);
}

void main() {
    float depth = texture(ViewDepth, texCoord).r;
    float ao = HasAmbient != 0 ? texture(Ambient, texCoord).a : 1.0;
    if (depth >= SKY_DEPTH || ClipCount <= 0 || Intensity <= 0.0) {
        fragColor = vec4(0.0, 0.0, 0.0, ao);
        return;
    }
    vec3 view = reconstruct(texCoord, depth, InverseProjection);
    vec3 point = (InverseView * vec4(view, 1.0)).xyz;
    vec3 normal = normalize(mat3(InverseView) * normalAt(ViewDepth, texCoord, view, InverseProjection));
    vec3 helper = abs(normal.y) < 0.95 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent = normalize(cross(normal, helper));
    vec3 bitangent = cross(normal, tangent);
    float sky = nativeSky(point);
    vec3 origin = point + normal * ClipBounds[0].w * 0.72;
    vec3 radiance = vec3(0.0);
    for (int cone = 0; cone < ConeCount; ++cone) {
        float y = sqrt(1.0 - (float(cone) + 0.5) / float(ConeCount));
        float radius = sqrt(1.0 - y * y);
        float angle = float(cone) * GOLDEN_ANGLE;
        vec3 direction = normalize(normal * y + radius * (tangent * cos(angle) + bitangent * sin(angle)));
        radiance += traceCone(origin, direction, sky);
    }
    radiance = min(radiance * (Intensity / float(ConeCount)), vec3(3.0));
    fragColor = vec4(history(radiance, point, normal), ao);
}
