vec3 jemCutoutOffset(uint voxel) {
    int packedOffset = int((voxel >> 15) & 32767u);
    if ((voxel & (1u << 14)) == 0u) {
        return vec3(0.0);
    }
    ivec3 steps = (ivec3(packedOffset) >> ivec3(0, 6, 9)) & ivec3(63, 7, 63);
    return vec3(steps) / vec3(126.0, 35.0, 126.0) - vec3(0.25, 0.2, 0.25);
}

float jemCutoutHit(sampler2D palette, int shape, int faceCount,
        vec3 origin, vec3 ray, float entering, float leaving) {
    for (int face = 0; face < faceCount; ++face) {
        int column = face * 7;
        vec3 a = texelFetch(palette, ivec2(column, shape), 0).xyz;
        vec3 ab = texelFetch(palette, ivec2(column + 1, shape), 0).xyz;
        vec3 ad = texelFetch(palette, ivec2(column + 2, shape), 0).xyz;
        vec3 normal = cross(ab, ad);
        float denominator = dot(normal, ray);
        if (abs(denominator) < 0.000001) {
            continue;
        }
        float hit = dot(normal, a - origin) / denominator;
        if (hit < max(entering, 0.0001) || hit >= leaving) {
            continue;
        }
        vec3 local = origin + ray * hit - a;
        float planeAreaSquared = dot(normal, normal);
        vec2 st = vec2(dot(cross(local, ad), normal), dot(cross(ab, local), normal)) / planeAreaSquared;
        if (any(lessThan(st, vec2(-0.00001))) || any(greaterThan(st, vec2(1.00001)))) {
            continue;
        }
        ivec2 pixel = ivec2(clamp(st * 16.0, vec2(0.0), vec2(15.0)));
        vec4 masks = texelFetch(palette, ivec2(column + 3 + pixel.y / 4, shape), 0);
        if ((int(masks[pixel.y % 4]) & (1 << pixel.x)) != 0) {
            leaving = hit;
        }
    }
    return leaving;
}
