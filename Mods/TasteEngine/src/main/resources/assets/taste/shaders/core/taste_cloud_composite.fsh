#version 150

uniform sampler2D SceneColor;
uniform sampler2D ViewDepth;
uniform sampler2D CloudColor;
uniform sampler2D CloudDistance;
uniform sampler2D CloudShadow;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform vec2 Layer1Shape;
uniform vec2 Layer2Shape;
uniform vec2 Layer3Shape;
uniform vec3 ShadowCenter;
uniform vec3 ShadowDirection;
uniform float ShadowSpan;
uniform float ShadowStrength;

in vec2 texCoord;
out vec4 fragColor;

const float MAX_DISTANCE = 4096.0;
const float SKY_DEPTH = 0.999999;

vec3 viewAt(vec2 uv, float depth) {
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / max(0.000001, p.w);
}

vec4 reconstructCloud(float depth, float limit) {
    ivec2 size = textureSize(CloudColor, 0);
    vec2 center = texCoord * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(center));
    vec2 blend = fract(center);
    vec4 accumulated = vec4(0.0);
    float totalWeight = 0.0;
    bool isSky = depth >= SKY_DEPTH;
    for (int y = 0; y < 2; ++y) {
        for (int x = 0; x < 2; ++x) {
            ivec2 tap = clamp(base + ivec2(x, y), ivec2(0), size - 1);
            vec2 uv = (vec2(tap) + 0.5) / vec2(size);
            float neighborDepth = texture(ViewDepth, uv).r;
            bool neighborIsSky = neighborDepth >= SKY_DEPTH;
            float weight = (x == 0 ? 1.0 - blend.x : blend.x) * (y == 0 ? 1.0 - blend.y : blend.y);
            if (!isSky && !neighborIsSky) {
                float neighborLimit = length(viewAt(uv, neighborDepth));
                weight *= 1.0 / (1.0 + abs(limit - neighborLimit) / max(1.0, limit * 0.025));
            } else if (isSky != neighborIsSky) {
                weight *= 0.025;
            }
            vec4 sampleColor = texelFetch(CloudColor, tap, 0);
            if (sampleColor.a > 0.0) {
                float cloudDistance = texelFetch(CloudDistance, tap, 0).r * MAX_DISTANCE;
                float tolerance = max(0.35, limit * 0.002);
                if (!isSky && cloudDistance >= limit + tolerance) continue;
                if (!isSky) sampleColor *= 1.0 - smoothstep(limit - tolerance, limit + tolerance, cloudDistance);
            }
            accumulated += sampleColor * weight;
            totalWeight += weight;
        }
    }
    return totalWeight > 0.0001 ? accumulated / totalWeight : vec4(0.0);
}

float shadowAt(vec3 position, vec2 slab, int index) {
    if (position.y >= slab.x + slab.y) return 1.0;
    vec2 slope = ShadowDirection.xz / max(0.06, ShadowDirection.y);
    vec2 center = ShadowCenter.xz + slope * max(0.0, slab.x - ShadowCenter.y);
    vec2 atBase = position.xz + slope * (slab.x - position.y);
    vec2 uv = (atBase - center) / ShadowSpan + 0.5;
    vec2 border = min(uv, 1.0 - uv);
    float fade = smoothstep(0.0, 0.10, min(border.x, border.y));
    if (fade <= 0.0) return 1.0;
    float transmission = texture(CloudShadow, uv)[index];
    float remaining = clamp((slab.x + slab.y - position.y) / slab.y, 0.0, 1.0);
    return mix(1.0, pow(max(0.001, transmission), remaining), fade);
}

void main() {
    vec4 scene = texture(SceneColor, texCoord);
    float depth = texture(ViewDepth, texCoord).r;
    bool isSky = depth >= SKY_DEPTH;
    vec3 view = isSky ? vec3(0.0) : viewAt(texCoord, depth);
    if (!isSky && ShadowStrength > 0.0 && scene.a > 0.0) {
        vec3 position = (InverseView * vec4(view, 1.0)).xyz;
        float shadow = shadowAt(position, Layer1Shape, 0) * shadowAt(position, Layer2Shape, 1)
                * shadowAt(position, Layer3Shape, 2);
        scene.rgb *= 1.0 - ShadowStrength * clamp(scene.a, 0.0, 1.0) * (1.0 - shadow);
    }
    vec4 cloud = reconstructCloud(depth, isSky ? MAX_DISTANCE : length(view));
    fragColor = vec4(scene.rgb * (1.0 - cloud.a) + cloud.rgb, scene.a);
}
