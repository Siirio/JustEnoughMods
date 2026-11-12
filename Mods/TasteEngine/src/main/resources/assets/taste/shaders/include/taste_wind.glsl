uniform vec4 TasteWindClock;
uniform vec4 TasteWindPreviousClock;
uniform vec3 TasteWindOrigin;
uniform vec4 TasteWindPlants;
uniform float TasteWindWater;

vec3 taste_wind_at(vec3 position, int anchorBits, vec4 clock) {
    int type = anchorBits & 7;
    if (type == 0 || type == 7 || dot(clock.zw, clock.zw) < 0.000001) return position;
    float anchor = float((anchorBits >> 3) & 3) / 3.0;
    if (anchor <= 0.0) return position;
    vec3 p = position + TasteWindOrigin;
    const float scale = 6.28318530718 / 512.0;
    float gust = sin(dot(p.xz, vec2(5.0, 7.0) * scale) + clock.x * 0.25);
    float phase = dot(p, vec3(27.0, 5.0, 39.0) * scale) + clock.x;
    float bend = (sin(phase) * 0.63 + sin(phase * 2.0) * 0.21) * (0.68 + clock.y * (gust * 0.5 + 0.5));
    vec2 wind = clock.zw;
    float strength = type == 1 ? TasteWindPlants.x : type == 2 ? TasteWindPlants.y
            : type == 3 ? TasteWindPlants.z : TasteWindPlants.w;
    if (type == 4) wind.y = 0.0;
    if (type == 5) wind.x = 0.0;
    if (type == 6) {
        bend = sin(dot(p, vec3(8.0, 5.0, 12.0) * scale) + clock.x * 0.20) * 0.55;
        strength = TasteWindWater;
    }
    vec2 displacement = clamp(wind * (bend * anchor * anchor * strength * 0.18), vec2(-0.18), vec2(0.18));
    return position + vec3(displacement.x, 0.0, displacement.y);
}

vec3 taste_deform(vec3 position, int anchorBits) {
    return taste_wind_at(position, anchorBits, TasteWindClock);
}

vec3 taste_deform_previous(vec3 position, int anchorBits) {
    return taste_wind_at(position, anchorBits, TasteWindPreviousClock);
}
