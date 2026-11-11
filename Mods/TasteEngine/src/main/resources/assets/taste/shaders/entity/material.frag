#version 330 core
uniform sampler2D albedoSampler;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 rimColor;
uniform float rimStrength;
in vec2 texCoord;
in vec3 normal;
in vec3 viewDirection;
out vec4 color;
void main() {
    vec4 albedo = texture(albedoSampler, texCoord);
    if (albedo.a < 0.05) discard;
    vec3 n = normalize(normal);
    float diffuse = max(dot(n, normalize(lightDirection)), 0.0);
    float rim = pow(1.0 - max(dot(n, normalize(viewDirection)), 0.0), 3.0) * rimStrength;
    color = vec4(albedo.rgb * (0.18 + lightColor * diffuse) + rimColor * rim, albedo.a);
}
