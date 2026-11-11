#version 330 core
uniform sampler2D currentSampler;
uniform sampler2D historySampler;
uniform sampler2D velocitySampler;
uniform sampler2D reactiveSampler;
uniform float historyWeight;
in vec2 texCoord;
out vec4 color;
void main() {
    vec2 velocity = texture(velocitySampler, texCoord).xy;
    float reactive = texture(reactiveSampler, texCoord).r;
    vec3 current = texture(currentSampler, texCoord).rgb;
    vec3 history = texture(historySampler, texCoord - velocity).rgb;
    vec3 minimumColor = current;
    vec3 maximumColor = current;
    vec2 texel = 1.0 / textureSize(currentSampler, 0);
    for (int y=-1;y<=1;y++) for(int x=-1;x<=1;x++) { vec3 sampleColor=texture(currentSampler,texCoord+vec2(x,y)*texel).rgb; minimumColor=min(minimumColor,sampleColor); maximumColor=max(maximumColor,sampleColor); }
    history = clamp(history, minimumColor, maximumColor);
    color = vec4(mix(current, history, historyWeight * (1.0 - reactive)), 1.0);
}
