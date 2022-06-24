package com.an.gl.base

import android.graphics.SurfaceTexture
import android.view.Surface
import com.an.gl.base.draw.*
import com.an.gl.base.texture.Texture
import com.an.gl.base.texture.TextureExternalOes


/**
 * 1: 创建TextureSurface,绑定并输出external oes纹理
 * 2: 创建绘制类
 * 3：通过绘制类把external oes 转成 2d oes，并保存在FBO中
 * **/
class MediaEglManager {

    //创建 external oes 纹理
    private val texture = TextureExternalOes()

    //居于external oes纹理创建SurfaceTexture，Camera、Video只支持external oes 纹理
    private val surfaceTexture: SurfaceTexture = SurfaceTexture(texture.getTextureId())

    //居于SurfaceTexture创建Surface，提供给Camera、Video推送图像流数据
    val surface: Surface = Surface(surfaceTexture)

    private val mediaDraw: MediaDraw = MediaDraw(texture)
    private val fboManager: FrameBufferObjectManager = FrameBufferObjectManager()
    private val screenDraw: SimpleDraw = SimpleDraw(fboManager.getTexture())


    fun setOnFrameAvailableListener(listener: SurfaceTexture.OnFrameAvailableListener) {
        surfaceTexture.setOnFrameAvailableListener(listener)
    }

    fun onSizeChange(width: Int, height: Int) {
        surfaceTexture.setDefaultBufferSize(width, height)
        mediaDraw.onSizeChange(width, height)
        screenDraw.onSizeChange(width, height)
        fboManager.onSizeChange(width, height)
    }

    fun onDraw(drawSelf: () -> Unit) {
        //取出新的一帧数据
        surfaceTexture.updateTexImage()

        //把Camera、Video设置给SurfaceTexture的矩阵数据传到总变换矩阵中
        surfaceTexture.getTransformMatrix(mediaDraw.dataMvpMatrix)

        //把SurfaceTexture的external oes纹理绘制并转成2d oes，保存在FBO中
        fboManager.load {
            mediaDraw.onDraw()
            //回调给外部其他需渲染到FBO中的操作
            drawSelf()
        }
        //输出到显示的Surface
        screenDraw.onDraw()
    }

    private class MediaDraw(texture: Texture) : SimpleDraw(texture) {
        override fun getVertexShadeCode(): String {
            return """
            attribute vec4 $GL_NAME_VERTEX_COORD;
            attribute vec4 $GL_NAME_TEXTURE_COORD;
            uniform mat4 $GL_NAME_MVP_MATRIX;
            varying vec2 vTexCoordinate;
            
            void main(){
                vTexCoordinate = ($GL_NAME_MVP_MATRIX * $GL_NAME_TEXTURE_COORD).xy;
                gl_Position = aPosition;
            }
        """.trimIndent()
        }

        override fun getFragmentShadeCode(): String {
            return """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;
            uniform samplerExternalOES $GL_NAME_TEXTURE;
            varying vec2 vTexCoordinate;

            void main () {
                gl_FragColor = texture2D($GL_NAME_TEXTURE, vTexCoordinate);
            }
        """.trimIndent()
        }
    }
}