#version 150

uniform sampler2D DiffuseSampler;
/** 这一下多狠，零到一。淡出就靠它一路收 */
uniform float Strength;
/**
 * 水波圈这会儿的圆心，屏幕 UV。
 * <p>
 * 这一层**只在屏幕上**，不摆任何世界里的几何 —— 落点投到屏幕上的那个位置就是圆心，
 * 落点在身后或者屏外时由 Java 那一头退回屏幕正中。
 */
uniform vec2 Center;
/** 波前这会儿在屏幕上的半径，以<b>屏幕高度</b>为单位（0.5 就是半个屏高） */
uniform float Ring;
/** 波带多宽，同上单位。给的是半径方向的宽度 */
uniform float Width;
/** 波带把采样点朝圆心收多少，同上单位。给得越大，那一圈上的画面被顶出去得越狠 */
uniform float Bend;
/**
 * 波带自带的一点明暗：外沿提亮、内沿压暗。
 * <p>
 * 只有位移的话，在均匀画面上（一片天空、一整面墙）**推了跟没推一样** —— 那一圈就看不见了。
 * 有了这一层，任何背景上都能看出这一圈过去。它随强度一起淡，所以收尾是"这一圈慢慢变淡"。
 * 给零就回到纯透明
 */
uniform float Rim;
/** 宽高比。带上它，这一圈在屏幕上才是正圆，不是椭圆 */
uniform float Aspect;

in vec2 texCoord;
out vec4 fragColor;

/** 采样点贴边的余量，防采到框外 */
const vec2 CLAMP_UV = vec2(0.0015);

void main() {
    vec2 uv = texCoord;
    float s = clamp(Strength, 0.0, 1.0);
    if (s <= 0.002) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    // 各向同性的距离：横轴乘宽高比，圈才是正圆。单位是屏幕高度，
    // 所以横过来那片屏幕上的"半个屏宽"比"半个屏高"更远 —— 这样一圈走完才是圆的
    vec2 d = (uv - Center) * vec2(Aspect, 1.0);
    float dist = length(d);
    float w = max(Width, 1.0E-4);
    float x = (dist - Ring) / w;
    // 波带：以波前为中心的一道软环，两侧都平滑收到零（不留硬边，免得看出"一条线"）
    float band = max(0.0, 1.0 - x * x);
    band = band * band * s;
    if (band <= 0.002) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    // 沿径向把采样点**朝圆心收**：这一圈上的画面因此被朝外顶出去，
    // 看着像那一层界面从屏幕里鼓出来、要冲到你眼前。波前一路往外走 ⇒ 这一鼓从圆心荡开去。
    // ⚠️ 位移不许超过"到圆心的距离"：波前刚出生那几帧它还很小，一条比半径还长的位移会把
    // 采样点整个拽到圆心另一侧 ⇒ 画面在中心翻过来。限到九成，够猛也不会翻
    vec2 dir = dist > 1.0E-6 ? d / dist : vec2(0.0);
    float push = min(Bend * band, dist * 0.9);
    vec2 shift = dir * (-push) * vec2(1.0 / Aspect, 1.0);
    vec3 color = texture(DiffuseSampler,
            clamp(uv + shift, CLAMP_UV, 1.0 - CLAMP_UV)).rgb;

    // 波带自带的明暗：外沿亮、内沿暗，让它在一整片同色的背景上也认得出
    float side = x > 0.0 ? 1.0 : -1.0;
    color = clamp(color + vec3(Rim * band * side), 0.0, 1.0);

    fragColor = vec4(color, 1.0);
}
