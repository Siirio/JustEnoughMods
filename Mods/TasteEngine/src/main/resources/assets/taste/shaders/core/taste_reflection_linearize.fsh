#version 150

uniform sampler2D NativeColor;
uniform sampler2D NativeDepth;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (texture(NativeDepth, texCoord).r >= 0.999999) {
        fragColor = vec4(0.0);
        return;
    }
    vec3 color = texture(NativeColor, texCoord).rgb;
    vec3 radiance = mix(color / 12.92, pow((color + 0.055) / 1.055, vec3(2.4)), greaterThan(color, vec3(0.04045)));
    fragColor = vec4(radiance, 1.0);
}
