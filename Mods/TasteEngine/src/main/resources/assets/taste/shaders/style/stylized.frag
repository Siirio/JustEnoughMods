#version 330 core
uniform sampler2D sceneSampler;
uniform sampler2D depthSampler;
uniform float outlineStrength;
uniform int toonBands;
in vec2 texCoord;
out vec4 color;
void main(){vec2 t=1.0/textureSize(sceneSampler,0);vec3 c=texture(sceneSampler,texCoord).rgb;float l=dot(c,vec3(0.2126,0.7152,0.0722));if(toonBands>1)c*=floor(l*toonBands+0.5)/float(toonBands)/max(l,0.02);float d=texture(depthSampler,texCoord).r;float e=abs(d-texture(depthSampler,texCoord+vec2(t.x,0)).r)+abs(d-texture(depthSampler,texCoord+vec2(0,t.y)).r);c*=1.0-smoothstep(0.0002,0.005,e)*outlineStrength;color=vec4(c,1);}
