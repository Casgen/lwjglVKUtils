#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec2 a_Position;
layout (location = 1) in vec4 a_Color;

layout (location = 0) out vec4 outColor;

/*vec2 positions[3] = vec2[](
    vec2(0.0, -0.5),
    vec2(0.5, 0.5),
    vec2(-0.5, 0.5)
);

vec3 colors[3] = vec3[](
    vec3(1.0, 0.0, 0.0),
    vec3(0.0, 1.0, 0.0),
    vec3(0.0, 0.0, 1.0)
);*/

void main() {
    //gl_Position = vec4(positions[gl_VertexIndex], 0.0, 1.0);
    gl_Position = vec4(a_Position, 0.f, 1.f);
    outColor = a_Color;
}
