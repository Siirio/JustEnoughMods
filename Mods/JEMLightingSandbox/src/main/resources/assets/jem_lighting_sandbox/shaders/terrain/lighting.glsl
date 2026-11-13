uniform int jemActive;
uniform sampler3D jemOpacity;
uniform sampler2D jemShapes;
uniform sampler2D jemChunkLights;
uniform vec3 jemVolumeOrigin;
uniform ivec3 jemVolumeSize;
uniform ivec3 jemCameraBlock;
uniform vec3 jemCameraFraction;
uniform ivec4 jemChunkLayout;
uniform int jemLightCount;
uniform vec4 jemLightPositionRange[8];
uniform vec4 jemLightColorStrength[8];
uniform vec4 jemSunDirectionStrength;
uniform vec3 jemSunColor;

const int JEM_MAX_LIGHTS = 8;
const int JEM_MAX_RAY_STEPS = 48;
const float JEM_SURFACE_BIAS = 0.003;
const float JEM_RAY_EPSILON = 0.0001;
const float JEM_SUN_SHADOW_REACH = 24.0;

bool jemInsideVolume(ivec3 cell) {
    return all(greaterThanEqual(cell, ivec3(0))) && all(lessThan(cell, jemVolumeSize));
}

ivec2 jemVoxelData(ivec3 cell) {
    return ivec2(round(texelFetch(jemOpacity, cell, 0).rg * 255.0));
}

bool jemHitsBox(vec3 low, vec3 high, vec3 origin, vec3 inverseRay, float entering, float leaving) {
    vec3 lowDistance = (low - origin) * inverseRay;
    vec3 highDistance = (high - origin) * inverseRay;
    vec3 nearPlane = min(lowDistance, highDistance);
    vec3 farPlane = max(lowDistance, highDistance);
    float nearHit = max(entering, max(nearPlane.x, max(nearPlane.y, nearPlane.z)));
    float farHit = min(leaving, min(farPlane.x, min(farPlane.y, farPlane.z)));
    return farHit - nearHit > JEM_RAY_EPSILON;
}

float jemVisibility(vec3 origin, vec3 direction, float reach, ivec3 lightCell) {
    ivec3 cell = ivec3(floor(origin));
    vec3 raySign = 2.0 * vec3(greaterThanEqual(direction, vec3(0.0))) - 1.0;
    ivec3 cellStep = ivec3(raySign);
    vec3 distancePerCell = 1.0 / max(abs(direction), vec3(0.000001));
    vec3 inverseRay = raySign * distancePerCell;
    vec3 cellEdge = vec3(cell) + max(vec3(cellStep), vec3(0.0));
    vec3 nextCrossing = (cellEdge - origin) * inverseRay;

    float entering = 0.0;
    float visibility = 1.0;
    for (int stepIndex = 0; stepIndex < JEM_MAX_RAY_STEPS; ++stepIndex) {
        if (!jemInsideVolume(cell) || all(equal(cell, lightCell))) {
            break;
        }
        float leaving = min(nextCrossing.x, min(nextCrossing.y, nextCrossing.z));
        int shape = jemVoxelData(cell).y;
        float end = min(leaving, reach);
        if (end - entering > JEM_RAY_EPSILON) {
            if (shape == 1) {
                return 0.0;
            }
            if (shape == 13) {
                visibility *= exp(-0.32 * (end - entering));
                if (visibility < 0.02) {
                    return 0.02;
                }
            } else if (shape >= 14) {
                for (int box = 0; box < 8; ++box) {
                    vec4 low = texelFetch(jemShapes, ivec2(box * 2, shape), 0);
                    if (low.w < 0.5) {
                        break;
                    }
                    vec3 high = texelFetch(jemShapes, ivec2(box * 2 + 1, shape), 0).xyz;
                    if (jemHitsBox(low.xyz, high, origin - vec3(cell), inverseRay, entering, end)) {
                        return 0.0;
                    }
                }
            }
        }
        if (leaving >= reach) {
            break;
        }
        bvec3 crossed = lessThanEqual(nextCrossing, vec3(leaving + 0.00001));
        cell += ivec3(crossed) * cellStep;
        nextCrossing += vec3(crossed) * distancePerCell;
        entering = leaving;
    }
    return visibility;
}

ivec2 jemChunkLightRow(vec3 position, out vec3 sectionOrigin) {
    ivec3 section = (jemCameraBlock + ivec3(floor(position + jemCameraFraction))) >> 4;
    ivec2 offset = abs(section.xz - (jemCameraBlock.xz >> 4));
    int height = section.y - jemChunkLayout.y;
    if (max(offset.x, offset.y) > jemChunkLayout.z || height < 0 || height >= jemChunkLayout.x) {
        return ivec2(-1);
    }
    ivec2 slot = ((section.xz % 65) + 65) % 65;
    ivec2 row = ivec2(slot.x * 17, slot.y * jemChunkLayout.x + height);
    vec4 header = texelFetch(jemChunkLights, row, 0);
    if (header.w < 0.5 || any(notEqual(ivec2(header.xy), section.xz))) {
        return ivec2(-1);
    }
    sectionOrigin = vec3((section << 4) - jemCameraBlock) - jemCameraFraction;
    return row;
}

float jemSunlight(vec3 rayOrigin, vec3 normal, float skyLevel) {
    float strength = jemSunDirectionStrength.w * smoothstep(0.45, 1.0, skyLevel);
    if (strength <= 0.0) {
        return 0.0;
    }
    vec3 direction = jemSunDirectionStrength.xyz;
    float incidence = max(dot(normal, direction), 0.0);
    float visibility = incidence > 0.0
            ? jemVisibility(rayOrigin, direction, JEM_SUN_SHADOW_REACH, ivec3(-1)) : 0.0;
    return strength * (1.0 - incidence * visibility);
}

vec3 jemExposedEmitterFace(ivec3 sourceCell, vec3 receiver) {
    vec3 towardReceiver = receiver - (vec3(sourceCell) + 0.5);
    float bestDistance = 0.499;
    ivec3 face = ivec3(0);
    for (int axis = 0; axis < 3; ++axis) {
        float distance = abs(towardReceiver[axis]);
        if (distance <= bestDistance) {
            continue;
        }
        ivec3 candidate = ivec3(0);
        candidate[axis] = towardReceiver[axis] < 0.0 ? -1 : 1;
        ivec3 neighbor = sourceCell + candidate;
        if (jemInsideVolume(neighbor) && jemVoxelData(neighbor).y != 1) {
            bestDistance = distance;
            face = candidate;
        }
    }
    return vec3(face);
}

vec3 jemLinearColor(vec3 color) {
    return pow(max(color, vec3(0.0)), vec3(2.2));
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
    vec3 localPosition = position - jemVolumeOrigin;
    vec3 edgeDistance = min(localPosition, vec3(jemVolumeSize) - localPosition);
    float sceneWeight = smoothstep(1.0, 10.0, min(edgeDistance.x, min(edgeDistance.y, edgeDistance.z)));
    vec3 sectionOrigin = vec3(0.0);
    ivec2 lightRow = jemChunkLightRow(position, sectionOrigin);
    int indexedCount = lightRow.x < 0 ? 0 : int(texelFetch(jemChunkLights, lightRow, 0).z);
    bool indexed = indexedCount > 0;
    if (!indexed && sceneWeight <= 0.0) {
        return nativeColor;
    }

    vec3 rayOrigin = localPosition + normal * JEM_SURFACE_BIAS;
    ivec3 receiverCell = ivec3(floor(localPosition - normal * JEM_SURFACE_BIAS));
    float receiverEmission = jemInsideVolume(receiverCell) ? float(jemVoxelData(receiverCell).x) / 15.0 : 0.0;
    vec3 contributedLight = vec3(0.0);
    float modeledBlockLevel = 0.0;
    for (int index = 0; index < JEM_MAX_LIGHTS; ++index) {
        if (index >= (indexed ? indexedCount : jemLightCount)) {
            break;
        }
        vec4 source = indexed ? texelFetch(jemChunkLights, lightRow + ivec2(1 + index * 2, 0), 0)
                : jemLightPositionRange[index];
        vec4 properties = indexed ? texelFetch(jemChunkLights, lightRow + ivec2(2 + index * 2, 0), 0)
                : jemLightColorStrength[index];
        if (indexed) source.xyz += sectionOrigin;
        vec3 toLight = source.xyz - position;
        float distanceSquared = dot(toLight, toLight);
        if (distanceSquared >= source.w * source.w || distanceSquared < 1.0e-6) {
            continue;
        }

        vec3 lightPosition = source.xyz - jemVolumeOrigin;
        ivec3 sourceCell = ivec3(floor(lightPosition));
        if (all(equal(receiverCell, sourceCell))) {
            continue;
        }

        float distance = sqrt(distanceSquared);
        vec3 absoluteDelta = abs(vec3(sourceCell) + 0.5 - rayOrigin);
        float taxicabDistance = absoluteDelta.x + absoluteDelta.y + absoluteDelta.z;
        float distanceFade = smoothstep(0.0, 2.0, source.w - distance);
        float lightLevel = max(0.0, properties.w - max(0.0, taxicabDistance - 1.0) / 15.0);
        float modeledSourceLevel = distanceFade * lightLevel;
        modeledBlockLevel = max(modeledBlockLevel, 0.35 * modeledSourceLevel);

        bool solidSource = jemInsideVolume(sourceCell) && jemVoxelData(sourceCell).y == 1;
        if (solidSource) {
            vec3 emitterFace = jemExposedEmitterFace(sourceCell, rayOrigin);
            if (all(equal(emitterFace, vec3(0.0)))) {
                continue;
            }
            // Move the target outside the emitting face so flush fixtures can light their own room.
            lightPosition = vec3(sourceCell) + 0.5 + 0.65 * emitterFace;
        }

        vec3 shadowRay = lightPosition - rayOrigin;
        float shadowRayLength = length(shadowRay);
        if (shadowRayLength < 0.0001) {
            continue;
        }
        vec3 direction = shadowRay / shadowRayLength;
        float incidence = max(dot(normal, direction), 0.0);
        if (incidence <= 0.0) {
            continue;
        }
        // Trace all intervening geometry for solid emitters, including the source's own other faces.
        float nativeVisibility = smoothstep(0.0, max(0.15, lightLevel * 0.75), lightLevels.x);
        float tracedVisibility = sceneWeight > 0.0 && jemInsideVolume(sourceCell)
                ? jemVisibility(rayOrigin, direction, shadowRayLength, solidSource ? ivec3(-1) : sourceCell)
                : nativeVisibility;
        float visibility = mix(nativeVisibility, tracedVisibility, sceneWeight);
        modeledBlockLevel = max(modeledBlockLevel, mix(0.35, 1.0, visibility) * modeledSourceLevel);
        if (solidSource) {
            incidence = mix(incidence, sqrt(incidence), 0.55);
        }
        float attenuation = distanceFade * 1.35 * properties.w / (1.0 + 0.08 * distanceSquared);
        contributedLight += properties.rgb * (visibility * incidence * attenuation);
    }

    // Minecraft packs the brightest nearby block source into its vertex light level.
    // Preserve a diffuse share and the receiving block's own emission.
    vec3 nativeLinear = jemLinearColor(nativeColor);
    vec3 skyLinear = min(jemLinearColor(skyBaseColor), nativeLinear);
    float externalLevel = max(0.0, lightLevels.x - receiverEmission);
    float replacedShare = 0.65 * min(modeledBlockLevel, externalLevel) / max(lightLevels.x, 1.0 / 15.0);
    vec3 residualBlockLight = (nativeLinear - skyLinear) * (1.0 - replacedShare);

    float sunlightOcclusion = sceneWeight > 0.0 ? jemSunlight(rayOrigin, normal, lightLevels.y) : 0.0;
    // The vanilla sky term already contains daylight. Darken its directional share
    // while keeping an ambient floor, instead of adding another full sun light.
    vec3 litSky = skyLinear * (vec3(1.0) - 0.55 * jemSunColor * sunlightOcclusion * sceneWeight);
    vec3 combined = litSky + residualBlockLight + jemLinearColor(albedo) * contributedLight;
    vec3 outputColor = pow(max(combined, vec3(0.0)), vec3(1.0 / 2.2));
    return mix(nativeColor, outputColor, indexed ? 1.0 : sceneWeight);
}
