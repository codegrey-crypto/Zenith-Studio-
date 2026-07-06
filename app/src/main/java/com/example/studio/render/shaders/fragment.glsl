#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uTexture;
uniform vec3 uLightDir;
uniform float uIntensity;
uniform float uShadingFade;

out vec4 fragColor;

void main() {
    vec4 color = texture(uTexture, vTexCoord);
    if (color.a < 0.05) {
        discard;
    }

    // Normal approximation for the extrude face
    vec3 normal = vec3(0.0, 0.0, 1.0);
    float light = max(dot(normal, normalize(uLightDir)), 0.0);

    float shade = 1.0 + (light * uIntensity * 0.5);
    color.rgb *= shade * uShadingFade;

    fragColor = color;
}
