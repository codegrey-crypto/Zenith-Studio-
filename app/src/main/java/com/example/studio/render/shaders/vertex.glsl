#version 300 es
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTexCoord;

uniform float uDepth;
uniform mat4 uMVP;

out vec2 vTexCoord;

void main() {
    vec4 pos = aPosition;
    pos.z += uDepth;
    gl_Position = uMVP * pos;
    vTexCoord = aTexCoord;
}
