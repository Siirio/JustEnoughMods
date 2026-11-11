#ifndef TASTE_BRDF_GLSL
#define TASTE_BRDF_GLSL
// Original reusable math. All colors are linear. alpha is linear roughness, NOT perceptual roughness.
vec3 tasteFresnelSchlick(vec3 f0, float cosTheta) {
    float f = pow(1.0 - clamp(cosTheta, 0.0, 1.0), 5.0);
    return f0 + (vec3(1.0) - f0) * f;
}
float tasteDistributionGGX(float noH, float alpha) {
    float a = clamp(alpha, 0.002, 1.0);
    float a2 = a * a;
    float d = noH * noH * (a2 - 1.0) + 1.0;
    return a2 / max(3.14159265359 * d * d, 1e-14);
}
float tasteSmithGGX1(float noX, float alpha) {
    float a = clamp(alpha, 0.002, 1.0);
    float a2 = a * a;
    return 2.0 * noX / max(noX + sqrt(a2 + (1.0 - a2) * noX * noX), 1e-7);
}
// Returns BRDF * NoL; caller multiplies by incident radiance and visibility. N,V,L are unit vectors.
vec3 tasteEvaluateBRDF(vec3 albedo, vec3 f0, float metallic, float alpha, vec3 n, vec3 v, vec3 l) {
    float noL = clamp(dot(n, l), 0.0, 1.0), noV = clamp(dot(n, v), 0.0, 1.0);
    vec3 halfSum = v + l;
    if (noL <= 0.0 || noV <= 0.0 || dot(halfSum, halfSum) < 1e-10) return vec3(0.0);
    vec3 h = normalize(halfSum);
    vec3 f = tasteFresnelSchlick(clamp(f0, 0.0, 1.0), max(dot(v, h), 0.0));
    float d = tasteDistributionGGX(clamp(dot(n, h), 0.0, 1.0), alpha);
    float g = tasteSmithGGX1(noV, alpha) * tasteSmithGGX1(noL, alpha);
    vec3 specular = f * (d * g / max(4.0 * noV * noL, 1e-7));
    vec3 diffuse = (vec3(1.0) - f) * (1.0 - clamp(metallic, 0.0, 1.0)) * albedo / 3.14159265359;
    return (diffuse + specular) * noL;
}
#endif
