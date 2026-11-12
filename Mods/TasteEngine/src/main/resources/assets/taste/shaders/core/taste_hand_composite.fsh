#version 150

uniform sampler2D HandColor;
uniform sampler2D HandDepth;
uniform sampler2D PreviousHand;
uniform mat4 HandProjection;
uniform mat4 InverseHandProjection;
uniform vec3 LightDirection;
uniform vec3 LightColor;
uniform float LightStrength;
uniform float SelfShadow;
uniform float DirectionalLighting;
uniform float Aperture;
uniform float BlurRadius;
uniform float FocalLength;
uniform float Shutter;
uniform float MotionSamples;
uniform float HandEmission;
uniform vec3 EnvironmentUp;
uniform vec3 EnvironmentSky;
uniform vec3 EnvironmentHorizon;
uniform float EnvironmentStrength;
uniform float ReflectRoughness;
uniform float ReflectMetallic;
uniform float ReflectF0;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const vec2 RING[8] = vec2[](vec2(1, 0), vec2(0.707, 0.707), vec2(0, 1), vec2(-0.707, 0.707),
        vec2(-1, 0), vec2(-0.707, -0.707), vec2(0, -1), vec2(0.707, -0.707));

vec3 decodeDisplay(vec3 color) {
    color = max(color, vec3(0.0));
    return mix(color / 12.92, pow((color + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), color));
}

vec3 encodeDisplay(vec3 color) {
    color = max(color, vec3(0.0));
    return mix(color * 12.92, 1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), color));
}

bool inFrame(vec2 uv) {
    return all(greaterThanEqual(uv, vec2(0.0))) && all(lessThanEqual(uv, vec2(1.0)));
}

vec3 viewPosition(vec2 uv, out bool valid) {
    float depth = texture(HandDepth, uv).r;
    valid = inFrame(uv) && depth < 0.999999;
    vec4 view = InverseHandProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return view.xyz / max(view.w, 1.0e-6);
}

vec3 normalAt(vec2 uv, vec3 center, vec2 pixel) {
    bool xValid;
    bool yValid;
    vec3 x = viewPosition(uv + vec2(pixel.x, 0), xValid);
    vec3 y = viewPosition(uv + vec2(0, pixel.y), yValid);
    if (!xValid || !yValid || max(abs(x.z - center.z), abs(y.z - center.z)) > 0.05) return vec3(0.0);
    vec3 normal = cross(x - center, y - center);
    float magnitude = length(normal);
    return magnitude > 1.0e-8 ? normal / magnitude : vec3(0.0);
}

float selfVisibility(vec3 center, vec3 direction) {
    if (SelfShadow < 0.5) return 1.0;
    float visible = 1.0;
    for (int tap = 1; tap <= 4; tap++) {
        vec3 ray = center + direction * (float(tap) * 0.022);
        vec4 clip = HandProjection * vec4(ray, 1.0);
        if (clip.w <= 0.0) continue;
        vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
        bool valid;
        vec3 hit = viewPosition(uv, valid);
        if (valid && hit.z > ray.z + 0.012 && hit.z < ray.z + 0.13)
            visible -= 0.18;
    }
    return visible;
}

float focusDepth(float currentDepth) {
    const vec2 anchors[6] = vec2[](vec2(0.22, 0.22), vec2(0.36, 0.32), vec2(0.48, 0.35),
            vec2(0.52, 0.35), vec2(0.64, 0.32), vec2(0.78, 0.22));
    float nearest = 16.0;
    for (int tap = 0; tap < 6; tap++) {
        bool valid;
        vec3 view = viewPosition(anchors[tap], valid);
        if (valid) nearest = min(nearest, -view.z);
    }
    return nearest < 16.0 ? nearest : currentDepth;
}

vec4 focusBlur(vec2 uv, vec4 center, float depth, vec2 pixel) {
    if (BlurRadius <= 0.0) return center;
    float focus = max(FocalLength + 0.0001, focusDepth(depth));
    float coc = FocalLength * FocalLength * abs(depth - focus)
            / max(Aperture * depth * (focus - FocalLength), 1.0e-8);
    float radius = min(BlurRadius, coc / (pixel.y * 0.048));
    if (radius < 0.5) return center;
    vec4 sum = center;
    float count = 1.0;
    for (int tap = 0; tap < 8; tap++) {
        vec2 sampleUv = uv + RING[tap] * pixel * radius;
        bool valid;
        vec3 view = viewPosition(sampleUv, valid);
        if (valid && abs(-view.z - depth) > max(0.03, depth * 0.16)) continue;
        sum += inFrame(sampleUv) ? texture(HandColor, sampleUv) : vec4(0.0);
        count += 1.0;
    }
    return sum / count;
}

float matchError(vec2 uv, vec2 offset, vec2 pixel) {
    vec4 current = texture(HandColor, uv);
    vec4 old = texture(PreviousHand, uv + offset);
    vec4 currentX = texture(HandColor, uv + vec2(pixel.x, 0.0));
    vec4 oldX = texture(PreviousHand, uv + offset + vec2(pixel.x, 0.0));
    vec4 currentY = texture(HandColor, uv + vec2(0.0, pixel.y));
    vec4 oldY = texture(PreviousHand, uv + offset + vec2(0.0, pixel.y));
    return dot(abs(current - old) + abs(currentX - oldX) + abs(currentY - oldY),
            vec4(0.12, 0.3, 0.08, 0.5));
}

vec4 motionBlur(vec2 uv, vec4 center, float depth, vec2 pixel) {
    if (Shutter <= 0.0) return center;
    float baseline = matchError(uv, vec2(0.0), pixel);
    if (baseline < 0.025) return center;
    float best = baseline;
    vec2 offset = vec2(0.0);
    for (int tap = 0; tap < 8; tap++) {
        for (int distance = 2; distance <= 8; distance += 6) {
            vec2 candidate = RING[tap] * pixel * float(distance);
            if (!inFrame(uv + candidate)) continue;
            float error = matchError(uv, candidate, pixel) + float(distance) * 0.002;
            if (error < best) {
                best = error;
                offset = candidate;
            }
        }
    }
    if (best > min(0.15, baseline * 0.7)) return center;
    int count = int(clamp(MotionSamples, 2.0, 16.0));
    vec4 sum = center;
    float weight = 1.0;
    for (int tap = 1; tap < count; tap++) {
        vec2 sampleUv = uv + offset * (float(tap) / float(count - 1)) * Shutter;
        bool valid;
        vec3 view = viewPosition(sampleUv, valid);
        if (valid && abs(-view.z - depth) > max(0.03, depth * 0.16)) continue;
        sum += inFrame(sampleUv) ? texture(HandColor, sampleUv) : vec4(0.0);
        weight += 1.0;
    }
    return sum / weight;
}

vec3 handGlow(vec2 uv, vec2 pixel) {
    if (HandEmission <= 0.0) return vec3(0.0);
    vec3 glow = vec3(0.0);
    for (int tap = 0; tap < 8; tap++) {
        vec2 sampleUv = uv + RING[tap] * pixel * 4.0;
        if (!inFrame(sampleUv)) continue;
        vec4 color = texture(HandColor, sampleUv);
        if (color.a <= 0.01) continue;
        vec3 linear = decodeDisplay(color.rgb / color.a);
        glow += linear * smoothstep(0.35, 0.9, dot(linear, LUMA)) * color.a;
    }
    return glow * min(1.0, HandEmission) * 0.045;
}

vec3 environmentReflection(vec3 base, vec3 view, vec3 normal) {
    vec3 towardsEye = normalize(-view);
    vec3 ray = reflect(-towardsEye, normal);
    float elevation = dot(ray, normalize(EnvironmentUp));
    float roughness = clamp(ReflectRoughness, 0.0, 1.0);
    float sky = mix(smoothstep(-0.18, 0.72, elevation), 0.45, roughness * roughness);
    vec3 ambient = mix(EnvironmentHorizon, EnvironmentSky, sky);
    vec3 f0 = mix(vec3(ReflectF0), clamp(base, 0.0, 1.0), ReflectMetallic);
    float grazing = pow(1.0 - max(dot(towardsEye, normal), 0.0), 5.0);
    vec3 fresnel = f0 + (1.0 - f0) * grazing;
    return ambient * fresnel * EnvironmentStrength * mix(0.42, 0.14, roughness);
}

void main() {
    vec2 pixel = 1.0 / vec2(textureSize(HandColor, 0));
    vec4 hand = texture(HandColor, texCoord);
    if (hand.a <= 0.005 && HandEmission <= 0.0) discard;
    vec3 glow = handGlow(texCoord, pixel);
    bool validDepth;
    vec3 view = viewPosition(texCoord, validDepth);
    if (validDepth && hand.a > 0.005) {
        hand = focusBlur(texCoord, hand, max(0.05, -view.z), pixel);
        hand = motionBlur(texCoord, hand, max(0.05, -view.z), pixel);
        if ((LightStrength > 0.0 || EnvironmentStrength > 0.0) && hand.a > 0.005) {
            vec3 normal = normalAt(texCoord, view, pixel);
            if (dot(normal, normal) > 0.0) {
                vec3 direction = normalize(LightDirection);
                float direct = max(dot(normal, direction), 0.0);
                vec3 albedo = decodeDisplay(hand.rgb / hand.a);
                float visibility = direct > 0.0 && LightStrength > 0.0 ? selfVisibility(view, direction) : 1.0;
                vec3 lit = albedo + albedo * LightColor * (direct * LightStrength * DirectionalLighting * visibility);
                lit *= 1.0 - direct * LightStrength * (1.0 - visibility);
                if (EnvironmentStrength > 0.0) lit += environmentReflection(albedo, view, normal);
                hand.rgb = encodeDisplay(lit) * hand.a;
            }
        }
    }
    if (hand.a <= 0.005 && dot(glow, LUMA) <= 0.0001) discard;
    fragColor = vec4(hand.rgb + glow, clamp(hand.a, 0.0, 1.0));
}
