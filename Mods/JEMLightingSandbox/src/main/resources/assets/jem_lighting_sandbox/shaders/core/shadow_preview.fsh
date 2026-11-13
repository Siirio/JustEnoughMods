#version 150

in vec2 sceneUv;
out vec4 fragColor;

uniform float Aspect;
uniform float Orbit;
uniform float RedEnabled;
uniform float BlueEnabled;
uniform float SunEnabled;

const float NO_HIT = 10000.0;
const float SURFACE_OFFSET = 0.002;

const vec3 FLOOR_MIN = vec3(-5.75, -0.30, -3.85);
const vec3 FLOOR_MAX = vec3( 5.75,  0.00,  4.50);
const vec3 COLUMN_MIN = vec3(-0.65, 0.00, -0.60);
const vec3 COLUMN_MAX = vec3( 0.65, 2.40,  0.60);
const vec3 RED_POSITION = vec3(-2.60, 1.32, -1.85);
const vec3 BLUE_POSITION = vec3(2.60, 1.32, -2.05);
const vec3 LAMP_HALF_SIZE = vec3(0.29);
const vec3 RED_COLOR = vec3(1.00, 0.025, 0.018);
const vec3 BLUE_COLOR = vec3(0.025, 0.16, 1.00);

const int FLOOR = 1;
const int COLUMN = 2;
const int POST = 3;
const int RED_LAMP = 4;
const int BLUE_LAMP = 5;

struct Ray {
    vec3 origin;
    vec3 direction;
    vec3 inverseDirection;
};

struct Hit {
    float distance;
    vec3 normal;
    int material;
};

Ray makeRay(vec3 origin, vec3 direction) {
    vec3 nonzero = mix(vec3(0.000001), direction, greaterThanEqual(abs(direction), vec3(0.000001)));
    return Ray(origin, direction, 1.0 / nonzero);
}

vec2 boxInterval(Ray ray, vec3 lower, vec3 upper, out vec3 nearPlanes) {
    vec3 first = (lower - ray.origin) * ray.inverseDirection;
    vec3 second = (upper - ray.origin) * ray.inverseDirection;
    nearPlanes = min(first, second);
    vec3 farPlanes = max(first, second);
    return vec2(max(nearPlanes.x, max(nearPlanes.y, nearPlanes.z)),
                min(farPlanes.x, min(farPlanes.y, farPlanes.z)));
}

void intersectBox(Ray ray, vec3 lower, vec3 upper, int material, inout Hit hit) {
    vec3 nearPlanes;
    vec2 interval = boxInterval(ray, lower, upper, nearPlanes);
    if (interval.x <= 0.0 || interval.x > interval.y || interval.x >= hit.distance) {
        return;
    }

    vec3 normal = vec3(0.0);
    if (nearPlanes.x >= nearPlanes.y && nearPlanes.x >= nearPlanes.z) {
        normal.x = -sign(ray.direction.x);
    } else if (nearPlanes.y >= nearPlanes.z) {
        normal.y = -sign(ray.direction.y);
    } else {
        normal.z = -sign(ray.direction.z);
    }
    hit = Hit(interval.x, normal, material);
}

void intersectLamp(Ray ray, vec3 center, int material, inout Hit hit) {
    intersectBox(ray, center - LAMP_HALF_SIZE, center + LAMP_HALF_SIZE, material, hit);
    vec3 foot = vec3(center.x, 0.0, center.z);
    intersectBox(ray, foot - vec3(0.14, 0.0, 0.14),
                 foot + vec3(0.14, center.y - LAMP_HALF_SIZE.y, 0.14), POST, hit);
}

Hit traceScene(Ray ray) {
    Hit hit = Hit(NO_HIT, vec3(0.0), 0);
    intersectBox(ray, FLOOR_MIN, FLOOR_MAX, FLOOR, hit);
    intersectBox(ray, COLUMN_MIN, COLUMN_MAX, COLUMN, hit);
    intersectLamp(ray, RED_POSITION, RED_LAMP, hit);
    intersectLamp(ray, BLUE_POSITION, BLUE_LAMP, hit);
    return hit;
}

float columnVisibility(vec3 position, vec3 normal, vec3 direction, float maxDistance) {
    Ray ray = makeRay(position + normal * SURFACE_OFFSET, direction);
    vec3 nearPlanes;
    vec2 interval = boxInterval(ray, COLUMN_MIN, COLUMN_MAX, nearPlanes);
    bool blocked = interval.x > 0.0 && interval.x <= interval.y && interval.x < maxDistance;
    return blocked ? 0.0 : 1.0;
}

vec3 lampLight(vec3 point, vec3 normal, vec3 lamp, vec3 color, float enabled) {
    if (enabled < 0.5) {
        return vec3(0.0);
    }
    vec3 toLight = lamp - point;
    float distanceSquared = dot(toLight, toLight);
    float distanceToLight = sqrt(max(distanceSquared, SURFACE_OFFSET));
    vec3 lightDirection = toLight / distanceToLight;
    float incidence = max(dot(normal, lightDirection), 0.0);
    if (incidence <= 0.0) {
        return vec3(0.0);
    }
    float visibility = columnVisibility(point, normal, lightDirection, distanceToLight);
    float attenuation = 7.0 / (0.65 + distanceSquared);
    return color * attenuation * incidence * visibility;
}

vec3 sunLight(vec3 point, vec3 normal) {
    vec3 direction = normalize(vec3(0.42, 0.84, -0.50));
    float incidence = max(dot(normal, direction), 0.0);
    if (SunEnabled < 0.5 || incidence <= 0.0) {
        return vec3(0.0);
    }
    float visibility = columnVisibility(point, normal, direction, NO_HIT);
    return vec3(0.31) * incidence * visibility;
}

vec3 surfaceColor(Hit hit, vec3 point, vec2 sampleWidth) {
    if (hit.material == FLOOR) {
        vec2 block = fract(point.xz);
        vec2 seam = min(block, 1.0 - block);
        vec2 edge = smoothstep(vec2(0.012), vec2(0.018) + sampleWidth, seam);
        return vec3(mix(0.32, 0.51, min(edge.x, edge.y)));
    }
    if (hit.material == COLUMN) {
        return vec3(0.48);
    }
    return vec3(0.18);
}

vec3 toDisplayColor(vec3 linearColor) {
    vec3 mapped = vec3(1.0) - exp(-linearColor);
    return pow(mapped, vec3(1.0 / 2.2));
}

void main() {
    float aspect = max(Aspect, 0.1);
    float halfHeight = 4.3 * max(1.0, 1.45 / aspect);
    vec2 screen = vec2(2.0 * sceneUv.x - 1.0, 1.0 - 2.0 * sceneUv.y);
    vec3 fromScene = normalize(vec3(sin(Orbit), 0.92, cos(Orbit)));
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), fromScene));
    vec3 up = cross(fromScene, right);
    vec3 center = vec3(0.0, 0.35, 0.25);
    vec3 origin = center + fromScene * 15.0
                + right * (screen.x * aspect * halfHeight) + up * (screen.y * halfHeight);
    Ray camera = makeRay(origin, -fromScene);
    Hit hit = traceScene(camera);
    vec3 point = camera.origin + camera.direction * hit.distance;
    vec2 sampleWidth = min(fwidth(point.xz), vec2(0.07));

    if (hit.material == 0) {
        fragColor = vec4(vec3(0.10), 1.0);
        return;
    }

    vec3 light = vec3(0.085);
    light += sunLight(point, hit.normal);
    light += lampLight(point, hit.normal, RED_POSITION, RED_COLOR, RedEnabled);
    light += lampLight(point, hit.normal, BLUE_POSITION, BLUE_COLOR, BlueEnabled);

    vec3 emitted = vec3(0.0);
    if (hit.material == RED_LAMP) {
        emitted = RED_COLOR * RedEnabled * 1.7;
    } else if (hit.material == BLUE_LAMP) {
        emitted = BLUE_COLOR * BlueEnabled * 1.7;
    }
    vec3 linearColor = surfaceColor(hit, point, sampleWidth) * light + emitted;
    fragColor = vec4(toDisplayColor(linearColor), 1.0);
}
