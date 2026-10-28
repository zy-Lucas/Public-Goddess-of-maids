#version 150

/*
 * 体积雾的代理球。
 * 5.7.30: Position 是**世界坐标**，用 ModelViewMat（由 Java 侧显式设好并 apply）投影；
 * 世界坐标另存一份传给片元，片元自己减 CameraPos 得到视线方向。
 * 球心由 uniform FogCenter 传（每球一次 endBatch，所以能逐球设）。
 */

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vColor;
out vec3 vWorld;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color;
    vWorld = Position;
}