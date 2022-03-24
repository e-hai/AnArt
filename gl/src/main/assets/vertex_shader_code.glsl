attribute vec4 vPosition;
attribute vec2 inTexCoor;
varying vec2 linkTexCoor;

void main() {
    gl_Position = vPosition;
    linkTexCoor=inTexCoor.xy;
}