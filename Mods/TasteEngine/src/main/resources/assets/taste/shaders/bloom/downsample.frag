#version 330 core
uniform sampler2D sourceSampler;
uniform float threshold;
uniform float knee;
in vec2 texCoord;
out vec4 color;
void main() {
    vec2 texel = 1.0 / textureSize(sourceSampler, 0);
    vec3 sampleColor = (texture(sourceSampler,texCoord+texel*vec2(-0.5,-0.5)).rgb + texture(sourceSampler,texCoord+texel*vec2(0.5,-0.5)).rgb + texture(sourceSampler,texCoord+texel*vec2(-0.5,0.5)).rgb + texture(sourceSampler,texCoord+texel*vec2(0.5,0.5)).rgb) * 0.25;
    float brightness = max(sampleColor.r,max(sampleColor.g,sampleColor.b));
    float soft = max(0.0, brightness-threshold+knee); soft = soft*soft/max(4.0*knee+1.0e-5,1.0e-5);
    float contribution = max(brightness-threshold,soft)/max(brightness,1.0e-5);
    color = vec4(sampleColor*contribution,1.0);
}
