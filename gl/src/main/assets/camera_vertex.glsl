attribute vec4 aPosition;
attribute vec4 aTexCoor;
varying vec2 vTexCoordinate;
uniform mat4 uMVPMatrix;

void main () {
    vTexCoordinate = (uMVPMatrix * aTexCoor).xy;
    gl_Position = aPosition;
}