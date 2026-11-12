#version 330 core

uniform sampler2D BlockAtlas;
uniform vec4 ActiveFamilies;
uniform vec3 EmissionSelectors;
in vec2 texCoord;
in vec3 normal;
in vec4 color;
flat in ivec2 material;
flat in int alphaCutoff;
flat in int optics;
layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 fragOptics;

vec2 octNormal(vec3 n) {
    n /= max(abs(n.x) + abs(n.y) + abs(n.z), 0.00001);
    vec2 direction = n.xy;
    if (n.z < 0.0) direction = (1.0 - abs(direction.yx)) * mix(vec2(-1.0), vec2(1.0), step(vec2(0.0), direction));
    return direction;
}

void main() {
    int family = material.x;
    float enabledFamily = family == 1 ? ActiveFamilies.x
                 : family == 3 ? ActiveFamilies.z
                 : family >= 5 ? ActiveFamilies.w : ActiveFamilies.y;
    if (family == 6) enabledFamily *= EmissionSelectors.x;
    if (family == 7) enabledFamily *= EmissionSelectors.y;
    if (family == 8) enabledFamily *= EmissionSelectors.z;
    if (family < 1 || enabledFamily < 0.5) discard;
    vec4 texel = texture(BlockAtlas, texCoord) * color;
    if (texel.a < float(alphaCutoff) / 65536.0) discard;
    ivec3 rgb = ivec3(round(clamp(texel.rgb, 0.0, 1.0) * 255.0));
    int tint = (rgb.r << 16) | (rgb.g << 8) | rgb.b;
    int flags = ((material.y & 65535) << 4) | family;
    fragColor = vec4(octNormal(normal), float(flags), float(tint));
    float ior = 1.0 + 2.0 * float(optics & 255) / 255.0;
    float opacity = float((optics >> 8) & 255) / 255.0;
    fragOptics = vec4(ior, opacity, 0.0, 1.0);
}
