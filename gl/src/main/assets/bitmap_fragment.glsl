#extension GL_OES_EGL_image_external : require

precision mediump float;
uniform sampler2D uTexture;
varying vec2 vTexCoordinate;

void main () {
    gl_FragColor = texture2D(uTexture, vTexCoordinate);
}