#version 150

/*
 * 星核的顶点着色器。
 * <p>
 * ⚠️ 5.8.0：这个文件**一直缺失** —— `star_core.json` 和 `star_core.fsh` 都在，
 * 只有 vsh 没有，于是每次启动都报：
 * `star core shader unavailable: Invalid shaders/core/star_core.json: File not found`
 * （那句 "File not found" 指的是 json 里引用的 **vertex** 找不到，不是 json 本身）。
 * 补上之后注册就能成功，将来要用也不会踩这个坑。
 * <p>
 * 输出与 `star_core.fsh` 的输入对应：`vertexColor` / `texCoord0`。
 */

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;
}
