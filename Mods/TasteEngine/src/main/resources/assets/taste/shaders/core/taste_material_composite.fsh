#version 150

#moj_import <taste:taste_reflection_capture.glsl>

uniform sampler2D SceneSampler;
uniform sampler2D OpaqueColor;
uniform sampler2D OpaqueDepth;
uniform sampler2D WorldDepth;
uniform sampler2D SurfaceData;
uniform sampler2D SurfaceDepth;
uniform sampler2D SurfaceOptics;
uniform float HasSurfaceOptics;
uniform sampler2D ReflectionSampler;
uniform sampler2D ReflectionPrevious;
uniform mat4 Projection;
uniform mat4 InverseProjection;
uniform mat4 View;
uniform mat4 InverseView;
uniform mat4 ReflectionPreviousProjection;
uniform mat4 ReflectionPreviousInverse;
uniform vec2 WaveOrigin;
uniform float VisualTime;
uniform float WaterAbsorption;
uniform float WaterClarity;
uniform float WaterIor;
uniform float WaterRefraction;
uniform float WaterWaveHeight;
uniform float WaterWaveLength;
uniform float WaterWaveSpeed;
uniform float WaterNormalStrength;
uniform float WaterFlowStrength;
uniform float WaterFoamWidth;
uniform float WaterFallFoam;
uniform float RippleCount;
uniform vec4 Ripple0;
uniform vec4 Ripple1;
uniform vec4 Ripple2;
uniform vec4 Ripple3;
uniform vec4 Ripple4;
uniform vec4 Ripple5;
uniform vec4 Ripple6;
uniform vec4 Ripple7;
uniform float GlassIor;
uniform float GlassThickness;
uniform float GlassTransmission;
uniform float GlassRefraction;
uniform float GlassRain;
uniform float GlassDropDensity;
uniform float GlassDropSize;
uniform float GlassDropSpeed;
uniform float IceRoughness;
uniform float IceFrost;
uniform float LavaCrust;
uniform float LavaWaveHeight;
uniform float LavaHeatDistortion;
uniform float LavaVisibility;
uniform float Underwater;
uniform float InLava;
uniform float Waterline;
uniform float UnderwaterDistortion;
uniform float ReflectionMode;
uniform float ReflectionSteps;
uniform float ReflectionDistance;
uniform float ReflectionThickness;
uniform float ReflectionRoughness;
uniform float ReflectionOnly;
uniform float ReflectionActive;
uniform float ReflectionHistoryWeight;
uniform float HasMaterial;
uniform float EmissionStrength;
uniform float LavaEmission;
uniform vec3 SoulColor;
uniform float CausticStrength;
uniform vec3 ShallowColor;
uniform vec3 DeepColor;
uniform vec3 SkyColor;

in vec2 texCoord;
out vec4 fragColor;

vec3 decodeDisplay(vec3 c) {
    c = max(c, vec3(0.0));
    return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c));
}

vec3 reconstruct(vec2 uv, float depth) {
    vec4 position = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return position.xyz / max(position.w, 0.000001);
}

vec3 surfaceNormal(vec2 oct) {
    vec3 n = vec3(oct, 1.0 - abs(oct.x) - abs(oct.y));
    float fold = max(0.0, -n.z);
    n.xy -= mix(vec2(-fold), vec2(fold), step(vec2(0.0), n.xy));
    return normalize(n);
}

vec3 tint(int value) {
    return decodeDisplay(vec3((value >> 16) & 255, (value >> 8) & 255, value & 255) / 255.0);
}

bool onScreen(vec2 uv) {
    return all(greaterThanEqual(uv, vec2(0.0))) && all(lessThanEqual(uv, vec2(1.0)));
}

vec2 project(vec3 view) {
    vec4 clip = Projection * vec4(view, 1.0);
    return clip.xy / max(clip.w, 0.000001) * 0.5 + 0.5;
}

float fresnel(float facing, float ior) {
    float ratio = (ior - 1.0) / (ior + 1.0);
    float f0 = ratio * ratio;
    return f0 + (1.0 - f0) * pow(1.0 - clamp(facing, 0.0, 1.0), 5.0);
}

vec2 flowDirection(int flowBits) {
    float angle = float(flowBits & 255) * 0.024639942;
    return vec2(cos(angle), sin(angle)) * (float((flowBits >> 8) & 255) / 255.0);
}

vec2 waveSlope(vec2 world, int flowBits, float height, float lengthBlocks, float speed) {
    vec2 flow = flowDirection(flowBits) * WaterFlowStrength;
    vec2 uv = world + WaveOrigin - flow * VisualTime * speed;
    float frequency = 6.2831853 / max(lengthBlocks, 0.1);
    float time = VisualTime * speed;
    vec2 a = vec2(0.94, 0.341);
    vec2 b = vec2(-0.36, 0.933);
    return height * frequency * (a * cos(dot(uv, a) * frequency - time)
         + 0.55 * b * cos(dot(uv, b) * frequency * 1.5 - time * 1.42));
}

vec4 rippleAt(int index) {
    if (index == 0) return Ripple0;
    if (index == 1) return Ripple1;
    if (index == 2) return Ripple2;
    if (index == 3) return Ripple3;
    if (index == 4) return Ripple4;
    if (index == 5) return Ripple5;
    if (index == 6) return Ripple6;
    return Ripple7;
}

vec2 rippleSlope(vec3 world) {
    vec2 slope = vec2(0.0);
    for (int i = 0; i < int(RippleCount); i++) {
        vec4 source = rippleAt(i);
        if (abs(world.y - source.y) > 0.45) continue;
        vec2 delta = world.xz - source.xz;
        float distance = length(delta);
        if (distance <= 0.001 || distance > 4.0) continue;
        float age = mod(source.w, 2.0);
        float strength = floor(source.w * 0.5) / 256.0;
        float ring = distance - age * 2.8;
        float envelope = exp(-ring * ring * 16.0 - age * 1.9) * (1.0 - smoothstep(2.8, 4.0, distance));
        slope += delta / distance * cos(ring * 18.0) * envelope * strength * 0.085;
    }
    return slope;
}

vec3 screenReflection(vec3 position, vec3 world, vec3 ray, vec3 worldRay, float roughness, vec3 base, bool water) {
    vec3 sky = SkyColor * mix(0.22, 1.0, clamp(worldRay.y, 0.0, 1.0));
    if (ReflectionMode < 0.5) return base;
    if (ReflectionMode < 1.5) return sky;
    vec4 probe = tasteLocalProbe(world, worldRay, roughness);
    vec3 miss = ReflectionMode > 2.5 ? mix(sky, probe.rgb, probe.a) : base;
    if (water) {
        vec4 planar = tastePlanarWater(world, ray.xy * 0.013);
        if (planar.a > 0.7) return mix(miss, planar.rgb, planar.a);
        miss = mix(miss, planar.rgb, planar.a);
    }
    if (roughness > ReflectionRoughness) return miss;
    float thickness = max(ReflectionThickness, 0.004);
    int steps = int(clamp(ReflectionSteps, 1.0, 128.0));
    float distanceLimit = max(ReflectionDistance, 0.1);
    float previous = 0.04;
    for (int i = 1; i <= steps; ++i) {
        float fraction = float(i) / float(steps);
        float distance = 0.06 + distanceLimit * fraction * fraction;
        vec3 point = position + ray * distance;
        if (point.z > -0.04) break;
        vec2 uv = project(point);
        if (!onScreen(uv)) break;
        float device = texture(OpaqueDepth, uv).r;
        if (device >= 1.0) { previous = distance; continue; }
        float z = reconstruct(uv, device).z;
        float delta = z - point.z;
        if (delta >= 0.0 && delta <= thickness) {
            float low = previous;
            float high = distance;
            for (int j = 0; j < 3; ++j) {
                float mid = (low + high) * 0.5;
                vec3 refined = position + ray * mid;
                vec2 refinedUV = project(refined);
                float opaque = reconstruct(refinedUV, texture(OpaqueDepth, refinedUV).r).z;
                if (opaque - refined.z > 0.0) high = mid;
                else low = mid;
            }
            vec2 hit = project(position + ray * high);
            float edge = min(min(hit.x, 1.0 - hit.x), min(hit.y, 1.0 - hit.y));
            float weight = smoothstep(0.0, 0.08, edge) * (1.0 - fraction) * (1.0 - roughness);
            return mix(miss, decodeDisplay(texture(OpaqueColor, hit).rgb), weight);
        }
        previous = distance;
    }
    return miss;
}

vec3 stabilizeReflection(vec3 color, vec3 world) {
    if (ReflectionHistoryWeight <= 0.0) return color;
    vec4 clip = ReflectionPreviousProjection * vec4(world, 1.0);
    if (clip.w <= 0.0) return color;
    vec3 old = clip.xyz / clip.w * 0.5 + 0.5;
    if (!onScreen(old.xy) || old.z <= 0.0 || old.z >= 1.0) return color;
    vec4 history = texture(ReflectionPrevious, old.xy);
    if (history.a >= 1.0) return color;
    vec4 expected = ReflectionPreviousInverse * vec4(old * 2.0 - 1.0, 1.0);
    vec4 stored = ReflectionPreviousInverse * vec4(old.xy * 2.0 - 1.0, history.a * 2.0 - 1.0, 1.0);
    if (abs(expected.z / expected.w - stored.z / stored.w) > max(ReflectionThickness, 0.025)) return color;
    vec3 margin = vec3(0.08) + color * 0.4;
    return mix(color, clamp(history.rgb, color - margin, color + margin), ReflectionHistoryWeight);
}

vec3 reflectionAt(float depth, vec3 fallback) {
    if (ReflectionActive < 0.5) return fallback;
    ivec2 size = textureSize(ReflectionSampler, 0);
    vec2 position = texCoord * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(position));
    vec2 fraction = fract(position);
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) {
        ivec2 cell = clamp(base + ivec2(x, y), ivec2(0), size - 1);
        vec4 item = texelFetch(ReflectionSampler, cell, 0);
        if (item.a >= 1.0) continue;
        vec2 uv = (vec2(cell) + 0.5) / vec2(size);
        float distance = abs(depth - reconstruct(uv, item.a).z);
        float confidence = 1.0 - smoothstep(0.035, max(0.15, abs(depth) * 0.024), distance);
        float weight = confidence * (x == 0 ? 1.0 - fraction.x : fraction.x) * (y == 0 ? 1.0 - fraction.y : fraction.y);
        sum += item.rgb * weight;
        total += weight;
    }
    return total > 0.00001 ? sum / total : fallback;
}

vec3 refracted(vec3 position, vec3 viewNormal, float ior, float strength) {
    vec3 incident = normalize(position);
    vec3 refractedRay = refract(incident, viewNormal, 1.0 / ior);
    vec2 offset = (refractedRay.xy - incident.xy) * strength;
    vec2 uv = clamp(texCoord + offset, vec2(0.0), vec2(1.0));
    float opaqueZ = reconstruct(uv, texture(OpaqueDepth, uv).r).z;
    if (opaqueZ > position.z + 0.03) uv = texCoord;
    return decodeDisplay(texture(OpaqueColor, uv).rgb);
}

float hash(vec2 cell) {
    vec3 p = fract(vec3(cell.xyx) * 0.1031);
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

vec2 glassDrops(vec3 world, vec3 normal, int exposed) {
    if (exposed == 0 || GlassRain <= 0.0 || GlassDropSize <= 0.0) return vec2(0.0);
    vec2 plane = vec2(abs(normal.x) > abs(normal.z) ? world.z : world.x, world.y);
    vec2 lattice = plane * 6.0;
    vec2 cell = floor(lattice);
    float id = hash(cell);
    if (id > clamp(GlassDropDensity * GlassRain * 0.4, 0.0, 1.0)) return vec2(0.0);
    vec2 center = vec2(hash(cell + 8.2), fract(hash(cell + 1.9) - VisualTime * GlassDropSpeed));
    vec2 drop = fract(lattice) - center;
    drop.y = (fract(drop.y + 0.5) - 0.5) * 0.55;
    float radius = GlassDropSize * 6.0;
    return normalize(vec3(drop, 0.02)).xy * (1.0 - smoothstep(radius * 0.55, radius, length(drop))) * GlassRain;
}

void main() {
    vec4 base = texture(SceneSampler, texCoord);
    vec4 data = texture(SurfaceData, texCoord);
    int flags = HasMaterial > 0.5 ? int(data.z + 0.5) : 0;
    int family = flags & 15;
    int aux = flags >> 4;
    float surfaceDepth = texture(SurfaceDepth, texCoord).r;
    float opaqueDepth = texture(OpaqueDepth, texCoord).r;
    vec3 position = reconstruct(texCoord, surfaceDepth);
    vec3 opaquePosition = reconstruct(texCoord, opaqueDepth);
    float frontDepth = reconstruct(texCoord, texture(WorldDepth, texCoord).r).z;
    float thickness = max(0.0, position.z - opaquePosition.z);
    vec3 color = base.rgb;
    bool visible = family != 0 && surfaceDepth < 1.0 && position.z >= max(frontDepth, opaquePosition.z) - 0.035;
    if (ReflectionOnly > 0.5 && (!visible || (family != 1 && family != 2 && family != 4))) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    if (visible) {
        vec3 normal = surfaceNormal(data.xy);
        vec3 world = (InverseView * vec4(position, 1.0)).xyz;
        vec3 materialTint = tint(int(data.a));
        vec4 optics = HasSurfaceOptics > 0.5 ? texture(SurfaceOptics, texCoord) : vec4(0.0);
        if (family == 1) {
            vec2 slope = waveSlope(world.xz, aux, WaterWaveHeight, WaterWaveLength, WaterWaveSpeed) + rippleSlope(world);
            normal = normalize(normal + vec3(-slope.x, 0.0, -slope.y) * WaterNormalStrength);
        } else if (family == 2) {
            vec2 drops = glassDrops(world, normal, aux & 1);
            vec3 lateral = normalize(vec3(normal.z, 0.0001, -normal.x));
            normal = normalize(normal + lateral * drops.x * 0.36 + vec3(0.0, drops.y * 0.36, 0.0));
        }
        vec3 vn = normalize(mat3(View) * normal);
        if (dot(vn, -position) < 0.0) vn = -vn;
        vec3 reflected = reflect(normalize(position), vn);
        vec3 worldRay = normalize(mat3(InverseView) * reflected);
        float facing = max(0.0, dot(normalize(-position), vn));
        if (ReflectionOnly > 0.5) {
            float roughness = family == 1 ? 0.05 : family == 2 ? 0.08 : IceRoughness;
            vec3 reflectedColor = screenReflection(position + vn * 0.06, world, reflected, worldRay, roughness, base.rgb, family == 1);
            fragColor = vec4(stabilizeReflection(reflectedColor, world), surfaceDepth);
            return;
        }
        if (family == 1) {
            float ior = clamp(WaterIor + (optics.a > 0.5 ? optics.r - 1.333 : 0.0), 1.0, 3.0);
            float depth = min(thickness, 96.0);
            vec3 absorption = mix(ShallowColor, DeepColor, 1.0 - exp(-depth * WaterAbsorption));
            vec3 transmission = exp(-depth * WaterAbsorption * mix(vec3(1.3), vec3(1.0, 0.68, 0.43), WaterClarity));
            if (optics.a > 0.5) transmission *= 1.0 - clamp(optics.g, 0.0, 1.0);
            vec3 behind = refracted(position, vn, ior, WaterRefraction);
            vec3 water = behind * transmission + absorption * (1.0 - transmission);
            vec3 reflection = reflectionAt(position.z, water);
            color = mix(water, reflection, fresnel(facing, ior));
            float shore = WaterFoamWidth > 0.0 ? 1.0 - smoothstep(0.0, WaterFoamWidth, depth) : 0.0;
            float falls = WaterFallFoam * (1.0 - abs(normal.y)) * length(flowDirection(aux));
            float grain = smoothstep(0.32, 0.72, hash(floor(world.xz * 9.0 + VisualTime)));
            color = mix(color, vec3(0.8, 0.9, 0.92), clamp((shore + falls * 0.4) * grain * 0.65, 0.0, 0.8));
        } else if (family == 2) {
            float ior = clamp(GlassIor + (optics.a > 0.5 ? optics.r - 1.5 : 0.0), 1.0, 3.0);
            vec3 transmission = pow(max(materialTint, vec3(0.05)), vec3(GlassThickness * 10.0 / max(facing, 0.22)));
            vec3 behind = refracted(position, vn, ior, GlassRefraction * min(0.13, GlassThickness * 0.6));
            vec3 glass = behind * mix(vec3(1.0), transmission, GlassTransmission);
            if (optics.a > 0.5) glass = mix(glass, base.rgb * materialTint, clamp(optics.g, 0.0, 1.0));
            vec3 reflection = reflectionAt(position.z, glass);
            color = mix(glass, reflection, fresnel(facing, ior));
        } else if (family == 3) {
            vec2 slope = waveSlope(world.xz, aux, LavaWaveHeight, 3.0, 0.45);
            vec2 uv = clamp(texCoord + slope * LavaHeatDistortion, vec2(0.0), vec2(1.0));
            float otherDepth = texture(SurfaceDepth, uv).r;
            vec4 other = texture(SurfaceData, uv);
            if ((int(other.z + 0.5) & 15) == 3 && abs(reconstruct(uv, otherDepth).z - position.z) < 0.45)
                color = texture(SceneSampler, uv).rgb;
            float crust = smoothstep(0.37, 0.68, hash(floor(world.xz * 5.0)));
            color = mix(color + materialTint * LavaEmission * EmissionStrength, color * 0.62, crust * LavaCrust);
        } else if (family == 4) {
            vec3 reflection = reflectionAt(position.z, base.rgb);
            float frost = IceFrost * smoothstep(0.6, 0.9, hash(floor(world.xz * 10.0)));
            color = mix(mix(base.rgb, reflection, fresnel(facing, 1.31)), vec3(0.65, 0.78, 0.86), frost * 0.4);
        } else {
            vec3 emission = family == 9 ? SoulColor * max(0.1, max(materialTint.r, max(materialTint.g, materialTint.b)))
                    : materialTint;
            color = base.rgb + emission * (float(aux) / 256.0) * EmissionStrength;
        }
    } else if (Underwater > 0.5 && UnderwaterDistortion > 0.0) {
        vec2 distortion = vec2(sin(texCoord.y * 36.0 + VisualTime), cos(texCoord.x * 43.0 + VisualTime * 1.3));
        vec2 uv = clamp(texCoord + distortion * UnderwaterDistortion, vec2(0.0), vec2(1.0));
        bool touchesSurface = HasMaterial > 0.5 && (int(texture(SurfaceData, uv).z + 0.5) & 15) == 1;
        if (Waterline < 0.5 || !touchesSurface) color = texture(SceneSampler, uv).rgb;
    }
    if (Underwater > 0.5 && opaqueDepth < 1.0 && CausticStrength > 0.0 && base.a > 0.0) {
        vec3 world = (InverseView * vec4(opaquePosition, 1.0)).xyz;
        vec2 lattice = (world.xz + WaveOrigin) * 5.0;
        float first = abs(sin(lattice.x + sin(lattice.y * 0.73 + VisualTime * 0.9) + VisualTime));
        float second = abs(sin(lattice.y * 1.1 + cos(lattice.x * 0.67 - VisualTime * 0.72)));
        float ridges = pow(1.0 - min(first, second), 5.0);
        color += color * ridges * CausticStrength * base.a;
    }
    if (InLava > 0.5) {
        float amount = 1.0 - exp(-length(opaquePosition) / max(0.1, LavaVisibility));
        color = mix(color, vec3(0.9, 0.12, 0.015), amount);
    }
    fragColor = vec4(max(color, vec3(0.0)), base.a);
}
