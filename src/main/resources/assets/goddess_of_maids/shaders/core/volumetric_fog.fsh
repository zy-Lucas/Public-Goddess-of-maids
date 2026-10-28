#version 150

/*
 * 体积雾 · 几何代理球 + 片元 raymarch
 * =========================================================================
 * 5.7.30: 球心通过顶点法线属性传进来（Normal 复用：法线三分量存球心坐标）。
 * 这样整批球共用一套 uniform，可以走 RenderType + MultiBufferSource
 * —— 那条路 5.7.27 已经被证实"能画出来"。
 * 此前用手动 Tesselator + setShader 一直静默失败，不再用那条路。
 */

uniform vec3 CameraPos;
uniform vec3 FogCenter;
uniform float FogRadius;
uniform float FogDensity;
uniform vec3 AmbientColor;
uniform vec3 LightColor;
uniform float NoiseScale;
uniform float Time;
uniform float SolidMode;

in vec4 vColor;
in vec3 vWorld;


out vec4 fragColor;

const int STEPS = 20;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

/** 四层 fbm（照抄 GetCloudNoise：频率 x3、权重 x0.5、最后 pow2） */
float fbm4(vec3 p) {
    float noise = 0.0;
    float persist = 1.0;
    float total = 0.0;
    for (int i = 0; i < 4; i++) {
        noise += valueNoise(p) * persist;
        total += persist;
        p *= 3.0;
        persist *= 0.5;
    }
    return pow(noise / total, 2.0);
}

void main() {
    if (SolidMode > 0.5) {
        fragColor = vec4(1.0, 0.0, 0.0, 1.0);
        return;
    }

    vec3 relCam = vWorld - CameraPos;
    float hitDist = length(relCam);
    vec3 dir = hitDist > 1.0E-5 ? relCam / hitDist : vec3(0.0, 0.0, 1.0);

    vec3 oc = CameraPos - FogCenter;
    float b = dot(oc, dir);
    float c = dot(oc, oc) - FogRadius * FogRadius;
    float h = b * b - c;
    if (h <= 0.0) {
        discard;
    }
    h = sqrt(h);
    // 5.7.32（日志 balls=1 shader=ok 但看不见）：
    // 原来 t1 = min(-b + h, hitDist)。**球正面的片元 hitDist 正好等于 t0**，
    // 于是 t1 <= t0 恒成立 ⇒ 正面片元全部 discard；背面又受深度/绕序影响 ⇒ 整体看不见。
    // 现在：积分区间取"射线与球的完整交集"（t0 → 出射点），
    // 配合 Java 侧的"只画背面"（cull front），每像素只画一次，不会双倍叠加。
    float t0 = max(-b - h, 0.0);
    float t1 = -b + h;
    if (t1 <= t0) {
        discard;
    }

    float stepLen = (t1 - t0) / float(STEPS);
    float dither = hash13(vec3(gl_FragCoord.xy, mod(Time, 600.0) * 37.0));
    vec3 pos = CameraPos + dir * (t0 + stepLen * dither);

    vec3 accum = vec3(0.0);
    float trans = 1.0;

    for (int i = 0; i < STEPS; i++) {
        vec3 rel = pos - FogCenter;
        float dist = length(rel) / FogRadius;
        float shape = 1.0 - smoothstep(0.45, 1.0, dist);
        if (shape > 0.0) {
            float n = fbm4(rel * NoiseScale + vec3(0.0, Time * 0.05, Time * 0.03));
            float dens = (n * 3.4 - 0.42) * shape * FogDensity;
            if (dens > 0.0001) {
                float hNorm = clamp(rel.y / FogRadius * 0.5 + 0.5, 0.0, 1.0);
                float shade = 0.30 + 0.70 * hNorm;
                vec3 col = AmbientColor * (0.40 + 0.60 * shade) + LightColor * shade;
                float ext = exp(-dens * stepLen * 4.5);
                accum += col * (trans * (1.0 - ext));
                trans *= ext;
                if (trans < 0.01) {
                    break;
                }
            }
        }
        pos += dir * stepLen;
    }

    float alpha = 1.0 - trans;
    if (alpha < 0.004) {
        discard;
    }
    fragColor = vec4(accum / max(alpha, 0.0001), alpha);
}
