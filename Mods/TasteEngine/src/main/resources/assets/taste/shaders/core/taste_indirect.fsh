#version 150

uniform sampler2D OpaqueColor;
uniform sampler2D OpaqueDepth;
uniform sampler2D PreviousIndirect;
uniform sampler2D PreviousDepth;
uniform mat4 InverseProjection;
uniform mat4 Projection;
uniform mat4 Reprojection;
uniform float AoDirections;
uniform float AoSteps;
uniform float AoRadius;
uniform float AoThickness;
uniform float AoStrength;
uniform float GiSamples;
uniform float GiDistance;
uniform float GiIntensity;
uniform float ColorBleed;
uniform float AoTemporal;
uniform float GiTemporal;

in vec2 texCoord;
out vec4 fragColor;

vec3 reconstruct(vec2 uv, float depth) {
    vec4 point = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return point.xyz / point.w;
}

vec3 positionAt(vec2 uv) {
    return reconstruct(uv, texture(OpaqueDepth, uv).r);
}

vec3 normalAt(vec2 uv, vec3 center) {
    vec2 texel = 1.0 / vec2(textureSize(OpaqueDepth, 0));
    vec3 left = center - positionAt(uv - vec2(texel.x, 0.0));
    vec3 right = positionAt(uv + vec2(texel.x, 0.0)) - center;
    vec3 bottom = center - positionAt(uv - vec2(0.0, texel.y));
    vec3 top = positionAt(uv + vec2(0.0, texel.y)) - center;
    vec3 crossProduct = cross(abs(left.z) < abs(right.z) ? left : right,
                              abs(bottom.z) < abs(top.z) ? bottom : top);
    float lengthSquared = dot(crossProduct, crossProduct);
    return lengthSquared > 1.0e-14 ? crossProduct * inversesqrt(lengthSquared) : vec3(0.0, 0.0, 1.0);
}

vec2 projectRadius(float radius, float depth) {
    return vec2(Projection[0][0], Projection[1][1]) * (0.5 * radius / max(depth, 0.05));
}

bool inside(vec2 uv) {
    return all(greaterThan(uv, vec2(0.0))) && all(lessThan(uv, vec2(1.0)));
}

float horizonAo(vec3 center, vec3 normal) {
    int directions = int(AoDirections);
    int steps = int(AoSteps);
    if (directions == 0) return 1.0;
    vec2 radius = projectRadius(AoRadius, -center.z);
    float blocked = 0.0;
    for (int d = 0; d < directions; ++d) {
        float angle = float(d) * 3.14159265 / float(directions);
        vec2 axis = vec2(cos(angle), sin(angle)) * radius;
        float left = 0.0;
        float right = 0.0;
        for (int s = 1; s <= steps; ++s) {
            vec2 offset = axis * float(s) / float(steps);
            for (int side = -1; side <= 1; side += 2) {
                vec2 uv = texCoord + offset * float(side);
                if (!inside(uv)) continue;
                vec3 delta = positionAt(uv) - center;
                float distance = length(delta);
                if (distance < 0.01 || distance >= AoRadius) continue;
                float thickness = max(AoThickness, 0.001);
                float horizon = max(0.0, dot(normal, delta / distance));
                horizon *= (1.0 - smoothstep(0.5 * AoRadius, AoRadius, distance))
                         * smoothstep(0.0, thickness, dot(normal, delta));
                if (side < 0) left = max(left, horizon);
                else right = max(right, horizon);
            }
        }
        blocked += left * left + right * right;
    }
    return clamp(1.0 - AoStrength * blocked / float(directions * 2), 0.0, 1.0);
}

vec3 decode(vec3 color) {
    return mix(color / 12.92, pow((color + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), color));
}

vec3 diffuseBounce(vec3 center, vec3 normal) {
    int samples = int(GiSamples);
    if (samples == 0 || GiIntensity <= 0.0) return vec3(0.0);
    vec2 radius = min(projectRadius(GiDistance, -center.z), vec2(0.48));
    vec3 incoming = vec3(0.0);
    for (int i = 0; i < samples; ++i) {
        float phase = float(i) * 2.39996323;
        float fraction = (float(i) + 0.5) / float(samples);
        vec2 uv = texCoord + vec2(cos(phase), sin(phase)) * radius * fraction * fraction;
        if (!inside(uv)) continue;
        float deviceDepth = texture(OpaqueDepth, uv).r;
        if (deviceDepth >= 1.0) continue;
        vec3 source = reconstruct(uv, deviceDepth);
        vec3 delta = source - center;
        float distance = length(delta);
        if (distance < 0.12 || distance > GiDistance) continue;
        vec3 direction = delta / distance;
        float receiver = max(0.0, dot(normal, direction));
        if (receiver <= 0.001) continue;
        float emitter = max(0.0, -dot(normalAt(uv, source), direction));
        if (emitter <= 0.001) continue;
        vec4 midpoint = Projection * vec4((source + center) * 0.5, 1.0);
        vec2 middleUv = midpoint.xy / midpoint.w * 0.5 + 0.5;
        if (!inside(middleUv) || positionAt(middleUv).z > (source.z + center.z) * 0.5 + 0.12) continue;
        vec3 radiance = decode(max(texture(OpaqueColor, uv).rgb, vec3(0.0)));
        float neutral = dot(radiance, vec3(0.2126, 0.7152, 0.0722));
        float falloff = (1.0 - distance / GiDistance) / (1.0 + distance * distance);
        incoming += mix(vec3(neutral), radiance, ColorBleed) * receiver * emitter * falloff;
    }
    return incoming * (GiIntensity * 2.0 / float(samples));
}

void main() {
    float depth = texture(OpaqueDepth, texCoord).r;
    if (depth >= 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    vec3 center = reconstruct(texCoord, depth);
    vec3 normal = normalAt(texCoord, center);
    vec3 bounce = diffuseBounce(center, normal);
    float ao = horizonAo(center, normal);
    if (max(AoTemporal, GiTemporal) > 0.0) {
        vec4 oldClip = Reprojection * vec4(center, 1.0);
        if (oldClip.w > 0.0) {
            vec3 oldScreen = oldClip.xyz / oldClip.w * 0.5 + 0.5;
            if (inside(oldScreen.xy) && oldScreen.z > 0.0 && oldScreen.z < 1.0) {
                float storedDepth = texture(PreviousDepth, oldScreen.xy).r;
                float expectedZ = reconstruct(oldScreen.xy, oldScreen.z).z;
                float actualZ = reconstruct(oldScreen.xy, storedDepth).z;
                if (storedDepth < 1.0 && abs(expectedZ - actualZ) <= max(0.06, 0.003 * abs(expectedZ))) {
                    vec4 history = texture(PreviousIndirect, oldScreen.xy);
                    bounce = mix(bounce, clamp(history.rgb, vec3(0.0), bounce * 2.0 + 0.05), GiTemporal);
                    ao = mix(ao, clamp(history.a, ao - 0.18, ao + 0.18), AoTemporal);
                }
            }
        }
    }
    fragColor = vec4(bounce, ao);
}
