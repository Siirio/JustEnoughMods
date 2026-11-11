#version 330 core

vec3 tasteDirectLight(vec3 albedo, vec3 normal, vec3 viewDirection, vec3 lightDirection,
        vec3 radiance, float roughness, float metallic, vec3 f0, float visibility) {
    vec3 halfVector = normalize(viewDirection + lightDirection);
    float noL = max(dot(normal, lightDirection), 0.0);
    float noV = max(dot(normal, viewDirection), 1.0e-4);
    float noH = max(dot(normal, halfVector), 0.0);
    float voH = max(dot(viewDirection, halfVector), 0.0);
    float alpha = max(roughness * roughness, 0.02);
    float alpha2 = alpha * alpha;
    float denominator = noH * noH * (alpha2 - 1.0) + 1.0;
    float distribution = alpha2 / max(3.14159265 * denominator * denominator, 1.0e-5);
    vec3 fresnel = f0 + (1.0 - f0) * pow(1.0 - voH, 5.0);
    float k = (alpha + 1.0) * (alpha + 1.0) * 0.125;
    float geometry = noL / (noL * (1.0 - k) + k) * noV / (noV * (1.0 - k) + k);
    vec3 specular = distribution * geometry * fresnel / max(4.0 * noL * noV, 1.0e-4);
    vec3 diffuse = (1.0 - metallic) * (1.0 - fresnel) * albedo / 3.14159265;
    return (diffuse + specular) * radiance * noL * visibility;
}
