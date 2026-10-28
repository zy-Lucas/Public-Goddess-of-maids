#version 150

uniform sampler2D DiffuseSampler;
uniform float FlashStrength;
uniform float FlashWhite;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    // push the contrast while desaturating: flat gray reads as nothing happened
    gray = clamp((gray - 0.5) * 1.35 + 0.5, 0.0, 1.0);
    vec3 shaded = mix(color.rgb, vec3(gray), FlashStrength);
    fragColor = vec4(mix(shaded, vec3(1.0), FlashWhite), color.a);
}
