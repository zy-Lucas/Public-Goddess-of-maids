#version 150

uniform sampler2D ScreenTexture;
uniform mat4 ProjMat;
uniform vec3 Center;
uniform float Intensity;
uniform float Radius;
uniform float Ring;
uniform float RingWidth;
uniform float Bend;
/**
 * 这一圈按哪种量法认环。
 * <p>
 * 1 = 世界里的水平距离（横向的一圈，只有 boss 房开场那一处在用）；
 * 0 = 视线到球心的垂距（屏幕上的正圆，往四面八方一起鼓，那是球形）。
 * 守护兽自爆与斩杀那两处走的是 0 这一档，跟它们一开始的样子一致。
 */
uniform float Flat;
/**
 * 内侧那条长尾要不要给。
 * <p>
 * <b>0</b>（默认，祭坛 / 自爆 / 斩杀那三处走这一档）= 给：内侧一路缓到中心，
 * 于是那个圆从圆心到壳边都在往外挪 —— 看着是"<b>一整个圆盘在鼓</b>"。
 * <p>
 * <b>1</b> = 不给：过了环就一路收干净，中间那一块纹丝不动 —— 只剩贴着壳边的那
 * **一圈环**，也就是"<b>甜甜圈</b>"。
 */
uniform float Hollow;

/** 空心那一档：这条带最多占壳半径的百分之几。带宽是按"格"折过来的，壳小的时候会大得离谱 */
const float HOLLOW_BAND_MAX = 0.20;
/** 空心那一档：收尾跨几个带宽。正常档用 OUT_SPAN（8），那一档太宽，环就没边了 */
const float HOLLOW_SPAN = 2.0;

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 viewPos;
in vec3 normal;

out vec4 fragColor;

/** 内侧那条长尾一直缓到中心，中心处剩下的那一点劲。给零就是"里面什么都不动" */
const float INNER_KEEP = 0.12;
/** 内侧长尾的尺度，单位是"带宽的倍数"。越大，往里衰减越慢、中间跟着动的那一片越大 */
const float TAIL_W = 12.0;
/**
 * 外侧收到零要跨多少个带宽。
 * <p>
 * 早先是在 3 倍带宽处一刀切，那道边贴在地形轮廓上时，看着就像"冲击波被地形切断了"
 * —— 其实只是它自己收得太急。这一层压根不测深度，方块挡不住它
 */
const float OUT_SPAN = 8.0;
/**
 * 那一圈自带的可见度：外沿提亮、内沿压暗，任何背景上都能看出"这里有一圈"。
 * <p>
 * 纯位移在均匀地表上是看不出来的（把一片均匀的地面推开，还是那片均匀的地面）——
 * 于是落到地面上那半圈就像被切掉了。要在哪个方向、哪种背景上都看得到一个完整的圆，
 * 就得有这么一层明暗。它随强度淡出，于是"淡出"是那一圈慢慢变淡，不是缩回去。
 * 给零就回到"纯透明"
 */
const float RIM_LIGHT = 0.14;
/**
 * 贴着地面那一圈额外横拧多狠。
 * <p>
 * 环扩到地面上的时候，光有"沿径向推开"看不出水波擦地那一下 —— 地面本来就是一整片，
 * 推开跟没推一样。所以贴地那一段另加一份**沿环走向**（切向）的拽，看着才像有东西贴着地滚过去。
 */
const float GROUND_BEND = 0.012;

vec2 viewToScreenUV(vec3 pos) {
    vec4 clip = ProjMat * vec4(pos, 1.0);
    float w = max(clip.w, 1.0E-4);
    return clamp(clip.xy / w * 0.5 + 0.5, vec2(0.001), vec2(0.999));
}

vec3 fetch(vec2 uv) {
    return texture(ScreenTexture, clamp(uv, vec2(0.0015), vec2(0.9985))).rgb;
}

void main() {
    // 横竖两个轴的投影比值就是宽高比，不用另传一个 uniform
    float aspect = abs(ProjMat[1][1]) / max(abs(ProjMat[0][0]), 1.0E-6);

    vec2 uv = viewToScreenUV(viewPos);
    vec3 passthrough = fetch(uv);

    if (Intensity <= 0.001) {
        fragColor = vec4(passthrough, 1.0);
        return;
    }

    vec3 ray = normalize(viewPos);
    float shell = max(Radius, 0.0005);
    if (Center.z > -0.05) {
        // 球心落到相机背后或者贴着相机，投出来的位置没有意义
        fragColor = vec4(passthrough, 1.0);
        return;
    }

    // 这一像素离中轴多远，写作环半径的倍数。0 是中轴，1 是环本身。两种量法：
    //   Flat = 1：世界里的水平距离 —— 那一圈钉在几何所在的那圈上，是横的、一格子厚，
    //     "扩张"由几何的半径往外长来表现（见几何那一段）。
    //     viewPos 与 Center 都是同一个空间（相机相对）的坐标，相减就是"离中轴多远"
    //   Flat = 0：视线到球心的垂距（屏幕上的角度）—— 那一圈以球心的屏幕位置为心，
    //     永远是屏幕上的一个正圆，往四面八方一起鼓 ⇒ 看着是球形。自爆与斩杀走这一档
    // 别拿 flat 当变量名：它在 GLSL 里是插值修饰符（关键字）
    float r;
    if (Flat > 0.5) {
        vec2 offset = vec2(viewPos.x - Center.x, viewPos.z - Center.z);
        r = length(offset) / shell;
    } else {
        r = length(cross(ray, Center)) / shell;
    }
    // ⚠️ 空心那一档的环铺在壳的最外沿（r = 1），照"出了壳就不管"在这里收手的话，
    // 环的外半侧整段被截掉 —— 只剩内侧半条，看着又细又小（他实测"连半格都不到"有一半
    // 是这个）。这一档的线放到环带外侧收尾之外，几何那头也跟着铺大（见 HOLLOW_SPREAD）
    float shellEdge = Hollow > 0.5 ? 1.0 + HOLLOW_BAND_MAX * HOLLOW_SPAN : 1.0;
    if (r >= shellEdge) {
        fragColor = vec4(passthrough, 1.0);
        return;
    }

    // 只有"环"那一圈最狠，两侧各自减下去。这一条跟透镜球不同：透镜是整颗球都在折光，
    // 看着像一颗玻璃球；这里是一圈透明的东西从落点推出去
    // 下限只用来防零：这一条带宽是按"格"折成球壳半径的倍数传进来的，壳大的时候它可以很小
    float band = max(RingWidth, 0.0002);
    // 空心那一档还得把这条带收细、收尾收紧：带宽按"格"折过来，壳只有半格的时候
    // 它会宽得比壳本身还大 —— 那样整颗壳都泡在"环附近"，环就看不出是个环了。
    // ⚠️ 这个局部名不能叫 span：下面还有一个 `float span = length(delta);`（屏幕距离），
    // 同名会把那一个整个遮掉
    float cut = OUT_SPAN;
    if (Hollow > 0.5) {
        band = min(band, HOLLOW_BAND_MAX);
        cut = HOLLOW_SPAN;
    }
    float d = (r - Ring) / band;
    // 外侧走到 cut 倍带宽那里本来就该是零了，正好收手
    if (d > cut) {
        fragColor = vec4(passthrough, 1.0);
        return;
    }
    // 环上最狠的那一层：芯子（d 走到 1 归零）加外面一段三倍宽的软坡当过渡。
    // 带宽给得小的时候只留芯子就成了一条硬缝，过渡这一段是必须的
    float k = max(0.0, 1.0 - d * d);
    float wide = max(0.0, 1.0 - d * d * 0.09);
    band = k * k * 0.8 + wide * wide * 0.2;
    if (d < 0.0) {
        if (Hollow > 0.5) {
            // 空心那一档：内侧不给那条长尾，过了环就一路收到零 ⇒ 中间那一块纹丝不动。
            // 环铺在 r = 1（壳的最外沿），收干净之后剩下的正好是贴着壳边的那一圈环
            float inner = max(0.0, 1.0 + d / cut);
            band = max(band, inner * inner);
        } else {
            // 内侧（朝球心那一侧）：一路缓到中心，中心也还留一点。
            // 早先这里跟外侧一样在 3 倍带宽外就归零 —— 环的里面是干的、边界还硬；
            // 现在整个中间都跟着动，只是越往中心越少（位移量里还乘着 span，中心那块几乎不动）
            float tail = INNER_KEEP + (1.0 - INNER_KEEP) / (1.0 + d * d / (TAIL_W * TAIL_W));
            band = max(band, tail);
        }
    } else {
        // 外侧（朝壳边那一侧）：一路平滑收到零，收尾那一下的斜率也是零
        // ⇒ 不再留下一条能被人看成"被切断"的边
        float fade = max(0.0, 1.0 - d / cut);
        band = max(band, fade * fade);
    }

    // 本像素与球心在屏幕上的差，先掰成各向同性，横轴那点差乘宽高比
    vec2 delta = (uv - viewToScreenUV(Center)) * vec2(aspect, 1.0);
    float span = length(delta);
    if (span < 1.0E-6) {
        fragColor = vec4(passthrough, 1.0);
        return;
    }
    vec2 radial = delta / span;

    // 位移。采样点朝外挪：环上那圈像素因此采到更外圈的景，顺着环走一圈，
    // 看到的就是空间被这一圈推歪了一下。
    // ★ 位移量与强度线性同步：**最后那四十来拍里，这一推一路慢慢减小到零**。
    //   不是"前期保持、末尾才收"（那样收缩全挤在最后，看着像突然收了一下），
    //   也不是"和原样对半混"（那出来的是同一处画面叠两份，看着是糊了不是淡了）
    float push = min(Bend * span * band, 0.30) * Intensity;
    vec2 shift = radial * push * vec2(1.0 / aspect, 1.0);

    // ★ 贴地那一下，只有横向那一档给：这一像素的竖直位置离环平面（也就是地面）越近，
    //   就额外横着拧得越多。横拧的方向取"沿环的走向"（垂直于径向）——
    //   看着像有一圈东西贴着地滚过去。只在这一圈本身上给，内侧那条长尾不掺和。
    //   球形那一档不加：它是往四面八方一起鼓的，"贴着地"对它没有意义
    if (Flat > 0.5) {
        float lift = abs(viewPos.y - Center.y) / max(shell, 0.001);
        float hug = 1.0 / (1.0 + lift * lift * 24.0);
        float onRing = max(1.0 - abs(d) * 0.5, 0.0);
        shift += vec2(-radial.y, radial.x) * (GROUND_BEND * hug * onRing * Intensity)
                * vec2(1.0 / aspect, 1.0);
    }
    vec3 color = fetch(uv + shift);

    // ★ 那一圈自带的明暗：外沿提亮、内沿压暗。
    //   纯位移在均匀地表上不可见，所以"哪个方向、哪种背景都要看到一个完整的圆"
    //   必须有这一层；它随强度淡出 ⇒ 淡出看起来是"那一圈慢慢变淡"
    float side = d > 0.0 ? 1.0 : -1.0;
    color = clamp(color + vec3(RIM_LIGHT * band * Intensity * side), 0.0, 1.0);

    fragColor = vec4(color, 1.0);
}
