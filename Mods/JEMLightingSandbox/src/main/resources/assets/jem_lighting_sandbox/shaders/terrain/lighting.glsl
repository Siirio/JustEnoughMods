uniform int jemActive;
uniform sampler2D jemChunkLights;
uniform ivec4 jemChunkLayout;
uniform int jemShadowSamples;
uniform vec4 jemSunDirectionStrength;
uniform vec3 jemSunColor;
uniform int jemEntityShadowActive;
uniform sampler2D jemEntityShadowTexture;
uniform vec3 jemEntityShadowOrigin;
uniform vec2 jemEntityShadowDirection;
uniform sampler2D jemWaterScene;
uniform sampler2D jemWaterDepth;
uniform int jemWaterActive;
uniform mat4 jemWaterMatrix;
uniform float jemWaterTime;
uniform vec3 jemWaterSky;
uniform vec2 jemWaterOrigin;
uniform vec4 jemCloud;
uniform sampler2D jemCloudTexture;

vec4 jemWaterSurface(vec4 base, vec3 position, vec2 lightLevels) {
    if (jemWaterActive == 0 || length(position) > 64.0) {
        return base;
    }
    vec2 world = position.xz + jemWaterOrigin;
    vec3 normal = normalize(vec3(0.11 * sin(world.x * 1.276272 + jemWaterTime * 1.6), 1.0,
            0.1 * sin(world.y * 1.521709 - jemWaterTime * 1.9)));
    vec3 incident = normalize(position);
    if (incident.y >= -0.01) {
        return base;
    }
    vec2 size = vec2(textureSize(jemWaterScene, 0));
    vec2 uv = gl_FragCoord.xy / size;
    vec2 bent = clamp(uv + normal.xz * 0.055, vec2(0.002), vec2(0.998));
    float depth = texture(jemWaterDepth, bent).r;
    if (depth < gl_FragCoord.z) {
        bent = uv;
    }
    vec3 through = texture(jemWaterScene, bent).rgb;
    vec3 reflected = jemWaterSky * smoothstep(0.45, 1.0, lightLevels.y);
    vec3 reflection = reflect(incident, normal);
    for (int sampleIndex = 1; sampleIndex <= 10; ++sampleIndex) {
        vec3 samplePoint = position + reflection * (0.4 + float(sampleIndex * sampleIndex) * 0.20);
        vec4 clip = jemWaterMatrix * vec4(samplePoint, 1.0);
        if (clip.w <= 0.0) break;
        vec3 screen = clip.xyz / clip.w * 0.5 + 0.5;
        if (any(lessThan(screen.xy, vec2(0.004))) || any(greaterThan(screen.xy, vec2(0.996)))) break;
        float opaque = texture(jemWaterDepth, screen.xy).r;
        float separation = screen.z - opaque;
        if (opaque < 1.0 && separation > 0.0 && separation < 0.006) {
            reflected = texture(jemWaterScene, screen.xy).rgb;
            break;
        }
    }
    float fresnel = 0.025 + 0.70 * pow(1.0 - clamp(dot(-incident, normal), 0.0, 1.0), 5.0);
    float clarity = 0.68 - 0.36 * clamp(base.a, 0.0, 1.0);
    vec3 water = mix(base.rgb, through * (0.65 + base.rgb * 0.35), clarity);
    return vec4(mix(water, reflected, fresnel), 1.0);
}

const int JEM_MAX_LIGHTS = 8;
const int JEM_LIGHT_MASK_OFFSET = 17;
const int JEM_LIGHT_ROW_TEXELS = 19;

ivec2 jemChunkLightRow(ivec3 relativeBlock, out vec3 sectionOrigin, out int lightCount,
        out uint candidates, out uint areaCandidates) {
    lightCount = 0;
    candidates = 0u;
    areaCandidates = 0u;
    ivec3 cameraSection = jemCameraBlock >> 4;
    ivec3 relativeSection = relativeBlock >> 4;
    ivec3 section = cameraSection + relativeSection;
    ivec2 offset = abs(relativeSection.xz);
    int height = section.y - jemChunkLayout.y;
    if (max(offset.x, offset.y) > jemChunkLayout.z || height < 0 || height >= jemChunkLayout.x) {
        return ivec2(-1);
    }
    int side = jemChunkLayout.w;
    ivec2 slot = ((section.xz % side) + side) % side;
    ivec2 row = ivec2(slot.x * JEM_LIGHT_ROW_TEXELS, slot.y * jemChunkLayout.x + height);
    vec4 header = texelFetch(jemChunkLights, row, 0);
    if (header.w < 0.5 || any(notEqual(ivec2(header.xy), section.xz))) {
        return ivec2(-1);
    }
    sectionOrigin = vec3((section << 4) - jemCameraBlock) - jemCameraFraction;
    lightCount = min(int(header.z), JEM_MAX_LIGHTS);
    if (lightCount > 0) {
        ivec3 local = (relativeBlock & 15) >> 3;
        int octant = local.x | (local.z << 1) | (local.y << 2);
        vec4 masks = texelFetch(jemChunkLights, row + ivec2(JEM_LIGHT_MASK_OFFSET + (octant >> 2), 0), 0);
        candidates = uint(masks[octant & 3]);
        areaCandidates = candidates & uint(int(header.w) - 1);
    }
    return row;
}

float jemWorldShadowWeight(vec3 normal, float skyLevel) {
    float strength = jemSunDirectionStrength.w * smoothstep(0.45, 1.0, skyLevel);
    return strength * smoothstep(0.0, 0.12, dot(normal, jemSunDirectionStrength.xyz));
}

float jemCloudOcclusion(vec3 position, float skyLevel) {
    if (jemCloud.w <= 0.0 || jemSunDirectionStrength.w == 0.0
            || jemSunDirectionStrength.y <= 0.10 || skyLevel <= 0.5 || position.y >= jemCloud.z) {
        return 0.0;
    }
    float travel = (jemCloud.z - position.y) / jemSunDirectionStrength.y;
    vec2 uv = fract(jemCloud.xy + (position.xz + jemSunDirectionStrength.xz * travel) / 3072.0);
    float coverage = textureLod(jemCloudTexture, uv, 0.0).a;
    return coverage * jemCloud.w * jemSunDirectionStrength.w * smoothstep(0.5, 0.95, skyLevel);
}

uniform sampler2D jemEntityModels;
uniform int jemEntityCasterCount;

bool jemHitsTriangle(vec3 origin, vec3 direction, float limit, vec3 a, vec3 b, vec3 c) {
    vec3 ab = b - a;
    vec3 ac = c - a;
    vec3 p = cross(direction, ac);
    float determinant = dot(ab, p);
    if (abs(determinant) < 0.00001) return false;
    vec3 s = origin - a;
    float u = dot(s, p) / determinant;
    if (u < 0.0 || u > 1.0) return false;
    vec3 q = cross(s, ab);
    float v = dot(direction, q) / determinant;
    float t = dot(ac, q) / determinant;
    return v >= 0.0 && u + v <= 1.0 && t > 0.035 && t < limit - 0.02;
}

uint jemAreaEntityMask(vec3 position, vec3 sampleCenter) {
    // The box encloses every sample segment, including the near-zero direction clamp.
    vec3 low = min(position, sampleCenter - vec3(0.501)) - jemEntityShadowOrigin - vec3(0.001);
    vec3 high = max(position, sampleCenter + vec3(0.501)) - jemEntityShadowOrigin + vec3(0.001);
    uint mask = 0u;
    for (int caster = 0; caster < jemEntityCasterCount; caster++) {
        vec3 minimum = texelFetch(jemEntityModels, ivec2(0, caster), 0).xyz;
        vec3 maximum = texelFetch(jemEntityModels, ivec2(1, caster), 0).xyz;
        if (all(lessThanEqual(minimum, high)) && all(greaterThanEqual(maximum, low))) {
            mask |= 1u << uint(caster);
        }
    }
    return mask;
}

bool jemPointEntityShadow(vec3 position, vec3 direction, float distance, uint casterMask) {
    vec3 origin = position - jemEntityShadowOrigin;
    vec3 safeDirection = max(abs(direction), vec3(0.00001)) * mix(vec3(-1.0), vec3(1.0), step(0.0, direction));
    for (int caster = 0; caster < jemEntityCasterCount && casterMask != 0u; caster++, casterMask >>= 1u) {
        if ((casterMask & 1u) == 0u) continue;
        vec4 minimum = texelFetch(jemEntityModels, ivec2(0, caster), 0);
        vec4 maximum = texelFetch(jemEntityModels, ivec2(1, caster), 0);
        vec3 t0 = (minimum.xyz - origin) / safeDirection;
        vec3 t1 = (maximum.xyz - origin) / safeDirection;
        vec3 enter = min(t0, t1);
        vec3 leave = max(t0, t1);
        if (max(0.0, max(enter.x, max(enter.y, enter.z))) >
                min(distance, min(leave.x, min(leave.y, leave.z)))) continue;
        for (int row = int(minimum.w); row < int(maximum.w); row++) {
            vec3 a = texelFetch(jemEntityModels, ivec2(0, row), 0).xyz;
            vec3 b = texelFetch(jemEntityModels, ivec2(1, row), 0).xyz;
            vec3 c = texelFetch(jemEntityModels, ivec2(2, row), 0).xyz;
            if (jemHitsTriangle(origin, direction, distance, a, b, c)) return true;
            vec3 d = texelFetch(jemEntityModels, ivec2(3, row), 0).xyz;
            if (jemHitsTriangle(origin, direction, distance, a, c, d)) return true;
        }
    }
    return false;
}

float jemEntityOcclusion(vec3 position, vec3 normal, float skyLevel) {
    if (jemEntityShadowActive == 0) {
        return 0.0;
    }
    vec2 direction = jemEntityShadowDirection;
    float strength = jemSunDirectionStrength.w * smoothstep(0.45, 1.0, skyLevel)
            * smoothstep(0.0, 0.12, dot(normal.xy, direction));
    if (strength <= 0.0) {
        return 0.0;
    }
    vec3 relative = position - jemEntityShadowOrigin;
    vec2 uv = vec2(dot(relative.xy, vec2(direction.y, -direction.x)), relative.z) / 48.0 + 0.5;
    vec2 edge = min(uv, 1.0 - uv);
    float coverage = smoothstep(0.004, 0.045, min(edge.x, edge.y));
    if (coverage <= 0.0) {
        return 0.0;
    }
    float depth = dot(relative.xy, direction);
    vec2 texel = 1.0 / vec2(textureSize(jemEntityShadowTexture, 0));
    float occlusion = 0.0;
    for (int y = -1; y <= 1; y += 2) {
        for (int x = -1; x <= 1; x += 2) {
            float sampleDepth = textureLod(jemEntityShadowTexture, uv + vec2(x, y) * texel * 0.45, 0.0).r;
            float travel = sampleDepth - depth;
            occlusion += step(0.06, travel) * (1.0 - smoothstep(21.0, 24.0, travel));
        }
    }
    return 0.25 * strength * coverage * occlusion;
}

float jemPointTransmission(JemRayOrigin origin, vec3 cameraOrigin, vec3 direction, float distance, ivec3 sourceCell,
        uint casterMask) {
    float energy = jemVisibility(origin, direction, distance, sourceCell);
    if (energy > 0.0 && casterMask != 0u && jemPointEntityShadow(cameraOrigin, direction, distance, casterMask)) {
        return 0.0;
    }
    return energy;
}

float jemBlockFaceLight(JemRayOrigin rayOrigin, vec3 cameraOrigin, vec3 normal, vec3 center, int grid, uint casterMask) {
    vec3 origin = rayOrigin.position;
    if (casterMask != 0u && grid > 1) {
        casterMask = jemAreaEntityMask(cameraOrigin, center - origin + cameraOrigin);
    }
    vec3 delta = origin - center;
    vec3 faceSign = sign(delta);
    vec3 absoluteDelta = abs(delta);
    bvec3 faces = greaterThan(absoluteDelta, vec3(0.501));
    // One face contributes at most one unit of projected area to the denominator.
    bool accumulateWeight = dot(vec3(faces), vec3(1.0)) > 1.0;
    vec3 absoluteNormal = abs(normal);
    float cell = 1.0 / float(grid);
    float weight = cell * cell;
    float light = 0.0;
    float totalWeight = 0.0;
    for (int axis = 0; axis < 3; axis++) {
        if (!faces[axis]) continue;
        vec3 faceNormal = vec3(0.0);
        faceNormal[axis] = faceSign[axis];
        int side = (axis + 1) % 3;
        int up = (axis + 2) % 3;
        vec3 faceCenter = center + faceNormal * 0.501;
        // Bound the cosine over the entire face. Keep non-facing projection weight if other faces can use it.
        float facingBound = dot(normal, faceCenter - origin) + 0.5 * (absoluteNormal[side] + absoluteNormal[up]);
        bool facesReceiver = facingBound > -0.0001;
        if (!facesReceiver && !accumulateWeight) continue;
        for (int y = 0; y < grid; y++) {
            for (int x = 0; x < grid; x++) {
                vec3 samplePosition = faceCenter;
                samplePosition[side] += (float(x) + 0.5) * cell - 0.5;
                samplePosition[up] += (float(y) + 0.5) * cell - 0.5;
                vec3 ray = samplePosition - origin;
                float travel = length(ray);
                vec3 direction = ray / max(travel, 0.00001);
                float facing = facesReceiver ? max(0.0, dot(normal, direction)) : 0.0;
                float projectedArea = weight * max(0.0, -faceSign[axis] * direction[axis]);
                if (accumulateWeight) {
                    totalWeight += projectedArea;
                }
                if (facing > 0.0 && projectedArea > 0.0) {
                    float visibility = jemPointTransmission(rayOrigin, cameraOrigin, direction, travel, JEM_NO_SOURCE,
                            casterMask);
                    light += visibility * projectedArea * mix(facing, sqrt(facing), 0.55);
                }
            }
        }
    }
    return light / max(1.0, totalWeight);
}

vec3 jemLinearColor(vec3 color) {
    return pow(max(color, vec3(0.0)), vec3(2.2));
}

int jemAreaLightCount(ivec2 row, int lightCount, uint candidates, vec3 sectionOrigin, vec3 position, vec3 rayOrigin,
        vec3 cameraInSection, ivec3 receiverCell, vec3 normal, out int dominant, out uint areaMask,
        out uint inRangeMask) {
    int count = 0;
    dominant = -1;
    areaMask = 0u;
    inRangeMask = 0u;
    float topEnergy = -1.0;
    float facingMargin = 0.501 * dot(abs(normal), vec3(1.0)) + 0.00001;
    for (int index = 0; index < lightCount; index++) {
        if ((candidates & (1u << uint(index))) == 0u) continue;
        vec4 source = texelFetch(jemChunkLights, row + ivec2(1 + index * 2, 0), 0);
        source.xyz += sectionOrigin;
        vec3 toLight = source.xyz - position;
        float distanceSquared = dot(toLight, toLight);
        if (distanceSquared >= source.w * source.w || distanceSquared < 1.0e-6) continue;
        vec3 center = source.xyz + cameraInSection;
        if (all(equal(receiverCell, ivec3(floor(center))))) continue;
        inRangeMask |= 1u << uint(index);

        vec3 ray = center - rayOrigin;
        if (dot(normal, toLight) <= -0.87 || all(lessThanEqual(abs(ray), vec3(0.501)))
                || dot(normal, ray) < -facingMargin) continue;

        vec4 properties = texelFetch(jemChunkLights, row + ivec2(2 + index * 2, 0), 0);
        if (properties.w >= 0.0) continue;
        areaMask |= 1u << uint(index);
        count++;
        float fade = smoothstep(0.0, 2.0, source.w - sqrt(distanceSquared));
        float channel = max(properties.r, max(properties.g, properties.b));
        float energy = fade * -properties.w * channel / (1.0 + 0.08 * distanceSquared);
        if (energy > topEnergy) {
            topEnergy = energy;
            dominant = index;
        }
    }
    return count;
}

int jemSecondaryAreaGrid(int otherCount, int secondary) {
    // At most 48 secondary rays across the three eligible faces of each source.
    if (otherCount == 1) return 3;
    if (otherCount <= 4 || secondary < 3) return 2;
    return 1;
}

vec3 jemApplyLighting(vec3 nativeColor, vec3 skyBaseColor, vec3 albedo, vec3 position, vec2 lightLevels,
        vec3 tangentX, vec3 tangentY) {
    if (jemActive == 0) {
        return nativeColor;
    }

    // Scale each derivative before crossing so the cutoff measures angle, not pixel size.
    tangentX *= inversesqrt(max(dot(tangentX, tangentX), 1.0e-20));
    tangentY *= inversesqrt(max(dot(tangentY, tangentY), 1.0e-20));
    vec3 normalVector = cross(tangentX, tangentY);
    float normalSquared = dot(normalVector, normalVector);
    if (normalSquared < 1.0e-8) {
        return nativeColor;
    }
    vec3 normal = normalVector * inversesqrt(normalSquared);
    if (dot(normal, -position) < 0.0) {
        normal = -normal;
    }
    vec3 cameraInSection = vec3(jemCameraBlock & 15) + jemCameraFraction;
    vec3 receiverPosition = position - normal * JEM_SURFACE_BIAS;
    ivec3 receiverCell = ivec3(floor(receiverPosition + cameraInSection));
    vec3 cameraRayOrigin = position + normal * JEM_SURFACE_BIAS;
    vec3 rayOrigin = cameraRayOrigin + cameraInSection;
    vec3 sectionOrigin = vec3(0.0);
    int lightCount;
    uint candidates;
    uint areaCandidates;
    ivec2 lightRow = jemChunkLightRow(receiverCell, sectionOrigin, lightCount, candidates, areaCandidates);

    float sunlightOcclusion = 0.0;
    float cloudOcclusion = 0.0;
    float worldShadowWeight = 0.0;
    if (lightLevels.y > 0.45 && any(greaterThan(min(nativeColor, skyBaseColor), vec3(0.0)))) {
        sunlightOcclusion = jemEntityOcclusion(position, normal, lightLevels.y);
        worldShadowWeight = jemWorldShadowWeight(normal, lightLevels.y);
        cloudOcclusion = jemCloudOcclusion(position, lightLevels.y);
    }
    bool traceSun = worldShadowWeight > sunlightOcclusion;
    if (lightCount == 0 && !traceSun && sunlightOcclusion <= 0.0 && cloudOcclusion <= 0.0) {
        return nativeColor;
    }

    JemRayOrigin preparedRay;
    if (traceSun || candidates != 0u) {
        preparedRay = jemPrepareRayOrigin(rayOrigin);
    }
    if (traceSun) {
        float visibility = jemVisibility(preparedRay, jemSunDirectionStrength.xyz,
                JEM_SUN_SHADOW_REACH, JEM_NO_SOURCE);
        sunlightOcclusion = max(sunlightOcclusion, worldShadowWeight * (1.0 - visibility));
    }
    if (lightCount == 0 && sunlightOcclusion <= 0.0 && cloudOcclusion <= 0.0) {
        return nativeColor;
    }

    vec3 contributedLight = vec3(0.0);
    float modeledBlockLevel = 0.0;
    int dominant = -1;
    uint areaMask = 0u;
    uint inRangeMask = 0u;
    int areaCount = 0;
    bool limitOverlap = (areaCandidates & (areaCandidates - 1u)) != 0u
            && (lightCount > 2 || jemShadowSamples > 2);
    if (limitOverlap) {
        areaCount = jemAreaLightCount(lightRow, lightCount, candidates, sectionOrigin, position, rayOrigin,
                cameraInSection, receiverCell, normal, dominant, areaMask, inRangeMask);
        candidates = inRangeMask;
    }
    int secondary = 0;
    uint casterMask = (1u << uint(jemEntityCasterCount)) - 1u;
    bool albedoCanReflect = any(greaterThan(albedo, vec3(0.0)));
    for (int index = 0; index < lightCount; ++index) {
        if ((candidates & (1u << uint(index))) == 0u) continue;
        vec4 source = texelFetch(jemChunkLights, lightRow + ivec2(1 + index * 2, 0), 0);
        source.xyz += sectionOrigin;
        vec3 toLight = source.xyz - position;
        float distanceSquared = dot(toLight, toLight);
        if (!limitOverlap && (distanceSquared >= source.w * source.w || distanceSquared < 1.0e-6)) {
            continue;
        }
        vec3 lightPosition = source.xyz + cameraInSection;
        ivec3 sourceCell = ivec3(floor(lightPosition));
        if (!limitOverlap && all(equal(receiverCell, sourceCell))) {
            continue;
        }

        vec4 properties = texelFetch(jemChunkLights, lightRow + ivec2(2 + index * 2, 0), 0);
        float emission = abs(properties.w);
        float distanceFade = smoothstep(0.0, 2.0, source.w - sqrt(distanceSquared));
        vec3 absoluteDelta = abs(vec3(sourceCell) + 0.5 - rayOrigin);
        float taxicabDistance = absoluteDelta.x + absoluteDelta.y + absoluteDelta.z;
        float lightLevel = max(0.0, emission - max(0.0, taxicabDistance - 1.0) / 15.0);
        modeledBlockLevel = max(modeledBlockLevel, distanceFade * lightLevel);
        float attenuation = distanceFade * 1.35 * emission / (1.0 + 0.08 * distanceSquared);
        if (attenuation <= 0.0 || !albedoCanReflect) continue;

        float lit = 0.0;
        if (properties.w < 0.0) {
            bool activeArea = limitOverlap ? (areaMask & (1u << uint(index))) != 0u
                    : dot(normal, toLight) > -0.87;
            if (activeArea) {
                int grid = jemShadowSamples;
                if (areaCount > 1 && index != dominant) {
                    grid = jemShadowSamples == 2 ? (areaCount <= 2 ? 2 : 1)
                            : min(jemShadowSamples, jemSecondaryAreaGrid(areaCount - 1, secondary++));
                }
                lit = jemBlockFaceLight(preparedRay, cameraRayOrigin, normal, lightPosition, grid, casterMask);
            }
        } else {
            vec3 shadowRay = lightPosition - rayOrigin;
            float reach = length(shadowRay);
            vec3 direction = shadowRay / max(reach, 0.00001);
            float facing = max(dot(normal, direction), 0.0);
            if (facing > 0.0 && reach > 0.0001) {
                lit = facing * jemPointTransmission(preparedRay, cameraRayOrigin, direction, reach, sourceCell,
                        casterMask);
            }
        }
        contributedLight += properties.rgb * (lit * attenuation);
    }

    // Minecraft packs the brightest nearby block source into its vertex light level.
    // Preserve a diffuse share and the receiving block's own emission.
    vec3 nativeLinear = jemLinearColor(nativeColor);
    vec3 skyLinear = min(jemLinearColor(skyBaseColor), nativeLinear);
    float receiverEmission = modeledBlockLevel > 0.0
            ? float((jemCellData(receiverCell) >> 10) & 15u) / 15.0 : 0.0;
    float externalLevel = max(0.0, lightLevels.x - receiverEmission);
    float replacedShare = 0.65 * min(modeledBlockLevel, externalLevel) / max(lightLevels.x, 1.0 / 15.0);
    vec3 residualBlockLight = (nativeLinear - skyLinear) * (1.0 - replacedShare);

    // Apply directional occlusion only to the skylight component.
    vec3 skyTint = mix(vec3(1.0), jemSunColor, jemSunDirectionStrength.w * 0.56);
    vec3 litSky = skyLinear * skyTint * (1.0 - 0.55 * sunlightOcclusion) * (1.0 - cloudOcclusion);
    vec3 combined = litSky + residualBlockLight + jemLinearColor(albedo) * contributedLight;
    return pow(max(combined, vec3(0.0)), vec3(1.0 / 2.2));
}
