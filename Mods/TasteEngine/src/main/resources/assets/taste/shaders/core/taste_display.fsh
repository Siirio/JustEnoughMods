#version 150

uniform sampler2D DisplaySampler;

in vec2 texCoord;
out vec4 fragColor;

vec3 encodeDisplayColor(vec3 color) {
    vec3 low = color * 12.92;
    vec3 high = 1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055;
    return mix(low, high, step(vec3(0.0031308), color));
}

vec3 sampleLinear(vec2 uv) {
    ivec2 size = textureSize(DisplaySampler, 0);
    vec2 pixel = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(pixel));
    ivec2 limit = size - 1;
    vec2 f = fract(pixel);
    vec3 a = texelFetch(DisplaySampler, clamp(base, ivec2(0), limit), 0).rgb;
    vec3 b = texelFetch(DisplaySampler, clamp(base + ivec2(1, 0), ivec2(0), limit), 0).rgb;
    vec3 c = texelFetch(DisplaySampler, clamp(base + ivec2(0, 1), ivec2(0), limit), 0).rgb;
    vec3 d = texelFetch(DisplaySampler, clamp(base + ivec2(1), ivec2(0), limit), 0).rgb;
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float luma(vec3 linear) {
    return dot(sqrt(max(linear, vec3(0.0))), vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec2 pixel = 1.0 / vec2(textureSize(DisplaySampler, 0));
    vec3 center = texture(DisplaySampler, texCoord).rgb;
    float nw = luma(texture(DisplaySampler, texCoord + pixel * vec2(-1, 1)).rgb);
    float ne = luma(texture(DisplaySampler, texCoord + pixel * vec2(1, 1)).rgb);
    float sw = luma(texture(DisplaySampler, texCoord + pixel * vec2(-1, -1)).rgb);
    float se = luma(texture(DisplaySampler, texCoord + pixel * vec2(1, -1)).rgb);
    float minimum = min(luma(center), min(min(nw, ne), min(sw, se)));
    float maximum = max(luma(center), max(max(nw, ne), max(sw, se)));
    if (maximum - minimum > max(0.0312, maximum * 0.125)) {
        vec2 direction = vec2(sw + se - nw - ne, nw + sw - ne - se);
        float regularization = max((nw + ne + sw + se) * 0.03125, 0.0078125);
        direction = clamp(direction / (min(abs(direction.x), abs(direction.y)) + regularization), -8.0, 8.0) * pixel;
        vec3 inner = (sampleLinear(texCoord - direction / 6.0) + sampleLinear(texCoord + direction / 6.0)) * 0.5;
        vec3 wide = inner * 0.5 + (sampleLinear(texCoord - direction * 0.5)
                + sampleLinear(texCoord + direction * 0.5)) * 0.25;
        float wideLuma = luma(wide);
        center = wideLuma < minimum || wideLuma > maximum ? inner : wide;
    }
    fragColor = vec4(encodeDisplayColor(clamp(center, 0.0, 1.0)), 1.0);
}
