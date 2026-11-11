#ifndef TASTE_WATER_WAVES_GLSL
#define TASTE_WATER_WAVES_GLSL
// Original four-wave height field, not Gerstner geometry. Lengths are blocks; waveNumber = 2*pi/wavelength.
// direction[] is normalized on CPU then uploaded as floats, NEVER re-normalized differently here.
// wrappedPhase[] is computed in CPU double at this frame's origin (WorldWavePhase.atOrigin), including time.
// Use identical parameters in displaced geometry, shading, water depth, and shadow passes.
float tasteWaterHeight(vec2 relativeXZ, vec4 amplitude, vec4 waveNumber,
        vec2 direction[4], vec4 wrappedPhase, out vec3 geometricNormal) {
    float height = 0.0;
    vec2 derivative = vec2(0.0);
    for (int i = 0; i < 4; ++i) {
        float phase = waveNumber[i] * dot(direction[i], relativeXZ) + wrappedPhase[i];
        height += amplitude[i] * sin(phase);
        derivative += amplitude[i] * waveNumber[i] * cos(phase) * direction[i];
    }
    geometricNormal = normalize(vec3(-derivative.x, 1.0, -derivative.y));
    return height;
}
#endif
