package com.an.gl.shader

import android.content.Context
import android.opengl.GLES31
import com.an.gl.base.FboManager
import com.an.gl.base.OesFboShader

class FaceDetectionShader(
    context: Context,
    frameBufferObject: FboManager
) : OesFboShader(GLES31.GL_TEXTURE_2D,frameBufferObject) {

    companion object {
        private const val FILE_SIMPLE_VERTEX = "face_detection_vertex.glsl"
        private const val FILE_SIMPLE_FRAGMENT = "face_detection_fragment.glsl"
    }
}