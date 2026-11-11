#version 330 core
uniform sampler2D sceneSampler;
uniform float exposure;
uniform float saturation;
uniform float contrast;
uniform float gammaValue;
uniform int toneMapper;
in vec2 texCoord;
out vec4 color;
vec3 filmic(vec3 x){return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0);}
vec3 neutral(vec3 c){float x=min(c.r,min(c.g,c.b));float o=x<0.08?x-6.25*x*x:0.04;c-=o;float p=max(c.r,max(c.g,c.b));if(p<0.76)return c;float q=1.0-0.0576/(p+0.24-0.76);c*=q/p;return mix(c,vec3(q),1.0-1.0/(0.15*(p-q)+1.0));}
void main(){vec3 c=max(texture(sceneSampler,texCoord).rgb*exp2(exposure),vec3(0));float l=dot(c,vec3(0.2126,0.7152,0.0722));c=mix(vec3(l),c,saturation);c=(c-0.18)*contrast+0.18;c=toneMapper==0?filmic(c):neutral(c);c=pow(max(c,vec3(0)),vec3(1.0/max(gammaValue,0.01)));color=vec4(c,1);}
