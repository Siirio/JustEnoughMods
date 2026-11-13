#version 150

uniform sampler2D AtmosphereTex;
uniform sampler2D SceneDepth;
uniform mat4 InverseViewProjection;

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
    fragColor = accumulated * (smoothstep(0.0, 0.15, weight) / max(weight, 0.00001));
}
