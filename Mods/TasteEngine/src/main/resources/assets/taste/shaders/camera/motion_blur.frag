#version 330 core
uniform sampler2D sceneSampler;
uniform sampler2D velocitySampler;
uniform float shutter;
in vec2 texCoord;
out vec4 color;
void main(){vec2 v=texture(velocitySampler,texCoord).xy*shutter;vec3 c=vec3(0);const int samples=8;for(int i=0;i<samples;i++){float t=(float(i)/float(samples-1)-0.5);c+=texture(sceneSampler,texCoord+v*t).rgb;}color=vec4(c/float(samples),1);}
