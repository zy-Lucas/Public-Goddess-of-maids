#version 150

uniform sampler2D DiffuseSampler;
/** 这一下多狠，零到一 */
uniform float Strength;
/**
 * 这一帧的随机种子。每游戏拍换一次（五十毫秒）—— 错位量一跳一个样才像"信号出岔"；
 * 每帧都换会变成满屏噪点，顺着一个方向匀速挪那是"画面歪了"
 */
uniform float Seed;
/**
 * 它这会儿在屏幕上的位置，以及"它身子那一块"在屏幕上的半径。
 * <p>
 * ⚠️ 这一道**只落在它周围那一圈**，不铺满屏幕：撕的是"它待着的那一处"，
 * 不是把玩家的画面整幅掀了。中心与半径由实体那一头每帧算好传进来。
 */
uniform vec2 Center;
uniform float Radius;
/** 宽高比。带上它，那个圈在屏幕上才是正圆，不是椭圆 */
uniform float Aspect;

in vec2 texCoord;
out vec4 fragColor;

/** 那几条错位的带最多横着错开多少（UV，屏宽的比例）*/
const float TEAR = 0.22;
/** 竖着切成几条带。切得越细，那一圈里的断口越密 */
const float BANDS = 26.0;
/**
 * 多少条带真的错位。
 * <p>
 * 全都错看着是"整幅画面歪了"，隔几条错一条才像被撕开 —— 错位的带与不错的带之间
 * 那道台阶，就是这个效果本身。
 */
const float HIT = 0.65;
/**
 * 错位带里头再带一点斜切（扭曲）。
 * <p>
 * 只横着平移的话，断口两侧是"硬换了一条"；让带内越靠上下沿越歪一点、上下也跟着错一点，
 * 那一段就成了"被扯开的" —— 这才像撕裂，不只是错位。
 */
const float TWIST = 0.18;
/**
 * 带内横向伸缩：带里那一段画面被横向拉长（或压扁）最多这么多。
 * <p>
 * ⚠️ 这一层才是"**形变**" —— 只有平移（哪怕和平移一起加了斜切），看着仍然是"整块挪了位置"；
 * 画面被真拉长/压扁了，才叫扭曲。
 */
const float STRETCH = 0.30;
/**
 * 错位带上再叠一层横向拖影（"刷"地横着拉过去的那种糊）。
 * <p>
 * 只有错位的话，看着是"画面错了几格"；补上这一层才像"这一块被横向拽了一下"。
 */
const float SMEAR = 0.035;
/** 拖影分几次取。取奇数，中间那一次正好落在原处 */
const int SMEAR_TAPS = 7;
/** 撕口那道亮线占一条带的几分之几（UV）*/
const float SEAM = 0.0018;
/** 撕口亮多少。这是"撕开"那个记号，亮一点才认得出 */
const float SEAM_LIGHT = 0.7;
/** 断口处的红蓝分离量（UV）。有一点才"电子"，多了就糊 */
const float CHROMA = 0.005;
/** 采样点贴边的余量，防采到框外 */
const vec2 CLAMP_UV = vec2(0.001);

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main() {
    vec2 uv = texCoord;
    float s = clamp(Strength, 0.0, 1.0);
    if (s <= 0.002) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    // 只在"它所在的那一块"上撕：离中心越远越轻，出了圈一点都不动。
    // ⚠️ **线性收**（不平方）：平方的话圈里过半的地方都只剩两三成劲，
    // 看着就是"只有正中心撕了一下"，整片都像没动静
    float radial = length((uv - Center) * vec2(Aspect, 1.0));
    float reach = max(Radius, 1.0E-4);
    float near = max(0.0, 1.0 - radial / reach);
    if (near <= 0.002) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }
    float amount = s * near;

    float line = uv.y * BANDS;
    float band = floor(line);
    // 这一条带错不错、错多少，按"带号 + 这一帧的种子"摇出来：同一个带号在同一帧上
    // 算出来一样，所以它不必过网
    float live = step(1.0 - HIT, hash(band * 3.71 + Seed * 11.3));
    float shift = hash(band * 7.13 + Seed * 31.7) * 2.0 - 1.0;
    // 带内位置：零在带中间、一到上下沿。越靠沿扯得越歪 —— 断口两侧是"被扯开的"，
    // 不是硬换一条（全都只做平移的话，看着是"画面错了几格"，不像撕）
    float inBand = abs(fract(line) - 0.5) * 2.0;
    float skew = inBand * inBand;

    uv.x += shift * TEAR * live * amount;
    uv.x += shift * TWIST * skew * live * amount;
    // 上下也带一点：断口不是横平竖直的一刀，是斜着撕开的
    uv.y += shift * TWIST * 0.5 * skew * live * amount;
    // 带内横向伸缩：以它为中心把采样点收一收/放一放 —— 带里那一段被拉长或压扁。
    // 只有平移 + 斜切，看着还是"整块挪了位置"；拉长/压扁了才像"被扯变形了"
    float stretch = 1.0 + shift * STRETCH * skew * live * amount;
    uv.x = (uv.x - Center.x) / max(stretch, 0.25) + Center.x;

    // 撕口：那几条错位的带，上下沿补一道细亮线。光有色块错位看着是"画糊了"，
    // 有这道缝才看得出是"撕开的"
    float edge = min(fract(line), 1.0 - fract(line));
    float seam = max(0.0, 1.0 - edge / SEAM) * live * amount;

    // 横向拖影：那几条错位的带，沿 x 多取几次平均。只错位不糊，看着是"画面错了几格"；
    // 补上这一层才是"刷地横着拉过去"
    float smear = SMEAR * live * amount;
    vec3 color;
    if (smear > 0.0005) {
        color = vec3(0.0);
        for (int i = 0; i < SMEAR_TAPS; i++) {
            float t = (float(i) / float(SMEAR_TAPS - 1) - 0.5) * smear;
            color += texture(DiffuseSampler,
                    clamp(uv + vec2(t, 0.0), CLAMP_UV, 1.0 - CLAMP_UV)).rgb;
        }
        color /= float(SMEAR_TAPS);
    } else {
        color = texture(DiffuseSampler, clamp(uv, CLAMP_UV, 1.0 - CLAMP_UV)).rgb;
    }
    // 断口边上那点红蓝分离
    float split = CHROMA * live * amount;
    if (split > 0.0) {
        color.r = texture(DiffuseSampler,
                clamp(uv + vec2(split, 0.0), CLAMP_UV, 1.0 - CLAMP_UV)).r;
        color.b = texture(DiffuseSampler,
                clamp(uv - vec2(split, 0.0), CLAMP_UV, 1.0 - CLAMP_UV)).b;
    }
    color += vec3(SEAM_LIGHT) * seam;
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
