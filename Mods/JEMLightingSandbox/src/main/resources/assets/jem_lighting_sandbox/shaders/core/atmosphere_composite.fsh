#version 150

uniform sampler2D AtmosphereTex;
uniform sampler2D SceneDepth;
uniform sampler2D SceneColor;
uniform mat4 InverseViewProjection;
uniform float Underwater;
uniform vec4 AirFog;
uniform vec4 AirFogGeometry;
uniform vec4 FogWind;
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

vec4 airFog(vec4 target, bool sky) {
    float targetLength = length(target.xyz);
    vec3 ray = target.xyz / max(targetLength, 0.000001);
    float distance = sky ? AirFogGeometry.w : targetLength / max(abs(target.w), 0.000001);
    float start = AirFogGeometry.x;
    float altitude = AirFogGeometry.y;
    float enclosed = AirFogGeometry.z;
    float span = max(distance - start, 0.0);
    float width = 1.6 / AirFog.a;
    vec2 wind = FogWind.xy;
    vec3 point = ray * (start + min(span * 0.32, 35.0));
    float layerHeight = altitude + point.y;
    vec2 location = FogWind.zw + point.xz - wind * (2.8 * clamp(layerHeight, -8.0, 18.0));
    vec2 phase = location * (6.2831853 / 1024.0);
    float band = sin(dot(phase, vec2(23.0, 15.0)) + 0.7 * sin(dot(phase, vec2(-9.0, 29.0))));
    float variation = mix(1.0, 0.92 + 0.19 * band, 1.0 - 0.45 * enclosed);
    float exposure = mix(exp(-max(layerHeight - 9.0, 0.0) * 0.022), 1.0, enclosed);
    float opacity = smoothstep(0.0, width, span * exposure * variation);
    if (sky) {
        float horizon = 1.0 - smoothstep(0.01, 0.24, abs(ray.y));
        opacity *= mix(horizon * 0.38, 0.7, enclosed);
    }
    float upper = smoothstep(1.0, 23.0, layerHeight);
    vec3 color = AirFog.rgb * mix(1.0, 0.63, upper);
    return vec4(color * opacity, opacity);
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
    if (AirFog.a > 0.0) {
        vec4 target = InverseViewProjection * vec4(sceneUv * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
        scatter += airFog(target, centerSky) * (1.0 - scatter.a);
    }
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
