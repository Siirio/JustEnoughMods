#version 330 core
uniform sampler2D lowSampler;
uniform sampler2D highSampler;
uniform float strength;
in vec2 texCoord;
out vec4 color;
void main(){ vec2 t=1.0/textureSize(lowSampler,0); vec3 low=texture(lowSampler,texCoord).rgb*4.0; low+=texture(lowSampler,texCoord+vec2(t.x,0)).rgb+texture(lowSampler,texCoord-vec2(t.x,0)).rgb+texture(lowSampler,texCoord+vec2(0,t.y)).rgb+texture(lowSampler,texCoord-vec2(0,t.y)).rgb; color=vec4(texture(highSampler,texCoord).rgb+low*(strength/8.0),1.0); }
