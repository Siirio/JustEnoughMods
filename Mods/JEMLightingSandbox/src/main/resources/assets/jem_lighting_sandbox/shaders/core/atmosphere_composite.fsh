#version 150

uniform sampler2D AtmosphereTex;
uniform sampler2D SceneDepth;
uniform sampler2D SceneColor;
uniform mat4 InverseViewProjection;
uniform float Underwater;
uniform float Heat;
uniform vec4 HeatOrigin;
uniform float AtmosphereTime;

in vec2 sceneUv;
out vec4 fragColor;

float readSceneDepth(vec2 uv, ivec2 size) {
    ivec2 pixel = clamp(ivec2(uv * vec2(size)), ivec2(0), size - 1);
    return texelFetch(SceneDepth, pixel, 0).r;
}

float viewDepth(vec2 uv, float rawDepth) {
    vec4 clipPosition = vec4(uv, rawDepth, 1.0);
    clipPosition.xyz = clipPosition.xyz * 2.0 - 1.0;
    float inverseDepth = (InverseViewProjection * clipPosition).w;
    return 1.0 / max(inverseDepth, 0.00001);
}

void addTap(ivec2 pixel, float weight, ivec2 atmosphereSize, ivec2 depthSize, float centerDepth,
        bool centerSky, inout vec4 accumulated, inout float accumulatedWeight) {
    pixel = clamp(pixel, ivec2(0), atmosphereSize - 1);
    vec2 uv = (vec2(pixel) + 0.5) / vec2(atmosphereSize);
    float rawDepth = readSceneDepth(uv, depthSize);
    bool sampleSky = rawDepth >= 1.0;
    if (sampleSky != centerSky) {
        return;
    }

    if (!centerSky) {
        float sampleDepth = viewDepth(uv, rawDepth);
        float separation = abs(sampleDepth - centerDepth);
        float scale = max(0.5, min(sampleDepth, centerDepth));
        weight *= 1.0 - smoothstep(0.02, 0.12, separation / scale);
    }
    accumulated += texelFetch(AtmosphereTex, pixel, 0) * weight;
    accumulatedWeight += weight;
}

void main() {
    ivec2 atmosphereSize = textureSize(AtmosphereTex, 0);
    ivec2 depthSize = textureSize(SceneDepth, 0);
    float rawDepth = readSceneDepth(sceneUv, depthSize);
    bool centerSky = rawDepth >= 1.0;
    float centerDepth = centerSky ? 0.0 : viewDepth(sceneUv, rawDepth);
    vec2 position = sceneUv * vec2(atmosphereSize) - 0.5;
    ivec2 basePixel = ivec2(floor(position));
    vec2 fraction = fract(position);
    vec2 inverseFraction = 1.0 - fraction;

    vec4 accumulated = vec4(0.0);
    float weight = 0.0;
    addTap(basePixel, inverseFraction.x * inverseFraction.y, atmosphereSize, depthSize,
        centerDepth, centerSky, accumulated, weight);
    addTap(basePixel + ivec2(1, 0), fraction.x * inverseFraction.y, atmosphereSize, depthSize,
        centerDepth, centerSky, accumulated, weight);
    addTap(basePixel + ivec2(0, 1), inverseFraction.x * fraction.y, atmosphereSize, depthSize,
        centerDepth, centerSky, accumulated, weight);
    addTap(basePixel + ivec2(1, 1), fraction.x * fraction.y, atmosphereSize, depthSize,
        centerDepth, centerSky, accumulated, weight);

    // Fade where the reduced buffer cannot represent the geometry under this pixel.
    vec4 scatter = accumulated * (smoothstep(0.0, 0.15, weight) / max(weight, 0.00001));
    if (Underwater > 0.5) {
        vec2 wave = vec2(sin(sceneUv.y * 54.0 + AtmosphereTime * 1.7),
                sin(sceneUv.x * 47.0 - AtmosphereTime * 1.35));
        vec2 screenEdge = min(sceneUv, 1.0 - sceneUv);
        float edge = smoothstep(0.0, 0.04, min(screenEdge.x, screenEdge.y));
        vec2 bent = sceneUv + wave * (0.0018 * edge);
        vec3 scene = texture(SceneColor, bent).rgb;
        fragColor = vec4(scene * (1.0 - scatter.a) + scatter.rgb, 1.0);
    } else if (Heat > 0.01) {
        vec4 target = InverseViewProjection * vec4(sceneUv * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
        vec3 ray = normalize(target.xyz);
        float plane = abs(ray.y);
        float distance = centerSky ? 40.0 : centerDepth;
        float distortion = Heat * smoothstep(1.5, 8.0, distance) * (1.0 - smoothstep(0.13, 0.45, plane));
        if (HeatOrigin.w > 0.0) {
            float closest = dot(ray, HeatOrigin.xyz);
            float separation = length(ray * closest - HeatOrigin.xyz);
            float rayDepth = centerSky ? 40.0 : length(target.xyz) / max(abs(target.w), 0.00001);
            distortion = closest > 0.0 && closest < rayDepth
                    ? Heat * (1.0 - smoothstep(0.0, HeatOrigin.w, separation)) : 0.0;
        }
        vec2 offset = vec2(sin(sceneUv.y * 140.0 - AtmosphereTime * 3.4),
                sin(sceneUv.x * 131.0 + AtmosphereTime * 4.0)) * distortion * 0.0013;
        vec2 bent = clamp(sceneUv + offset, vec2(0.001), vec2(0.999));
        vec3 scene = texture(SceneColor, bent).rgb;
        fragColor = vec4(scene * (1.0 - scatter.a) + scatter.rgb, 1.0);
    } else {
        fragColor = scatter;
    }
}
