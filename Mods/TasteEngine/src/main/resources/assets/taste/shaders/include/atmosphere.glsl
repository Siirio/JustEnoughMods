#ifndef TASTE_ATMOSPHERE_GLSL
#define TASTE_ATMOSPHERE_GLSL
// sigmaT = extinction per block; distance is in blocks. Source radiance per block is integrated separately.
vec3 tasteBeerLambert(vec3 sigmaT, float distanceBlocks) {
    return exp(-max(sigmaT, vec3(0.0)) * max(distanceBlocks, 0.0));
}
vec3 tasteFogIntegral(vec3 sigmaT, float distanceBlocks) {
    float d = max(distanceBlocks, 0.0);
    vec3 s = max(sigmaT, vec3(0.0)), tau = s * d;
    vec3 series = d * (vec3(1.0) - 0.5 * tau + tau * tau / 6.0);
    vec3 exact = (vec3(1.0) - exp(-tau)) / max(s, vec3(1e-20));
    return mix(series, exact, step(vec3(0.001), tau));
}
// cosTheta = dot(incoming LIGHT-TRAVEL direction, outgoing direction TO CAMERA).
// When using direction FROM sample TO light, negate that direction before the dot product.
float tasteHenyeyGreenstein(float cosTheta, float anisotropy) {
    float g = clamp(anisotropy, -0.99, 0.99);
    float den = max(1.0 + g * g - 2.0 * g * clamp(cosTheta, -1.0, 1.0), 1e-6);
    return (1.0 - g * g) / (12.56637061436 * den * sqrt(den));
}
#endif
