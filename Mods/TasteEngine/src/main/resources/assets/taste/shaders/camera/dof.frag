#version 330 core
uniform sampler2D sceneSampler;
uniform sampler2D depthSampler;
uniform float focusDistance;
uniform float aperture;
uniform float maximumRadius;
in vec2 texCoord;
out vec4 color;
void main(){float d=texture(depthSampler,texCoord).r;float coc=clamp(abs(d-focusDistance)/max(d,0.001)*aperture,0.0,maximumRadius);vec2 t=1.0/textureSize(sceneSampler,0)*coc;vec3 c=texture(sceneSampler,texCoord).rgb*0.2;c+=texture(sceneSampler,texCoord+vec2(t.x,0)).rgb*0.2;c+=texture(sceneSampler,texCoord-vec2(t.x,0)).rgb*0.2;c+=texture(sceneSampler,texCoord+vec2(0,t.y)).rgb*0.2;c+=texture(sceneSampler,texCoord-vec2(0,t.y)).rgb*0.2;color=vec4(c,1);}
