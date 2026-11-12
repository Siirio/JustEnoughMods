#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NativeEmission;
uniform sampler2D Atmosphere;
uniform float HasAtmosphere;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    vec4 emission = texture(NativeEmission, texCoord);
    float visible = step(emission.a, texture(DepthSampler, texCoord).r + 0.00000024);
    float transmission = HasAtmosphere > 0.5 ? texture(Atmosphere, texCoord).a : 1.0;
    fragColor = vec4(scene.rgb + emission.rgb * (visible * transmission), scene.a);
}
