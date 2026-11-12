#version 150

uniform sampler2D SceneColor;
uniform sampler2D ParticleColor;
uniform sampler2D ParticleReveal;
uniform float Weighted;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(SceneColor, texCoord);
    vec4 particle = texture(ParticleColor, texCoord);
    if (Weighted > 0.5) {
        float reveal = clamp(texture(ParticleReveal, texCoord).r, 0.0, 1.0);
        vec3 average = particle.rgb / max(particle.a, 0.0001);
        fragColor = vec4(scene.rgb * reveal + average * (1.0 - reveal), scene.a);
    } else {
        fragColor = vec4(scene.rgb * (1.0 - particle.a) + particle.rgb, scene.a);
    }
}
