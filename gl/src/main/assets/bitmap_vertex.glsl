//顶点着色器输入变量由attribute来声明
attribute vec4 vPosition;

attribute vec2 vTexCoordinate;

//片段着色器输入变量用arying来声明
varying vec2 v_TexCoordinate;

void main () {
    v_TexCoordinate =vTexCoordinate;
    gl_Position = vPosition;
}