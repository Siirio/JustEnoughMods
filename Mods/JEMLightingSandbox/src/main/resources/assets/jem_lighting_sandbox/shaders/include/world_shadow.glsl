uniform isampler2D jemSections;
uniform usamplerBuffer jemVoxels;
uniform sampler2D jemShapes;
uniform ivec2 jemWorldSections;
uniform ivec3 jemCameraBlock;
uniform vec3 jemCameraFraction;
uniform float jemShadowTime;

const int JEM_SECTION_RING = 69;
const int JEM_PAGE_WORDS = 4099;
const uint JEM_SHAPE_MASK = 1023u;
const uint JEM_BANNER_OVERHANG = 1u << 30;
const uint JEM_CUTOUT_OVERHANG = 1u << 31;
const float JEM_SURFACE_BIAS = 0.003;
const float JEM_RAY_EPSILON = 0.0001;
const float JEM_SUN_SHADOW_REACH = 24.0;
const ivec3 JEM_NO_SOURCE = ivec3(2147483647);

vec3 jemInverseRay(vec3 direction) {
    vec3 signOfRay = 2.0 * vec3(greaterThanEqual(direction, vec3(0.0))) - 1.0;
    return signOfRay / max(abs(direction), vec3(0.000001));
}

ivec4 jemSectionAt(ivec3 relativeSection) {
    ivec3 section = (jemCameraBlock >> 4) + relativeSection;
    int y = section.y - jemWorldSections.x;
    if (y < 0 || y >= jemWorldSections.y) {
        return ivec4(0, 0, -1, 0);
    }
    ivec2 slot = ((section.xz % JEM_SECTION_RING) + JEM_SECTION_RING) % JEM_SECTION_RING;
    ivec4 entry = texelFetch(jemSections, ivec2(slot.x, slot.y * jemWorldSections.y + y), 0);
    return all(equal(entry.xy, section.xz)) ? entry : ivec4(0);
}

uint jemCellData(ivec4 section, ivec3 cell) {
    if (section.z < 0) return uint(section.w);
    if (section.z == 0) return 0u;
    ivec3 local = cell & 15;
    int index = local.x + (local.z << 4) + (local.y << 8);
    return texelFetch(jemVoxels, (section.z - 1) * JEM_PAGE_WORDS + index).r;
}

uint jemCellData(ivec3 cell) {
    return jemCellData(jemSectionAt(cell >> 4), cell);
}

vec2 jemBoxInterval(vec3 low, vec3 high, vec3 origin, vec3 inverseRay) {
    vec3 a = (low - origin) * inverseRay;
    vec3 b = (high - origin) * inverseRay;
    vec3 nearPlane = min(a, b);
    vec3 farPlane = max(a, b);
    return vec2(max(nearPlane.x, max(nearPlane.y, nearPlane.z)),
            min(farPlane.x, min(farPlane.y, farPlane.z)));
}

float jemBoxHit(vec3 low, vec3 high, vec3 origin, vec3 inverseRay, float entering, float leaving) {
    vec2 bounds = jemBoxInterval(low, high, origin, inverseRay);
    float first = max(bounds.x, entering);
    return min(bounds.y, leaving) - first > JEM_RAY_EPSILON ? first : leaving;
}

bool jemIsBanner(int shape) {
    return shape >= 3 && shape < 23;
}

vec3 jemBannerLocal(vec3 vector, vec2 yaw) {
    return vec3(yaw.x * vector.x + yaw.y * vector.z, -vector.y,
            yaw.y * vector.x - yaw.x * vector.z) * 1.5;
}

vec3 jemUnrotateCloth(vec3 vector, vec2 pitch) {
    return vec3(vector.x, pitch.x * vector.y + pitch.y * vector.z,
            pitch.x * vector.z - pitch.y * vector.y);
}

float jemBannerHit(int shape, ivec3 cell, vec3 origin, vec3 ray, float entering, float leaving) {
    vec3 anchor = texelFetch(jemShapes, ivec2(6, shape), 0).xyz;
    vec2 yaw = texelFetch(jemShapes, ivec2(7, shape), 0).xy;
    vec3 local = jemBannerLocal(origin - vec3(cell) - anchor, yaw);
    vec3 direction = jemBannerLocal(ray, yaw);
    vec3 inverseRay = jemInverseRay(direction);
    vec3 low = texelFetch(jemShapes, ivec2(0, shape), 0).xyz;
    vec3 high = texelFetch(jemShapes, ivec2(1, shape), 0).xyz;
    float hit = jemBoxHit(low, high, local, inverseRay, entering, leaving);
    if (shape >= 7) {
        low = texelFetch(jemShapes, ivec2(4, shape), 0).xyz;
        high = texelFetch(jemShapes, ivec2(5, shape), 0).xyz;
        hit = jemBoxHit(low, high, local, inverseRay, entering, hit);
    }
    ivec3 world = cell + (jemCameraBlock & ~15);
    int phase = ((world.x % 100) * 7 + (world.y % 100) * 9 + (world.z % 100) * 13) % 100;
    float sway = 0.01 * cos((float(phase) + jemShadowTime) * 0.062831853);
    float angle = (-0.0125 + sway) * 3.14159265;
    vec2 pitch = vec2(cos(angle), sin(angle));
    vec3 clothOrigin = jemUnrotateCloth(local + vec3(0.0, 2.0, 0.0), pitch);
    vec3 clothDirection = jemInverseRay(jemUnrotateCloth(direction, pitch));
    low = texelFetch(jemShapes, ivec2(2, shape), 0).xyz;
    high = texelFetch(jemShapes, ivec2(3, shape), 0).xyz;
    return jemBoxHit(low, high, clothOrigin, clothDirection, entering, hit);
}

void jemSkipEmptyCube(vec3 origin, vec3 direction, vec3 inverseRay, ivec3 stepDirection,
        vec3 positive, int shift, inout ivec3 cell, inout vec3 edges, out float entering) {
    ivec3 boundaryCell = ((cell >> shift) + ivec3(positive)) << shift;
    vec3 boundary = (vec3(boundaryCell) - origin) * inverseRay;
    entering = min(boundary.x, min(boundary.y, boundary.z));
    bvec3 crossed = lessThanEqual(boundary, vec3(entering + JEM_RAY_EPSILON));
    vec3 position = origin + direction * entering;
    vec3 next = floor(position + direction * JEM_RAY_EPSILON);
    cell = ivec3(mix(next, vec3(boundaryCell) + min(vec3(stepDirection), vec3(0.0)), crossed));
    edges = (vec3(cell) + positive - origin) * inverseRay;
}

struct JemRayOrigin {
    vec3 position;
    ivec3 cell;
    ivec4 section;
    uint voxel;
};

JemRayOrigin jemPrepareRayOrigin(vec3 position) {
    ivec3 cell = ivec3(floor(position));
    ivec4 section = jemSectionAt(cell >> 4);
    return JemRayOrigin(position, cell, section, jemCellData(section, cell));
}

// Point and area samples for a fragment share their first section and voxel.
float jemVisibility(JemRayOrigin start, vec3 direction, float reach, ivec3 ignoredCell) {
    vec3 origin = start.position;
    ivec3 cell = start.cell;
    vec3 inverseRay = jemInverseRay(direction);
    ivec3 stepDirection = ivec3(sign(inverseRay));
    vec3 steps = abs(inverseRay);
    vec3 positive = max(vec3(stepDirection), vec3(0.0));
    vec3 edges = (vec3(cell) + positive - origin) * inverseRay;

    float entering = 0.0;
    float visibility = 1.0;
    ivec3 sectionPos = cell >> 4;
    ivec4 section = start.section;
    ivec3 lastSubcell = ivec3(2147483647);
    int lastWord = -1;
    uint occupancy = 0u;
    // Every caller uses a normalized ray no longer than 24 blocks.
    for (int index = 0; index < 48 && entering < reach; index++) {
        if (any(notEqual(sectionPos, cell >> 4))) {
            sectionPos = cell >> 4;
            section = jemSectionAt(sectionPos);
            lastWord = -1;
        }
        uint uniformVoxel = uint(section.w);
        if (section.z < 0 && section.w == 1 && !all(equal(cell, ignoredCell))) {
            return 0.0;
        }
        bool aboveOccluders = section.z > 0
                && ((section.w >> 1) == 0 || (direction.y >= 0.0 && (cell.y & 15) >= (section.w >> 1)));
        if (section.z == 0 || aboveOccluders
                || (section.z < 0 && (uniformVoxel & (JEM_SHAPE_MASK | JEM_BANNER_OVERHANG | JEM_CUTOUT_OVERHANG)) == 0u)) {
            jemSkipEmptyCube(origin, direction, inverseRay, stepDirection, positive, 4, cell, edges, entering);
            continue;
        }
        if (section.z > 0 && (section.w & 1) == 0) {
            ivec3 subcell = cell >> 2;
            if (any(notEqual(subcell, lastSubcell))) {
                lastSubcell = subcell;
                ivec3 local = subcell & 3;
                int bit = local.x | (local.z << 2) | (local.y << 4);
                int word = bit >> 5;
                if (word != lastWord) {
                    lastWord = word;
                    occupancy = texelFetch(jemVoxels, (section.z - 1) * JEM_PAGE_WORDS + 4096 + word).r;
                }
                if ((occupancy & (1u << uint(bit & 31))) == 0u) {
                    jemSkipEmptyCube(origin, direction, inverseRay, stepDirection, positive, 2, cell, edges, entering);
                    continue;
                }
            }
        }
        float leaving = min(edges.x, min(edges.y, edges.z));
        float end = min(reach, leaving);
        if (end - entering > JEM_RAY_EPSILON) {
            bool ignored = all(equal(cell, ignoredCell));
            uint voxel = index == 0 ? start.voxel : jemCellData(section, cell);
            int shape = int(voxel & JEM_SHAPE_MASK);
            float hit = end;
            if (!ignored) {
                if (jemIsBanner(shape)) {
                    hit = jemBannerHit(shape, cell, origin, direction, entering, hit);
                } else if (shape == 1) {
                    return 0.0;
                } else if (shape == 2) {
                    visibility *= exp(-1.25 * (end - entering));
                } else if (shape >= 23) {
                    vec2 cutout = texelFetch(jemShapes, ivec2(63, shape), 0).xy;
                    vec3 local = origin - vec3(cell);
                    if (cutout.x > 0.0) {
                        float cutoutHit = jemCutoutHit(jemShapes, shape, int(cutout.x),
                                local - jemCutoutOffset(voxel), direction, entering, hit);
                        if (cutoutHit < hit) {
                            if (cutout.y >= 1.0) {
                                hit = cutoutHit;
                            } else {
                                visibility *= 1.0 - cutout.y;
                            }
                        }
                    } else {
                        for (int box = 0; box < 8; box++) {
                            vec4 low = texelFetch(jemShapes, ivec2(box * 2, shape), 0);
                            if (low.w < 0.5) break;
                            vec3 high = texelFetch(jemShapes, ivec2(box * 2 + 1, shape), 0).xyz;
                            hit = jemBoxHit(low.xyz, high, local, inverseRay, entering, hit);
                        }
                    }
                }
            }
            if ((voxel & JEM_BANNER_OVERHANG) != 0u) {
                for (int offset = -1; offset <= 1; offset += 2) {
                    ivec3 owner = cell + ivec3(0, offset, 0);
                    uint neighbor = all(equal(owner >> 4, sectionPos)) ? jemCellData(section, owner) : jemCellData(owner);
                    int banner = int(neighbor & JEM_SHAPE_MASK);
                    if (jemIsBanner(banner) && (offset > 0 || banner >= 7)) {
                        hit = jemBannerHit(banner, owner, origin, direction, entering, hit);
                    }
                }
            }
            if ((voxel & JEM_CUTOUT_OVERHANG) != 0u) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        for (int x = -1; x <= 1; x++) {
                            if ((x | y | z) == 0) continue;
                            ivec3 owner = cell + ivec3(x, y, z);
                            uint other = all(equal(owner >> 4, sectionPos)) ? jemCellData(section, owner) : jemCellData(owner);
                            if ((other & (1u << 14)) == 0u) continue;
                            int faceShape = int(other & JEM_SHAPE_MASK);
                            vec2 cutout = texelFetch(jemShapes, ivec2(63, faceShape), 0).xy;
                            float cutoutHit = jemCutoutHit(jemShapes, faceShape, int(cutout.x),
                                    origin - vec3(owner) - jemCutoutOffset(other), direction, entering, hit);
                            if (cutoutHit < hit) {
                                if (cutout.y >= 1.0) {
                                    hit = cutoutHit;
                                } else {
                                    visibility *= 1.0 - cutout.y;
                                }
                            }
                        }
                    }
                }
            }
            if (hit < end || visibility <= 0.01) return 0.0;
        }
        bvec3 crossed = lessThanEqual(edges, vec3(leaving + 0.00001));
        cell += ivec3(crossed) * stepDirection;
        edges += vec3(crossed) * steps;
        entering = leaving;
    }
    return visibility;
}
