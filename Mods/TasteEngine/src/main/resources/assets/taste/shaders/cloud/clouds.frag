#version 330 core
uniform sampler3D noiseVolume;
uniform vec3 cameraPosition;
uniform vec3 wind;
uniform vec3 sunDirection;
uniform vec3 sunColor;
uniform float time;
uniform vec3 layerBase;
uniform vec3 layerThickness;
uniform vec3 layerCoverage;
uniform vec3 layerDensity;
in vec3 rayDirection;
out vec4 cloudColor;
void main() {
    vec3 direction = normalize(rayDirection);
    vec3 radiance = vec3(0.0);
    float transmittance = 1.0;
    for (int layer = 0; layer < 3; layer++) {
        float base = layerBase[layer];
        float thickness = max(layerThickness[layer], 1.0);
        float distance = (base - cameraPosition.y) / max(direction.y, 0.01);
        vec3 point = cameraPosition + direction * max(distance, 0.0);
        vec3 uvw = point * 0.002 + wind * time * 0.002 + float(layer) * 0.17;
        float shape = texture(noiseVolume, uvw).r - (1.0 - layerCoverage[layer]);
        float density = max(shape, 0.0) * layerDensity[layer];
        float optical = density * thickness * 0.03;
        float layerT = exp(-optical);
        float phase = 0.25 + 0.75 * pow(max(dot(direction, sunDirection), 0.0), 8.0);
        radiance += transmittance * (1.0 - layerT) * sunColor * phase;
        transmittance *= layerT;
    }
    cloudColor = vec4(radiance, 1.0 - transmittance);
}
