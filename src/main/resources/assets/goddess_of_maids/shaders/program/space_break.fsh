#version 150

/**
 * 空间破碎：世界里悬着一个圆盘，圆盘之内看出去是一片<b>真正在远处</b>的星空。
 * <p>
 * 形状交给几何：每个像素还原成世界射线，与盘面求交，交点落在盘内才算洞。
 * <p>
 * 星空<b>直接按视线方向采</b> —— 这是"远处的东西"唯一正确的写法：转头它跟着视线一一对应地转
 * （就像抬头看真星空），走两步则几乎不动（无限远没有视差）。钉在盘面上采会立刻变成一张贴图，
 * 这点反复试过了。
 * <p>
 * 单面也在这一层判：射线与法线的点积一旦不为负，就说明这道裂痕已经背过去了，原样返回。
 * 放在像素级比在 Java 里每帧算一次更跟手，也不会有"慢半拍"。
 */
uniform sampler2D DiffuseSampler;
/** 场景深度。有它，洞才会被前面的方块挡住 */
uniform sampler2D DepthSampler;
uniform float Near;
uniform float Far;
/** 屏幕宽高比。收缩遮罩按它算，才是个正圆而不是椭圆 */
uniform float Aspect;
/** 砖一共铺几遍（屏幕短边） */
uniform float Tiles;
/** 收缩遮罩的半径：一 = 整屏，零 = 收没了 */
uniform float Shrink;
/** 这一下走到哪一步，零到一 */
uniform float Progress;
/** 秒，星星靠它闪 */
uniform float Time;
/** 相机基向量（当前帧）：把屏幕像素还原成世界方向 */
uniform vec3 CamLook;
uniform vec3 CamRight;
uniform vec3 CamUp;
uniform float TanX;
uniform float TanY;
/** 相机位置、盘心、盘的法线，都是世界坐标 */
uniform vec3 CamPos;
uniform vec3 CrackPos;
uniform vec3 CrackNormal;
/** 盘的半径，世界单位。屏幕上多大由透视自己算 */
uniform float WorldRadius;
/** 一 = 传送门（洞内是苔石砖），零 = 空间破碎（洞内是星空） */
uniform float Portal;
/** 靠近程度：零远、一贴脸。门洞随它往外扩，到一就整屏都是砖 */
uniform float Open;
/**
 * 定格：一就整屏只画砖，不再求交。
 * <p>
 * 传送那一瞬间玩家的位置已经换了，门口那圈几何也就跟着失效 —— 这时候还按几何算，
 * 画面会闪一下原场景。传送期间用这个把砖钉死在屏幕上，等新地方加载完再放开。
 */
uniform float Freeze;

in vec2 texCoord;
out vec4 fragColor;

/** 门沿那一圈光。张开时是它，收缩时也是它 —— 只写一份，两处才不会漂 */
const vec3 RIM_COLOR = vec3(1.0, 0.10, 0.80);

const vec2 CLAMP_UV = vec2(0.0015);

vec2 hash2d(vec2 p) {
    return fract(sin(vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)))) * 43758.5453);
}

vec3 hash3(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
             dot(p, vec3(269.5, 183.3, 246.1)),
             dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(p) * 43758.5453);
}

/**
 * 一层星，按世界方向撒。
 * <p>
 * 星在格子里的位置只抖动 ±0.24（不到格宽的一半），这样跨格子的时候"换星"的位置差很小，
 * 不会出现一颗星突然跳走或凭空冒出来。核心收到很小、光晕也只有淡淡一圈 —— 糊多半是光晕铺太开。
 */
float starLayer(vec3 dir, float scale, float t, float seed, float threshold, float radius) {
    vec3 p = dir * scale;
    vec3 cell = floor(p);
    vec3 h = hash3(cell + seed);
    if (h.x < threshold) {
        return 0.0;
    }
    vec3 center = cell + 0.5 + (h - 0.5) * 0.48;
    float d = length(p - center);
    float core = smoothstep(radius, 0.0, d);
    float glow = smoothstep(radius * 1.30, 0.0, d) * 0.055;

    // 闪烁只到七成，永远留一点底光：真正的星是忽明忽暗，不是开关
    // 频率给得很低：真正的星是"慢慢呼吸"，一秒里变好几次看着就像在闪灯
    float rate = 0.35 + h.y * 1.1;
    float phase = h.z * 40.0;
    float breathe = 0.5 + 0.5 * sin(t * rate + phase);
    // 底光留四成，且用平方压一下曲线：亮暗两头的变化都缓
    float twinkle = mix(0.42, 1.0, breathe * breathe);
    return (core + glow) * twinkle;
}

/**
 * 苔石砖。
 * <p>
 * 原版 {@code PostChain} 只认内置的几个采样器（DiffuseSampler / DepthSampler / NoiseSampler），
 * 加自定义的会直接抛 {@code Invalid shaders/program/...}，所以拿不到原版那张贴图，
 * 只能照着它的样子画：错缝砌、灰砖配深浅、苔藓压在砖缝边与砖面上。
 */
vec3 brickColor(vec2 uv) {
    vec2 p = uv * Tiles;
    float row = floor(p.y);
    // 奇数行错半块
    p.x += mod(row, 2.0) * 0.5;
    vec2 cell = floor(p);
    vec2 f = fract(p);

    // 缝：横竖各一道，压得比砖面暗
    float mx = smoothstep(0.0, 0.03, f.x) * smoothstep(0.0, 0.03, 1.0 - f.x);
    float my = smoothstep(0.0, 0.03, f.y) * smoothstep(0.0, 0.03, 1.0 - f.y);
    float seam = mx * my;

    // 砖面：灰，块与块之间深浅不同
    vec2 h = hash2d(cell);
    vec3 stone = mix(vec3(0.36, 0.36, 0.35), vec3(0.47, 0.47, 0.45), h.x);
    // 细噪点：原版那张砖面上有一层碎石子
    stone += (hash2d(cell * 7.31 + f * 3.0).x - 0.5) * 0.07;

    // 苔藓：约四成砖长，颜色压在缝边上，像从缝里长出来
    float mossMask = step(0.58, h.y);
    float edge = 1.0 - seam;
    vec3 moss = mix(vec3(0.20, 0.30, 0.14), vec3(0.30, 0.42, 0.20), hash2d(cell * 3.7).x);
    stone = mix(stone, moss, mossMask * (0.55 + 0.45 * edge));

    return stone * mix(0.55, 1.0, seam);
}

/** 远处那片天：极暗的底子 + 从大到小五层星 */
vec3 voidColor(vec3 dir, float t) {
    // 底色几乎全黑，只在靠下那侧带一点冷紫，像是银河的边缘
    float up = dir.y * 0.5 + 0.5;
    vec3 base = mix(vec3(0.004, 0.0, 0.007), vec3(0.048, 0.010, 0.082), up * up * up);

    float s = 0.0;
    // 极少数几颗大的，带一圈很淡的辉
    s += starLayer(dir, 120.0, t, 91.0, 0.9955, 0.36) * 1.55;
    // 中号：主体
    s += starLayer(dir, 300.0, t, 0.0, 0.964, 0.115) * 1.10;
    s += starLayer(dir, 520.0, t, 17.0, 0.971, 0.085) * 0.85;
    // 细碎：铺满，密度最高
    s += starLayer(dir, 880.0, t, 41.0, 0.975, 0.060) * 0.60;
    s += starLayer(dir, 1500.0, t, 73.0, 0.980, 0.048) * 0.38;
    return base + vec3(1.0, 0.96, 1.0) * s;
}

// ---- 裂口轮廓：九边形，直边 ----
// 半径随角度变化这种东西，在极坐标里永远画成弧，频率再低也只是大弧。
// 要直边就得老老实实按线段求交：把九个顶点摆成多边形，像素方向打过去，
// 看它先碰到哪条边。
const int EDGE_N = 7;

/** 深度缓冲是非线性的，先换回"离相机多远" */
float linearDepth(float d) {
    return (2.0 * Near * Far) / (Far + Near - d * (Far - Near));
}

float vertexRadius(int i) {
    // 固定的伪随机：形状稳定，不会一帧一换
    float f = float(i) + 1.0;
    return 0.58 + 0.42 * fract(sin(f * 12.9898) * 43758.5453);
}

vec2 vertexAt(int i) {
    float a = 6.2831853 * float(i) / float(EDGE_N);
    return vec2(cos(a), sin(a)) * vertexRadius(i);
}

float polyRadius(vec2 dir) {
    float best = 1.0e9;
    for (int i = 0; i < EDGE_N; i++) {
        vec2 p0 = vertexAt(i);
        vec2 p1 = vertexAt(i == EDGE_N - 1 ? 0 : i + 1);
        vec2 e = p1 - p0;
        float den = dir.x * e.y - dir.y * e.x;
        if (abs(den) > 1.0e-6) {
            float t = (p0.x * dir.y - p0.y * dir.x) / den;
            float s = (p0.x * e.y - p0.y * e.x) / den;
            if (t >= 0.0 && t <= 1.0 && s > 0.0 && s < best) {
                best = s;
            }
        }
    }
    return best > 1.0e8 ? 1.0 : best;
}

void main() {
    vec2 uv = texCoord;
    vec3 scene = texture(DiffuseSampler, clamp(uv, CLAMP_UV, 1.0 - CLAMP_UV)).rgb;
    float t = clamp(Progress, 0.0, 1.0);
    if (t <= 0.002) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    // 定格：整屏就是那面砖，不需要圆心也不需要求交。
    // 收缩也走这一支 —— 屏幕上画一个圆，圆内是砖、圆外是原画面，
    // 半径由 Shrink 从一退到零，于是"砖往中间收没了"。纯屏幕空间，不碰世界几何
    if (Freeze > 0.5) {
        vec3 brick = brickColor(uv);
        vec2 local = (uv - 0.5) * vec2(Aspect, 1.0);
        // 半径按"中心到四角"归一：Shrink 是一的时候四个角也要盖得住。
        // 直接拿一当半径的话，四角差那么一点，会露出一圈原画面
        float corner = length(vec2(Aspect * 0.5, 0.5)) * 1.06;
        float r = Shrink * corner;
        float d = length(local);
        float mask = 1.0 - smoothstep(r - 0.03, r + 0.03, d);
        vec3 col = mix(scene, brick, mask);

        // 门沿那一圈紫光。张开时它在盘边上，收尾时得跟着半径一起往里走 ——
        // 这一支不碰世界几何，所以圈只能在屏幕上按半径画
        float pulse = 0.82 + 0.18 * sin(Time * 3.4);
        float outer = exp(-max(0.0, d - r) * 26.0) * (1.0 - mask);
        // 砖这一侧再压一点辉，边缘才有"玻璃边在发光"的厚度
        float inner = smoothstep(r - 0.10, r, d) * mask;
        // 收到最后一点就淡掉：不然中心会留一个亮点，然后突然没了
        float alive = smoothstep(0.0, 0.10, Shrink);
        col += RIM_COLOR * (outer * 0.95 + inner * 0.32) * pulse * alive;

        fragColor = vec4(col, 1.0);
        return;
    }

    // 这个像素看出去的世界射线
    vec2 ndc = (uv - 0.5) * 2.0;
    vec3 dir = normalize(CamLook + CamRight * (ndc.x * TanX) + CamUp * (ndc.y * TanY));

    // 单面：射线与法线的点积必须为负（看着盘的正面）。齐平和背过去都原样返回。
    // 逐像素判，和画面同一帧，不会有"慢半拍"
    float facing = dot(dir, CrackNormal);
    if (facing >= -1.0E-4) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    // 射线与盘面求交
    float tHit = dot(CrackPos - CamPos, CrackNormal) / facing;
    if (tHit <= 0.0) {
        fragColor = vec4(scene, 1.0);
        return;
    }
    vec3 hit = CamPos + dir * tHit;
    vec3 rel = hit - CrackPos;
    float radial = length(rel);

    // 前四成张开，最后一段收回去 —— 这段收尾是给"空间破碎"那个洞用的，
    // 传送门的收缩靠 Open 往回走，不看 t
    // 一出现就是满的：裂缝不是气球，不需要"撑开"的过程。
    // 只在最后那一段收回去
    float grow = 1.0 - smoothstep(0.82, 1.0, t);
    // 盖住整个屏幕四角所需的盘半径。屏幕角上的像素偏离视线中心最多，
    // 正对时那个偏离量约等于"到盘的距离 × tanY × √(Aspect²+1)"
    float fullR = max(0.05, length(CrackPos - CamPos) * TanY * sqrt(Aspect * Aspect + 1.0));
    // Open 直接就是"盖住了多少"：零是起始大小，一是四角都盖满。
    // 用 mix 而不是乘一个倍数，满屏这个点才有确定的几何含义 ——
    // 站在多近算满、就站在多近传送，两边才对得上
    float radius = mix(WorldRadius * grow, fullR * 1.05, clamp(Open, 0.0, 1.0));
    // 沿圆周切一圈尖齿。用三角波而不是正弦：正弦出来是花瓣边，裂口要的是硬折角。
    // 三层不同频率叠在一起，齿距才不整齐 —— 整齐的齿看着像齿轮
    vec3 axis = normalize(CrackNormal);
    vec3 tax = normalize(cross(axis, abs(axis.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
    vec3 tay = cross(axis, tax);
    float ang = atan(dot(rel, tay), dot(rel, tax)) / 6.2831853;
    // 裂口按九边形的直边算，不是圆周上的波动
    //
    // ⚠️⚠️ 5.7.2：这个 shader 被**两种用法**共用，形状必须跟着分开：
    //   · Portal = 0 —— 守护兽处决挑起：地面裂口，要**不规则**（九边形随机顶点，就是 polyRadius）
    //   · Portal = 1 —— 遗迹之眼开的传送门：要**正圆**，不该带碎口的形状
    // 原来这里无条件乘 polyRadius，所以传送门也是不规则的多边形。
    // （Portal 之前只拿去 mix 背景色，从没参与形状。）
    radius *= mix(polyRadius(vec2(dot(rel, tax), dot(rel, tay))), 1.0, clamp(Portal, 0.0, 1.0));
    // 边缘软过渡按世界单位算，远看近看都顺
    // 洞壁在这一像素上有多远：拿交点到相机的距离比一遍。
    // 场景里那个像素离得更近，说明前面有东西挡着，这一块就不该画洞
    float viewDist = dot(hit - CamPos, normalize(CamLook));
    float sceneDist = linearDepth(texture(DepthSampler, texCoord).r);
    float blocked = 1.0 - step(viewDist - 0.06, sceneDist);
    float inHole = (1.0 - smoothstep(radius - 0.015, radius + 0.015, radial)) * (1.0 - blocked);

    // 星空直接用视线方向 —— 远处的东西只由方向决定，位置挪动不该改变它
    vec3 sky = voidColor(dir, Time);
    // 传送门用砖；砖按屏幕铺，人越靠近砖越大，铺满那一刻正好交接传送
    vec3 brick = brickColor(uv);
    vec3 bg = mix(sky, brick, clamp(Portal, 0.0, 1.0));
    vec3 col = mix(scene, bg, inHole);

    // 盘沿一圈品红（和收尾那圈同一个颜色）
    float rim = exp(-max(0.0, radial - radius) * 4.0) * (1.0 - inHole);
    col += RIM_COLOR * rim * 0.95;

    // 每个折角往外拉一道裂纹。只有一圈折线的话，地上就是一块多边形口子，
    // 跟飞出来的碎片对不上 —— 这些细纹才是"这一下把地面撕开了"的证据
    float vAng = fract(ang * float(EDGE_N) + 0.5) - 0.5;
    float crack = 1.0 - smoothstep(0.008, 0.020, abs(vAng));
    float span = smoothstep(radius * 0.92, radius * 1.02, radial)
               * (1.0 - smoothstep(radius * 1.45, radius * 1.75, radial));
    col += RIM_COLOR * crack * span * 1.15;

    fragColor = vec4(col, 1.0);
}
