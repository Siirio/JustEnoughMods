#version 150

uniform sampler2D SceneDepth;
uniform mat4 InverseViewProjection;
uniform vec3 SunDirection;
uniform float SunStrength;
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

    vec4 scatter = vec4(WaterColor * haze, haze);

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
