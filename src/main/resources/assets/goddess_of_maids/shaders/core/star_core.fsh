#version 150

uniform float Intensity;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // uv.x 横跨光束：0 和 1 在两边，0.5 是轴心。uv.y 是沿光束的实际距离（格）
    float axis = clamp(1.0 - abs(texCoord0.x - 0.5) * 2.0, 0.0, 1.0);
    vec3 base = vertexColor.rgb;

    // ⚠️ 5.6.109：这一份是**四角星专用**的，跟 slash_ray.fsh 的差别全在"收边"。
    // 原来共用 slash_ray 时，小夕看到星的四条臂外面糊着一圈半透明光晕 ——
    // 那是 alpha 走 pow(axis, 0.75)（衰减很慢，整条带子横向一大片都不透明），
    // 再加最外圈还有 base*0.13 的暗色底，于是臂外面挂着一层看得见的雾。
    // 星是"闪光"，该是几道光，不是几团雾：这里把衰减改成 2.4 次幂，
    // 并且**去掉 ember 外场**，两侧直接归零。
    vec3 blood = base * 0.86;
    vec3 white = mix(base, vec3(1.0, 0.88, 0.82), 0.92);

    vec3 color = mix(blood, white, pow(axis, 14.0));

    // 沿长度起伏一点：光柱才不像一根静止的塑料棒
    float flicker = 0.88 + 0.12 * sin(texCoord0.y * 5.0);

    // 2.4 次幂：只有靠近轴心的一条才亮，臂的边缘迅速归零 —— 光晕就没了
    float alpha = pow(axis, 2.4) * vertexColor.a * Intensity * flicker;
    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}