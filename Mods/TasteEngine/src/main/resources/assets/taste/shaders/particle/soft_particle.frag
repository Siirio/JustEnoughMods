#version 330 core
uniform sampler2D particleSampler;
uniform sampler2D sceneDepthSampler;
uniform float softness;
in vec2 texCoord;
in float particleDepth;
in vec4 particleColor;
out vec4 color;
void main() {
    vec4 sprite = texture(particleSampler, texCoord) * particleColor;
    float sceneDepth = texture(sceneDepthSampler, gl_FragCoord.xy / textureSize(sceneDepthSampler, 0)).r;
    float fade = clamp((sceneDepth - particleDepth) / max(softness, 1.0e-5), 0.0, 1.0);
    color = vec4(sprite.rgb * sprite.a * fade, sprite.a * fade);
}
