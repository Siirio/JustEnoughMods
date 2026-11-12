#version 150
uniform sampler2D DataSampler;
uniform sampler2D SecondarySampler;
uniform mat4 InverseProjection;
uniform float Mode;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    int mode = int(Mode);
    vec3 color;
    if (mode == 0) {
        float checker = mod(floor(gl_FragCoord.x / 24.0) + floor(gl_FragCoord.y / 24.0), 2.0);
        color = mix(vec3(0.13, 0.03, 0.13), vec3(0.65, 0.07, 0.65), checker);
    } else {
        vec4 data = texture(DataSampler, texCoord);
        if (mode == 1) {
            vec4 view = InverseProjection * vec4(texCoord * 2.0 - 1.0, data.r * 2.0 - 1.0, 1.0);
            color = vec3(clamp(log2(1.0 + abs(view.z / view.w)) / 10.0, 0.0, 1.0));
        } else if (mode == 2) {
            vec3 n = vec3(data.xy, 1.0 - abs(data.x) - abs(data.y));
            float fold = max(0.0, -n.z);
            n.xy -= mix(vec2(-fold), vec2(fold), step(vec2(0.0), n.xy));
            color = normalize(n) * 0.5 + 0.5;
        } else if (mode == 5) {
            color = vec3(0.5 + data.xy * 22.0, min(length(data.xy) * 30.0, 1.0));
        } else if (mode == 6) {
            color = vec3(data.r);
        } else if (mode == 7) {
            color = vec3(data.z, data.w / 255.0, 1.0 - data.z);
        } else if (mode == 8) {
            vec3 change = abs(data.rgb - texture(SecondarySampler, texCoord).rgb);
            color = vec3(clamp(max(change.r, max(change.g, change.b)) * 5.0, 0.0, 1.0), 0.0, 0.0);
        } else if (mode == 10) {
            color = vec3(clamp(0.5 + data.r * 0.1, 0.0, 1.0));
        } else {
            color = pow(max(data.rgb, vec3(0.0)), vec3(1.0 / 2.2));
        }
    }
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
