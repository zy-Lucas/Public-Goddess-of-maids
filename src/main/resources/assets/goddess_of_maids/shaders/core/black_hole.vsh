#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;
out vec3 viewPos;
out vec3 normal;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPosition;

    texCoord0 = UV0;
    vertexColor = Color;
    viewPos = viewPosition.xyz;
    normal = normalize((ModelViewMat * vec4(Normal, 0.0)).xyz);
}
