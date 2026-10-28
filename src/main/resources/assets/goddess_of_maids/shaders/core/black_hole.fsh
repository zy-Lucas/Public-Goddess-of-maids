#version 150

uniform sampler2D ScreenTexture;
uniform mat4 ProjMat;
uniform vec3 Center;
uniform float Intensity;
uniform float Radius;
uniform float Horizon;
uniform float Bend;
uniform float EdgeBand;

in vec2 texCoord0;
in vec4 vertexColor;
in vec3 viewPos;
in vec3 normal;

out vec4 fragColor;

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
        // 球心落到相机背后或者贴着相机，投出来的位置没有意义。
        // 这个门槛跟外面那份清单里剔的是同一个数，两边不一致时这里会被判成球贴在脸前
        fragColor = vec4(passthrough, 1.0);
        return;
    }

    // 这条视线到球心的垂距，写作球壳半径的倍数。0 是正对球心，1 是球壳边缘。
    // 球心由外面算好传进来，不在这里拿球面顶点反推：那种反推在三角形内部
    // 会偏一点，偏多少还随三角形走，画面上就浮出一圈周期性的花瓣
    float r = length(cross(ray, Center)) / shell;

    if (r >= 1.0) {
        // 球壳覆盖之外，原样交还
        fragColor = vec4(passthrough, 1.0);
        return;
    }

    // 视界之内光出不来。这里不渐变，边界清清楚楚是个圆
    if (r <= Horizon) {
        fragColor = vec4(mix(passthrough, vec3(0.0), Intensity), 1.0);
        return;
    }

    // 本像素与本球心在屏幕上的差，先掰成各向同性，横轴那点差乘宽高比
    vec2 delta = (uv - viewToScreenUV(Center)) * vec2(aspect, 1.0);
    float span = length(delta);
    if (span < 1.0E-6) {
        fragColor = vec4(passthrough, 1.0);
        return;
    }
    vec2 radial = delta / span;

    // 位移场。轮廓两头都归零：中心吸得最狠，往外先涨后落，到球壳边缘自己收回来。
    // 相机进不到球壳里面，Java 那边会把靠得太近的球心沿原方向推到镜头前面
    float push = Bend * span * (1.0 - r * r) / (r * r + 0.18);

    // 挨着球壳边缘那一段再收一层。不加这一层的话，扭曲到边缘是一刀切下去的，
    // 圆里面在弯、圆外面一动不动，接缝看着生硬。带宽按扭曲半径的比例走，由外面给。
    // 下限垫一点，免得填 0 时两个端点重合
    push *= 1.0 - smoothstep(min(1.0 - EdgeBand, 0.999), 1.0, r);

    // 采样点朝外挪。引力把光往里拉，落到眼里的这束光来向比看到的方向更偏外，
    // 反推光源就得往外走。中心那块像素因此采到更外圈的景，四周的东西看着被拽着往里陷。
    // 朝里挪会变成把外圈的景硬塞进中心，出来是一堆同心环，不是透镜
    vec2 shift = radial * push * vec2(1.0 / aspect, 1.0);
    vec3 color = fetch(uv + shift);
    // 红蓝各偏一点，边上散出彩边
    color.r = fetch(uv + shift * 1.07).r;
    color.b = fetch(uv + shift * 0.93).b;

    // 视界外那圈被拽亮的光，只铺到视界外面一点，铺满会把弯折盖住。
    // 不乘 Intensity：整块颜色最后统一跟原画面 mix 一次，这里再乘一道等于把出场的
    // 过程平方了，该撑满的时候实际才走到一半出头，跟那一声对不上
    float glow = 1.0 - smoothstep(Horizon, Horizon * 1.6, r);
    color += vec3(0.34, 0.12, 0.62) * glow * glow * 0.16;

    // 黑边往外一小段过渡。这段很窄，事件视界看上去就是这个圆本身的大小
    float edge = smoothstep(Horizon, Horizon * 1.03, r);
    color = mix(vec3(0.0), color, edge);

    fragColor = vec4(mix(passthrough, color, Intensity), 1.0);
}
