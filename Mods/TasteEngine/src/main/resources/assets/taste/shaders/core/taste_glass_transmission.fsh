#version 330 core

uniform sampler2D BlockAtlas;
uniform sampler2D FirstDepth;
uniform float PeelLayer;
in vec2 texCoord;
in vec4 color;
flat in ivec2 material;
flat in int alphaCutoff;
layout(location = 0) out vec4 fragColor;

void main() {
    if (!gl_FrontFacing || (material.x != 2 && material.x != 4) || (material.y & 2) == 0) discard;
    if (PeelLayer > 0.5) {
        float first = texelFetch(FirstDepth, ivec2(gl_FragCoord.xy), 0).r;
        if (first >= 1.0 || gl_FragCoord.z <= first + (2.0 / 16777215.0)) discard;
    }
    vec4 texel = texture(BlockAtlas, texCoord) * color;
    if (texel.a < float(alphaCutoff) / 65536.0) discard;
    vec3 tint = mix(texel.rgb / 12.92, pow((texel.rgb + 0.055) / 1.055, vec3(2.4)),
            step(vec3(0.04045), texel.rgb));
    float thickness = max(0.04, float((material.y >> 2) & 1023) / 128.0);
    vec3 transmission = pow(mix(vec3(1.0), clamp(tint, 0.035, 1.0), texel.a),
            vec3(0.55 + min(thickness, 2.0) * 0.3));
    fragColor = vec4(transmission, gl_FragCoord.z);
}
