#version 150

uniform float Intensity;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // uv.x 横跨光束：0 和 1 在两边，0.5 是轴心。uv.y 是沿光束的实际距离（格）
    float axis = clamp(1.0 - abs(texCoord0.x - 0.5) * 2.0, 0.0, 1.0);
    vec3 base = vertexColor.rgb;

    // 三层叠出来：外场是压到近黑的红，往里烧成血红，最中心只剩一条极细的白热丝。
    // 白芯一旦铺宽整条光就发粉，所以它走 16 次方 —— 只有正轴心那一点点够得着
    vec3 ember = base * 0.13;
    vec3 blood = base * 0.78;
    vec3 white = mix(base, vec3(1.0, 0.86, 0.80), 0.90);

    vec3 color = mix(ember, blood, pow(axis, 1.5));
    color = mix(color, white, pow(axis, 16.0));

    // 沿长度起伏一点：光柱才不像一根静止的塑料棒
    float flicker = 0.84 + 0.16 * sin(texCoord0.y * 5.0);

    // 外场也得有分量 —— 压得住身后的画面才叫黑红，光靠亮芯只会发白
    float alpha = pow(axis, 0.75) * vertexColor.a * Intensity * flicker;
    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
