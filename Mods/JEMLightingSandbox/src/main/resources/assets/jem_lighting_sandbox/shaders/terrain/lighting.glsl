uniform int jemActive;
uniform sampler3D jemOpacity;
uniform vec3 jemVolumeOrigin;
uniform ivec3 jemVolumeSize;
uniform int jemLightCount;
uniform vec4 jemLightPositionRange[8];
uniform vec4 jemLightColorStrength[8];
uniform vec4 jemSunDirectionStrength;
uniform vec3 jemSunColor;

const int JEM_MAX_LIGHTS = 8;
const int JEM_MAX_RAY_STEPS = 48;
const float JEM_SURFACE_BIAS = 0.035;
const float JEM_SUN_SHADOW_REACH = 24.0;

bool jemInsideVolume(ivec3 cell) {
    return all(greaterThanEqual(cell, ivec3(0))) && all(lessThan(cell, jemVolumeSize));
}

// Intersections at a shared edge advance every tied axis, skipping zero-length voxel spans.
float jemVisibility(vec3 origin, vec3 direction, float reach, ivec3 lightCell) {
    ivec3 cell = ivec3(floor(origin));
    ivec3 cellStep = ivec3(sign(direction));
    vec3 distancePerCell = 1.0 / max(abs(direction), vec3(0.000001));
    vec3 cellEdge = vec3(cell) + max(vec3(cellStep), vec3(0.0));
    vec3 nextCrossing = (cellEdge - origin) * vec3(cellStep) * distancePerCell;
    nextCrossing += (1.0 - step(vec3(0.000001), abs(direction))) * 1.0e6;

    for (int stepIndex = 0; stepIndex < JEM_MAX_RAY_STEPS; ++stepIndex) {
        float distance = min(nextCrossing.x, min(nextCrossing.y, nextCrossing.z));
        if (distance >= reach) {
            break;
        }
        bvec3 crossed = lessThanEqual(nextCrossing, vec3(distance + 0.00001));
        cell += ivec3(crossed) * cellStep;
        nextCrossing += vec3(crossed) * distancePerCell;

        if (!jemInsideVolume(cell) || all(equal(cell, lightCell))) {
            break;
        }
        if (texelFetch(jemOpacity, cell, 0).r > 0.5) {
            return 0.0;
        }
    }
    return 1.0;
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

vec3 jemLinearColor(vec3 color) {
    return pow(max(color, vec3(0.0)), vec3(2.2));
}

vec3 jemApplyLighting(vec3 nativeColor, vec3 skyBaseColor, vec3 albedo, vec3 position, vec2 lightLevels) {
    if (jemActive == 0) {
        return nativeColor;
    }

    vec3 normalVector = cross(dFdx(position), dFdy(position));
    float normalSquared = dot(normalVector, normalVector);
    if (normalSquared < 1.0e-12) {
        return nativeColor;
    }
    vec3 normal = normalVector * inversesqrt(normalSquared);
    if (dot(normal, -position) < 0.0) {
        normal = -normal;
    }
    vec3 localPosition = position - jemVolumeOrigin;
    vec3 edgeDistance = min(localPosition, vec3(jemVolumeSize) - localPosition);
    float sceneWeight = smoothstep(1.0, 10.0, min(edgeDistance.x, min(edgeDistance.y, edgeDistance.z)));
    if (sceneWeight <= 0.0) {
        return nativeColor;
    }

    vec3 rayOrigin = localPosition + normal * JEM_SURFACE_BIAS;
    ivec3 receiverCell = ivec3(floor(localPosition - normal * JEM_SURFACE_BIAS));
    vec3 contributedLight = vec3(0.0);
    float modeledBlockLevel = 0.0;
    for (int index = 0; index < JEM_MAX_LIGHTS; ++index) {
        if (index >= jemLightCount) {
            break;
        }
        vec4 source = jemLightPositionRange[index];
        vec4 properties = jemLightColorStrength[index];
        vec3 toLight = source.xyz - position;
        float distanceSquared = dot(toLight, toLight);
        if (distanceSquared >= source.w * source.w || distanceSquared < 1.0e-6) {
            continue;
        }

        vec3 sourceCellPosition = source.xyz - jemVolumeOrigin;
        ivec3 sourceCell = ivec3(floor(sourceCellPosition));
        if (all(equal(receiverCell, sourceCell))) {
            continue;
        }

        float distance = sqrt(distanceSquared);
        vec3 absoluteDelta = abs(sourceCellPosition - rayOrigin);
        float taxicabDistance = absoluteDelta.x + absoluteDelta.y + absoluteDelta.z;
        float distanceFade = smoothstep(0.0, 2.0, source.w - distance);
        float lightLevel = max(0.0, properties.w - max(0.0, taxicabDistance - 1.0) / 15.0);
        modeledBlockLevel = max(modeledBlockLevel, distanceFade * lightLevel);

        vec3 shadowRay = sourceCellPosition - rayOrigin;
        float shadowRayLength = length(shadowRay);
        if (shadowRayLength < 0.0001) {
            continue;
        }
        vec3 direction = shadowRay / shadowRayLength;
        float incidence = max(dot(normal, direction), 0.0);
        if (incidence <= 0.0) {
            continue;
        }
        float visibility = jemVisibility(rayOrigin, direction, shadowRayLength, sourceCell);
        float attenuation = distanceFade * 1.35 * properties.w / (1.0 + 0.08 * distanceSquared);
        contributedLight += properties.rgb * (visibility * incidence * attenuation);
    }

    // Minecraft packs the brightest nearby block source into its vertex light level.
    // Replace only the estimated share of that value carried by selected sources.
    vec3 nativeLinear = jemLinearColor(nativeColor);
    vec3 skyLinear = min(jemLinearColor(skyBaseColor), nativeLinear);
    float replacedShare = clamp(modeledBlockLevel / max(lightLevels.x, 1.0 / 15.0), 0.0, 1.0);
    vec3 residualBlockLight = (nativeLinear - skyLinear) * (1.0 - replacedShare);

    float sunlightOcclusion = jemSunlight(rayOrigin, normal, lightLevels.y);
    // The vanilla sky term already contains daylight. Darken its directional share
    // while keeping an ambient floor, instead of adding another full sun light.
    vec3 litSky = skyLinear * (vec3(1.0) - 0.55 * jemSunColor * sunlightOcclusion);
    vec3 combined = litSky + residualBlockLight + jemLinearColor(albedo) * contributedLight;
    vec3 outputColor = pow(max(combined, vec3(0.0)), vec3(1.0 / 2.2));
    return mix(nativeColor, outputColor, sceneWeight);
}
