uniform sampler2D ProbeAtlas;
uniform sampler2D ProbeDepth;
uniform sampler2D PlanarCapture;
uniform vec3 ProbeOrigin;
uniform float ProbeFaceSize;
uniform float ProbeRange;
uniform mat4 PlanarProjection;
uniform float PlanarEnabled;
uniform float PlanarHeight;
uniform float CaptureNativeSrgb;

vec3 tasteCaptureLinear(vec4 sampleValue) {
    vec3 color = clamp(sampleValue.rgb / max(sampleValue.a, 0.001), 0.0, 16.0);
    if (CaptureNativeSrgb < 0.5) return color;
    return mix(color / 12.92, pow((color + 0.055) / 1.055, vec3(2.4)), greaterThan(color, vec3(0.04045)));
}

vec2 tasteProbeAtlasUv(vec3 ray, float inset) {
    vec3 extent = abs(ray);
    int face;
    vec2 point;
    if (extent.x >= max(extent.y, extent.z)) {
        face = ray.x >= 0.0 ? 0 : 1;
        point = vec2(ray.x >= 0.0 ? -ray.z : ray.z, -ray.y) / extent.x;
    } else if (extent.y >= extent.z) {
        face = ray.y >= 0.0 ? 2 : 3;
        point = vec2(ray.x, ray.y >= 0.0 ? ray.z : -ray.z) / extent.y;
    } else {
        face = ray.z >= 0.0 ? 4 : 5;
        point = vec2(ray.z >= 0.0 ? ray.x : -ray.x, -ray.y) / extent.z;
    }
    vec2 local = clamp(point * 0.5 + 0.5, vec2(inset), vec2(1.0 - inset));
    return (vec2(float(face % 3), float(face / 3)) + local) / vec2(3.0, 2.0);
}

vec3 tasteProbeParallax(vec3 position, vec3 ray) {
    float capturedDepth = texture(ProbeDepth, tasteProbeAtlasUv(ray, 0.5 / ProbeFaceSize)).r;
    if (capturedDepth >= 0.999999) return ray;
    const float nearPlane = 0.08;
    float cameraDepth = ProbeRange * nearPlane / max(nearPlane, ProbeRange - capturedDepth * (ProbeRange - nearPlane));
    vec3 extent = abs(ray);
    float hitRadius = cameraDepth / max(extent.x, max(extent.y, extent.z));
    vec3 delta = position - ProbeOrigin;
    if (dot(delta, delta) >= hitRadius * hitRadius) return ray;
    float b = dot(delta, ray);
    float d = b * b - dot(delta, delta) + hitRadius * hitRadius;
    if (d <= 0.0) return ray;
    float distanceToHit = -b + sqrt(d);
    return normalize(delta + ray * distanceToHit);
}

/** Captured radiance in RGB and independent confidence in alpha. */
vec4 tasteLocalProbe(vec3 position, vec3 worldRay, float roughness) {
    if (ProbeFaceSize < 1.0 || ProbeRange <= 0.0) return vec4(0.0);
    float weight = 1.0 - smoothstep(ProbeRange * 0.18, ProbeRange * 0.55, length(position - ProbeOrigin));
    if (weight <= 0.0) return vec4(0.0);
    vec3 ray = tasteProbeParallax(position, normalize(worldRay));
    float maxMip = log2(ProbeFaceSize);
    float mip = clamp(roughness * roughness, 0.0, 1.0) * maxMip;
    float inset = exp2(ceil(mip)) * 0.5 / ProbeFaceSize;
    vec4 captured = textureLod(ProbeAtlas, tasteProbeAtlasUv(ray, inset), mip);
    return vec4(tasteCaptureLinear(captured), clamp(captured.a, 0.0, 1.0) * weight);
}

vec4 tastePlanarWater(vec3 position, vec2 waveOffset) {
    if (PlanarEnabled < 0.5 || abs(position.y - PlanarHeight) > 0.22) return vec4(0.0);
    vec4 projected = PlanarProjection * vec4(position.x, PlanarHeight, position.z, 1.0);
    if (projected.w <= 0.0001) return vec4(0.0);
    vec2 uv = projected.xy / projected.w * 0.5 + 0.5 + waveOffset;
    vec2 edge = min(uv, 1.0 - uv);
    float confidence = smoothstep(0.0, 0.075, min(edge.x, edge.y));
    if (confidence <= 0.0) return vec4(0.0);
    vec4 captured = texture(PlanarCapture, uv);
    return vec4(tasteCaptureLinear(captured), clamp(captured.a, 0.0, 1.0) * confidence);
}
