#version 150

uniform sampler2D DisplaySampler;
uniform float ColorVision;
in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

vec3 assist(vec3 color) {
    if (ColorVision < 0.5) return color;
    float redGreen = color.r - color.g;
    float blueYellow = color.b - dot(color.rg, vec2(0.23, 0.77));
    vec3 correction;
    if (ColorVision < 1.5) correction = vec3(0.0, redGreen * 0.10, redGreen * 0.52);
    else if (ColorVision < 2.5) correction = vec3(redGreen * 0.15, 0.0, -redGreen * 0.48);
    else correction = vec3(blueYellow * 0.54, -blueYellow * 0.11, 0.0);
    correction -= dot(correction, LUMA);
    float headroom = 1.0;
    for (int channel = 0; channel < 3; channel++) {
        float value = correction[channel];
        if (value > 0.0) headroom = min(headroom, (1.0 - color[channel]) / value);
        if (value < 0.0) headroom = min(headroom, -color[channel] / value);
    }
    return color + correction * clamp(headroom, 0.0, 1.0);
}

void main() {
    vec4 original = texture(DisplaySampler, texCoord);
    fragColor = vec4(assist(original.rgb), original.a);
}
