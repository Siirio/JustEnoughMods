#version 150

uniform sampler2D SceneDepth;
uniform sampler2D CaveSmokeMask;
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
uniform vec4 RiverBank0;
uniform vec4 RiverBank1;
uniform vec4 RiverBank2;
uniform vec4 CaveBank;
uniform vec4 SnowBank;
uniform vec4 GroundWind;
uniform vec3 CaveSmokeColor;

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

// Intersect an air bank with the visible ray and return a bounded optical depth.
vec3 bankIntersection(vec3 ray, float sceneDistance, vec3 center, vec3 radii) {
    vec3 origin = -center / radii;
    vec3 direction = ray / radii;
    float a = dot(direction, direction);
    float b = dot(origin, direction);
    float c = dot(origin, origin) - 1.0;
    float h = b * b - a * c;
    if (h <= 0.0) {
        return vec3(0.0);
    }
    float root = sqrt(h);
    float nearEdge = max(0.0, (-b - root) / a);
    float farEdge = min(sceneDistance, (-b + root) / a);
    return vec3(max(0.0, farEdge - nearEdge), (nearEdge + farEdge) * 0.5, sqrt(clamp(h / a, 0.0, 1.0)));
}

float riverMist(vec3 ray, float sceneDistance, vec4 bank, float phase) {
    if (bank.w <= 0.002) {
        return 0.0;
    }
    vec2 drift = GroundWind.xy * sin(AtmosphereTime * 0.49 + phase) * 1.95;
    vec3 center = bank.xyz + vec3(drift.x, 0.7 + 0.12 * sin(AtmosphereTime * 0.38 + phase), drift.y);
    vec3 hit = bankIntersection(ray, sceneDistance, center, vec3(10.5, 1.14, 8.8));
    return bank.w * hit.z * (1.0 - exp(-hit.x * 0.13)) * 0.25;
}

float caveLayer(vec3 ray, float sceneDistance, vec3 center, vec3 radii, float phase) {
    vec3 hit = bankIntersection(ray, sceneDistance, center, radii);
    if (hit.x <= 0.0) {
        return 0.0;
    }
    vec3 samplePosition = (ray * hit.y - center) / radii;
    vec2 uv = vec2(samplePosition.x * 0.40 + 0.5, samplePosition.y * 0.38 + 0.5);
    uv.x = fract(uv.x + samplePosition.z * 0.21 + 0.08 * sin(AtmosphereTime * 0.38 + phase));
    float mask = texture(CaveSmokeMask, uv).a;
    return hit.z * (1.0 - exp(-hit.x * 0.10)) * mix(0.38, 0.90, mask);
}

float smokyCaveAir(vec3 ray, float sceneDistance) {
    if (CaveBank.w <= 0.002) {
        return 0.0;
    }
    float slow = AtmosphereTime * 0.24;
    vec3 lowCenter = CaveBank.xyz + vec3(3.4 * sin(slow), 0.7 + 0.15 * sin(slow * 1.5), 2.0 * cos(slow));
    vec3 highCenter = CaveBank.xyz + vec3(-3.7 * sin(slow + 1.8), 2.4 + 0.30 * sin(slow), -1.8 * cos(slow * 0.72));
    float low = caveLayer(ray, sceneDistance, lowCenter, vec3(17.0, 0.85, 12.0), 0.0);
    float high = caveLayer(ray, sceneDistance, highCenter, vec3(23.0, 1.12, 15.0), 1.8);
    float clearNear = smoothstep(2.0, 8.0, sceneDistance);
    return CaveBank.w * clearNear * (low * 0.30 + high * 0.23);
}

float snowGust(vec3 ray, float sceneDistance) {
    if (SnowBank.w <= 0.002) {
        return 0.0;
    }
    vec2 forward = GroundWind.zw;
    vec2 across = vec2(-forward.y, forward.x);
    vec3 localRay = vec3(dot(ray.xz, forward), ray.y, dot(ray.xz, across));
    vec3 front = SnowBank.xyz + vec3(0.0, 0.45, 0.0);
    vec3 lead = vec3(dot(front.xz, forward), front.y, dot(front.xz, across));
    vec3 trail = lead + vec3(-6.6, 0.65 + 0.26 * sin(AtmosphereTime * 1.2), -1.5);
    vec3 nearBank = bankIntersection(localRay, sceneDistance, lead, vec3(8.8, 0.90, 7.0));
    vec3 farBank = bankIntersection(localRay, sceneDistance, trail, vec3(10.0, 1.25, 9.0));
    float frontAlpha = nearBank.z * (1.0 - exp(-nearBank.x * 0.13));
    float tailAlpha = farBank.z * (1.0 - exp(-farBank.x * 0.09));
    return SnowBank.w * (0.44 * frontAlpha + 0.23 * tailAlpha);
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

    if (!submerged) {
        float river = riverMist(ray, sceneDistance, RiverBank0, 0.0)
                + riverMist(ray, sceneDistance, RiverBank1, 2.2)
                + riverMist(ray, sceneDistance, RiverBank2, 4.4);
        scatter += vec4(vec3(0.76, 0.82, 0.78) * river, river) * (1.0 - scatter.a);
        float smoke = smokyCaveAir(ray, sceneDistance);
        scatter += vec4(CaveSmokeColor * smoke, smoke) * (1.0 - scatter.a);
        float powder = snowGust(ray, sceneDistance);
        scatter += vec4(vec3(0.84, 0.90, 0.95) * powder, powder) * (1.0 - scatter.a);
    }

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
