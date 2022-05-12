attribute vec4 aPosition;
attribute vec2 aTexCoor;
varying vec2 vTexCoordinate;

void main(){
    vTexCoordinate =aTexCoor;
    gl_Position = aPosition;
}