#version 150

uniform sampler2D WorldColor;
uniform sampler2D WorldDepth;
uniform sampler2D TranslucentColor;
uniform sampler2D TranslucentDepth;
uniform sampler2D ItemEntityColor;
uniform sampler2D ItemEntityDepth;
uniform sampler2D ParticlesColor;
uniform sampler2D ParticlesDepth;
uniform sampler2D WeatherColor;
uniform sampler2D WeatherDepth;
uniform sampler2D CloudsColor;
uniform sampler2D CloudsDepth;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 layers[5] = vec4[5](
        texture(TranslucentColor, texCoord), texture(ItemEntityColor, texCoord),
        texture(ParticlesColor, texCoord), texture(WeatherColor, texCoord), texture(CloudsColor, texCoord));
    float depths[5] = float[5](
        texture(TranslucentDepth, texCoord).r, texture(ItemEntityDepth, texCoord).r,
        texture(ParticlesDepth, texCoord).r, texture(WeatherDepth, texCoord).r, texture(CloudsDepth, texCoord).r);
    float worldDepth = texture(WorldDepth, texCoord).r;
    vec3 color = vec3(0.0);
    float visibility = 1.0;
    for (int sampleIndex = 0; sampleIndex < 5; sampleIndex++) {
        int front = -1;
        float frontDepth = worldDepth + 0.000001;
        for (int index = 4; index >= 0; index--) {
            if (layers[index].a > 0.0 && depths[index] < frontDepth) {
                front = index;
                frontDepth = depths[index];
            }
        }
        if (front < 0) break;
        color += layers[front].rgb * visibility;
        visibility *= 1.0 - clamp(layers[front].a, 0.0, 1.0);
        layers[front].a = 0.0;
    }
    fragColor = vec4(color + texture(WorldColor, texCoord).rgb * visibility, 1.0);
}
