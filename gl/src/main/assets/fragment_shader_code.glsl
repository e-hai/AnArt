#extension GL_OES_EGL_image_external:require
precision mediump float;
varying vec2 linkTexCoor;
uniform samplerExternalOES samplerTexture;

void main() {
    gl_FragColor = texture2D(samplerTexture, linkTexCoor);
}
