#version 150

/*
 * 体积雾 · 完整 raymarch（复刻 Complementary 5.9 的 Unbound 体积云）
 * ===========================================================================
 * 小夕："这个云真的就像一个真正的实体一样存在在世界里，这就是我预期中的雾"
 *       "抄你给我抄完整一点啊" / "完全不一样…你这一次认真点，尽量一次复刻出这个云的效果"
 *
 * 这次照着 unboundClouds.glsl 抄全，不再用几何近似。抄的是这四段：
 *
 * ① 噪声（L133~158）——4 个 octave 的 fbm：
 *      for (i<4) { noise += Noise3D(pos) * persist; pos *= 3.0; persist *= 0.5; }
 *      noise = pow2(noise / total);
 * ② 密度（L183~197）——"噪声减阈值"，阈值随高度变化：
 *      noise *= noiseMult * CLOUD_UNBOUND_AMOUNT;
 *      threshold = pow2(pow2(pow2(clamp(|alt - y| / stretch, 0.001, 0.999))));  // 8 次方
 *      return noise - (threshold * 0.2 + 0.25);
 * ③ 步进（L260~285）——固定步长 + dither 抖起点 + 超过场景深度就停：
 *      sampleCount = int(planeDistanceDif / stepMult + dither + 1);
 *      tracePos += traceAdd;
 *      if (lTracePos > lViewPosM) continue;
 * ④ 光照（L390 / L467）——云内高度决定明暗，再叠环境色与阳光色：
 *      cloudShading = (higherPlaneAltitude - cloudSampleY) / cloudTallness;
 *      colorSample = cloudAmbientColor * (0.4 + 0.6*cloudShading) + cloudLightColor * cloudShading;
 *
 * 额外加了"向太阳走几步算透光"（自阴影）—— 云有立体感的另一半，
 * 光影里由 shadowmap 提供，我们没有 shadowmap，就用密度场自己算。
 */

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;

/** 相机基向量与位置 */
uniform vec3 CamLook;
uniform vec3 CamRight;
uniform vec3 CamUp;
uniform vec3 CamPos;
uniform float TanX;
uniform float TanY;
uniform float Near;
uniform float Far;

/** 秒（用于噪声流动） */
uniform float Time;

/** 雾团：xyz = 球心，w = 半径 */
uniform vec4 FogA;
uniform vec4 FogB;
uniform vec4 FogC;
uniform vec4 FogD;
/** 四团的浓度 */
uniform vec4 FogDens;

/** 环境色（天空色）与阳光色 */
uniform vec3 AmbientColor;
uniform vec3 LightColor;
/** 太阳方向（世界空间，已归一化） */
uniform vec3 SunDir;
/** 噪声频率（世界单位倒数）。越小 => 团越大 */
uniform float NoiseScale;
/** 密度总倍率 */
uniform float DensMult;

in vec2 texCoord;
out vec4 fragColor;

/** 主步进次数 */
const int STEPS = 18;
/** 自阴影步进次数 */
const int LIGHT_STEPS = 3;
/** 自阴影每步前进多少格 */
const float LIGHT_STEP = 0.55;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

/** 三线性 value noise，0~1 */
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

/**
 * 四层 fbm —— 照抄 GetCloudNoise 的循环：
 * 每层把 pos 乘 3（频率翻三倍）、persist 乘 0.5（贡献减半），最后 pow2。
 */
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

/** 一团雾在世界点 p 的密度（照抄 GetCloudNoise 的"噪声减阈值"结构） */
float ballDensity(vec3 p, vec4 ball, float dens) {
    if (dens <= 0.0 || ball.w <= 0.0) {
        return 0.0;
    }
    vec3 rel = p - ball.xyz;
    float d = length(rel) / ball.w;
    if (d > 1.15) {
        return 0.0;
    }
    // 球的内外：靠边淡出去（这是"范围"，不是"密度"）
    float shape = 1.0 - smoothstep(0.45, 1.05, d);
    if (shape <= 0.0) {
        return 0.0;
    }
    // 碎形：fbm 在球内流动
    float n = fbm4((p - ball.xyz) * NoiseScale + vec3(0.0, Time * 0.05, Time * 0.03));
    // 云的关键：pow2 之后减去一个常数阈值 —— 噪声"超过阈值"的地方才有雾，所以形状是碎的
    // 5.7.22: 阈值 0.62 -> 0.45。fbm 的 valueNoise 均值约 0.5，pow2 后约 0.25，
    // 0.25*2.6 = 0.65，刚过 0.62 -- 只有极少数像素有雾，看着就是"完全不可见"。
    float dens0 = n * 3.4 - 0.42;
    if (dens0 <= 0.0) {
        return 0.0;
    }
    return dens0 * shape * dens;
}

float densityAt(vec3 p) {
    return ballDensity(p, FogA, FogDens.x)
         + ballDensity(p, FogB, FogDens.y)
         + ballDensity(p, FogC, FogDens.z)
         + ballDensity(p, FogD, FogDens.w);
}

/** 非线性深度 → 世界距离（1.20.1 是 reversed-Z：1=近、0=远） */
float linearDepth(float d) {
    float z = 1.0 - d;
    return (2.0 * Near * Far) / (Far + Near - z * (Far - Near));
}

void main() {
    vec2 uv = texCoord;
    vec3 scene = texture(DiffuseSampler, clamp(uv, vec2(0.0015), vec2(0.9985))).rgb;

    // 这个像素看出去的世界方向
    vec2 ndc = (uv - 0.5) * 2.0;
    vec3 dir = normalize(CamLook + CamRight * (ndc.x * TanX) + CamUp * (ndc.y * TanY));

    // 场景深度：雾不能画到墙后面（云也是这么做的：lTracePos > lViewPosM 就停）
    float rawDepth = texture(DepthSampler, texCoord).r;
    float sceneDist = linearDepth(rawDepth);
    // 5.7.22: DepthSampler 至今没读通过。reversed-Z 下 rawDepth=1.0 表示"贴脸"，
    // linearDepth 会算出约 0.1 格 -- 限程被压成 10 厘米，雾一点都看不见（上一版就是这样）。
    // 所以：深度算出来小得不合理时，**当作读不到**，改用雾团自身范围。
    // 代价是暂时没有"被墙拦腰截断"，但先让雾能看见。
    if (sceneDist < 1.0) {
        sceneDist = 1.0e6;
    }

    // 5.7.21: 限程必须跟着雾团走。用 128 格做限程会让 18 步跨 7 格/步 ——
    // 既采不到雾（全是空的），又白烧 GPU。这里取"最远那团的边界 + 2 格"。
    float fogEnd = 0.0;
    if (FogDens.x > 0.0 && FogA.w > 0.0) fogEnd = max(fogEnd, distance(FogA.xyz, CamPos) + FogA.w);
    if (FogDens.y > 0.0 && FogB.w > 0.0) fogEnd = max(fogEnd, distance(FogB.xyz, CamPos) + FogB.w);
    if (FogDens.z > 0.0 && FogC.w > 0.0) fogEnd = max(fogEnd, distance(FogC.xyz, CamPos) + FogC.w);
    if (FogDens.w > 0.0 && FogD.w > 0.0) fogEnd = max(fogEnd, distance(FogD.xyz, CamPos) + FogD.w);
    if (fogEnd <= 0.01) {
        fragColor = vec4(scene, 1.0);
        return;
    }
    float maxEnd = min(sceneDist, fogEnd + 2.0);
    float start = max(0.05, Near);

    float stepLen = (maxEnd - start) / float(STEPS);
    if (stepLen <= 0.0005) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    // dither：抖起点，消掉固定步长留下的同心层（云：tracePos += traceAdd * dither）
    float dither = hash13(vec3(gl_FragCoord.xy, mod(Time, 600.0) * 37.0));
    // 从"最近那团的入口"开始步进，别从相机一路白走（省一大半步数）
    float fogStart = 1.0e9;
    if (FogDens.x > 0.0 && FogA.w > 0.0) fogStart = min(fogStart, distance(FogA.xyz, CamPos) - FogA.w);
    if (FogDens.y > 0.0 && FogB.w > 0.0) fogStart = min(fogStart, distance(FogB.xyz, CamPos) - FogB.w);
    if (FogDens.z > 0.0 && FogC.w > 0.0) fogStart = min(fogStart, distance(FogC.xyz, CamPos) - FogC.w);
    if (FogDens.w > 0.0 && FogD.w > 0.0) fogStart = min(fogStart, distance(FogD.xyz, CamPos) - FogD.w);
    if (fogStart > maxEnd) {
        fragColor = vec4(scene, 1.0);
        return;
    }
    float from = max(start, fogStart - 0.5);
    float stepLen2 = (maxEnd - from) / float(STEPS);
    if (stepLen2 <= 0.0005) {
        fragColor = vec4(scene, 1.0);
        return;
    }
    vec3 pos = CamPos + dir * (from + stepLen2 * dither);

    vec3 accum = vec3(0.0);
    float trans = 1.0;

    for (int i = 0; i < STEPS; i++) {
        float dens = densityAt(pos) * DensMult;
        if (dens > 0.0001) {
            // ── 光照：朝太阳再走几步，看有多少雾挡着 => 透光率（自阴影）──
            float shadow = 0.0;
            vec3 lp = pos;
            for (int j = 0; j < LIGHT_STEPS; j++) {
                lp += SunDir * LIGHT_STEP;
                shadow += densityAt(lp) * DensMult;
            }
            float lightTrans = exp(-shadow * LIGHT_STEP * 0.85);

            // 云内高度决定明暗：顶亮底暗（cloudShading）
            float hNorm = 0.0;
            float wsum = 0.0;
            if (FogDens.x > 0.0 && FogA.w > 0.0) {
                hNorm += clamp((pos.y - FogA.y) / (2.0 * FogA.w) + 0.5, 0.0, 1.0) * FogDens.x;
                wsum += FogDens.x;
            }
            if (FogDens.y > 0.0 && FogB.w > 0.0) {
                hNorm += clamp((pos.y - FogB.y) / (2.0 * FogB.w) + 0.5, 0.0, 1.0) * FogDens.y;
                wsum += FogDens.y;
            }
            if (FogDens.z > 0.0 && FogC.w > 0.0) {
                hNorm += clamp((pos.y - FogC.y) / (2.0 * FogC.w) + 0.5, 0.0, 1.0) * FogDens.z;
                wsum += FogDens.z;
            }
            if (FogDens.w > 0.0 && FogD.w > 0.0) {
                hNorm += clamp((pos.y - FogD.y) / (2.0 * FogD.w) + 0.5, 0.0, 1.0) * FogDens.w;
                wsum += FogDens.w;
            }
            float shade = wsum > 0.0 ? (hNorm / wsum) : 0.5;

            // 云的颜色公式：ambient*(0.4+0.6*shade) + light*shade
            vec3 sampleCol = (AmbientColor * (0.40 + 0.60 * shade) + LightColor * shade)
                           * (0.35 + 0.65 * lightTrans);

            // Beer-Lambert：这一步吃掉多少
            float ext = exp(-dens * stepLen2 * 4.5);
            accum += sampleCol * (trans * (1.0 - ext));
            trans *= ext;
            if (trans < 0.01) {
                break;
            }
        }
        pos += dir * stepLen2;
        if (length(pos - CamPos) > maxEnd) {
            break;
        }
    }

    fragColor = vec4(scene * trans + accum, 1.0);
}
