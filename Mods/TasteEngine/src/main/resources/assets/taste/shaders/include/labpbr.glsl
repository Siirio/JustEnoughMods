#ifndef TASTE_LABPBR_GLSL
#define TASTE_LABPBR_GLSL
// Original LabPBR 1.3 decoder. Specification: https://shaderlabs.org/wiki/LabPBR_Material_Standard
// _s/_n are LINEAR data textures. Fetch _s discrete byte channels without bilinear/mipmap ID blending.
struct TasteMaterial {
    float linearRoughness;
    float metallic;
    vec3 f0;
    vec3 reflectionTint;
    float porosity;
    float subsurface;
    float emission;
};
int tasteByte(float v) { return int(floor(clamp(v, 0.0, 1.0) * 255.0 + 0.5)); }
vec3 tasteConductorF0(vec3 n, vec3 k) {
    return ((n - 1.0) * (n - 1.0) + k * k) / ((n + 1.0) * (n + 1.0) + k * k);
}
// Physical constants from LabPBR spec; Fresnel remains a Schlick approximation in this seed.
vec3 tasteMetalF0(int code) {
    if (code == 230) return tasteConductorF0(vec3(2.9114,2.9497,2.5845), vec3(3.0893,2.9318,2.7670));
    if (code == 231) return tasteConductorF0(vec3(0.18299,0.42108,1.3734), vec3(3.4242,2.3459,1.7704));
    if (code == 232) return tasteConductorF0(vec3(1.3456,0.96521,0.61722), vec3(7.4746,6.3995,5.3031));
    if (code == 233) return tasteConductorF0(vec3(3.1071,3.1812,2.3230), vec3(3.3314,3.3291,3.1350));
    if (code == 234) return tasteConductorF0(vec3(0.27105,0.67693,1.3164), vec3(3.6092,2.6248,2.2921));
    if (code == 235) return tasteConductorF0(vec3(1.91,1.83,1.44), vec3(3.51,3.4,3.18));
    if (code == 236) return tasteConductorF0(vec3(2.3757,2.0847,1.8453), vec3(4.2655,3.7153,3.1365));
    return tasteConductorF0(vec3(0.15943,0.14512,0.13547), vec3(3.9291,3.1900,2.3808)); // 237 silver
}
TasteMaterial tasteDecodeLabPbr(vec4 specular, vec3 linearAlbedo) {
    TasteMaterial m;
    int g = tasteByte(specular.g), b = tasteByte(specular.b), a = tasteByte(specular.a);
    m.linearRoughness = pow(1.0 - clamp(specular.r, 0.0, 1.0), 2.0);
    // G-buffer Z stores perceptual roughness: 1-specular.r (or sqrt(this alpha)); BRDF consumes alpha.
    m.metallic = g >= 230 ? 1.0 : 0.0;
    bool knownMetal = g >= 230 && g <= 237;
    m.f0 = g < 230 ? vec3(float(g) / 255.0) : (knownMetal ? tasteMetalF0(g) : linearAlbedo);
    m.reflectionTint = knownMetal ? linearAlbedo : vec3(1.0); // Custom/reserved metal: albedo already IS F0.
    m.porosity = b <= 64 ? float(b) / 64.0 : 0.0;
    m.subsurface = b > 64 ? float(b - 65) / 190.0 : 0.0;
    m.emission = a == 255 ? 0.0 : float(a) / 254.0; // 255 is the no-emission sentinel, not maximum emission.
    return m;
}
vec3 tasteDecodeLabNormal(vec2 rg) {
    vec2 xy = vec2(rg.x * 2.0 - 1.0, 1.0 - rg.y * 2.0); // DirectX Y- -> tangent-space Y+ exactly once.
    return normalize(vec3(xy, sqrt(max(1.0 - dot(xy, xy), 0.0))));
}
// _n.b is material AO; _n.a is height (1 = surface, 0 = deepest). Apply AO only to indirect light.
#endif
