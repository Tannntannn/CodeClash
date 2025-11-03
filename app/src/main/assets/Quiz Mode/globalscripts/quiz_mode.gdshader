shader_type canvas_item;

uniform float border_thickness : hint_range(0.0, 0.3) = 0.08;
uniform float flash_intensity : hint_range(0.0, 1.0) = 0.8;
uniform float fade_softness : hint_range(0.01, 0.2) = 0.04;
uniform float edge_intensity : hint_range(1.0, 3.0) = 2.0;
uniform vec3 warning_color : source_color = vec3(1.0, 0.0, 0.0);

void fragment() {
    vec2 uv = UV;

    // Calculate distance from each edge
    float left_dist = uv.x;
    float right_dist = 1.0 - uv.x;
    float top_dist = uv.y;
    float bottom_dist = 1.0 - uv.y;

    // Find the minimum distance to any edge
    float edge_dist = min(min(left_dist, right_dist), min(top_dist, bottom_dist));

    // Create smooth border with enhanced fade
    float border = 0.0;
    if (edge_dist < border_thickness) {
        // Main fade gradient
        border = 1.0 - smoothstep(0.0, border_thickness, edge_dist);

        // Apply fade softness for smoother transition
        border = smoothstep(0.0, fade_softness, border_thickness - edge_dist) * border;

        // Add extra intensity at the very edges for dramatic effect
        float edge_boost = 1.0 - smoothstep(0.0, border_thickness * 0.2, edge_dist);
        border = mix(border, border * edge_intensity, edge_boost);

        // Clamp to prevent over-brightness
        border = clamp(border, 0.0, 1.0);
    }

    // Apply warning color with border mask and flash intensity
    COLOR = vec4(warning_color, COLOR.a * border * flash_intensity);
}