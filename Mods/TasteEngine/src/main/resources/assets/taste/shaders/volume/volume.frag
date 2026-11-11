#version 330 core
uniform sampler2D depthSampler;
uniform sampler2D visibilitySampler;
uniform vec3 fogColor;
uniform float density;
uniform float maximumDistance;
in vec2 texCoord;
out vec4 volumeColor;
void main() {
    float depth = texture(depthSampler, texCoord).r * maximumDistance;
    float visibility = texture(visibilitySampler, texCoord).r;
    float transmittance = exp(-max(density, 0.0) * max(depth, 0.0));
    vec3 scattering = fogColor * (1.0 - transmittance) * visibility;
    volumeColor = vec4(scattering, 1.0 - transmittance);
}
