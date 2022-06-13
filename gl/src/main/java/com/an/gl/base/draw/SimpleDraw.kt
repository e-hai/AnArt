package com.an.gl.base.draw

import android.opengl.GLES31
import com.an.gl.base.texture.Texture
import com.an.gl.util.GlUtil
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 标志该坐标的轴数
 * **/
const val GL_COORDINATE_SYSTEM_XY = 2    //x,y轴组成一个坐标
const val GL_COORDINATE_SYSTEM_XYZ = 3   //x,y,z轴组成一个坐标
const val GL_COORDINATE_SYSTEM_XYZW = 4  //x,y,z,w轴组成一个坐标

/**
 * GL文件中应固定这几个参数名
 * **/
const val GL_NAME_VERTEX_COORD = "aPosition" //顶点坐标
const val GL_NAME_TEXTURE_COORD = "aTexCoor" //纹理坐标
const val GL_NAME_TEXTURE = "uTexture"       //纹理
const val GL_NAME_MVP_MATRIX = "uMVPMatrix"  //总变换矩阵

open class SimpleDraw(val texture: Texture) : Draw {

    val dataMvpMatrix = floatArrayOf(
        1f, 0f, 0f, 0f, //屏幕左上
        0f, 1f, 0f, 0f, //屏幕左下
        0f, 0f, 1f, 0f, //屏幕右下
        0f, 0f, 0f, 1f, //屏幕右上
    )
    private val dataVertexCoord = floatArrayOf(
        -1.0f, 1.0f,  //屏幕左上
        -1.0f, -1.0f, //屏幕左下
        1.0f, -1.0f,  //屏幕右下
        1.0f, 1.0f    //屏幕右上
    )
    private val dataTextureCoord = floatArrayOf(
        0.0f, 1.0f, //屏幕左上
        0.0f, 0.0f, //屏幕左下
        1.0f, 0.0f, //屏幕右下
        1.0f, 1.0f  //屏幕右上
    )
    val dataDrawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    lateinit var vertexCoordBuffer: FloatBuffer  //顶点坐标
    lateinit var textureCoordBuffer: FloatBuffer //纹理坐标
    lateinit var drawOrderBuffer: ShortBuffer    //绘制路径

    var programHandle: Int = 0       //OpenGL ES Program索引
    var vertexCoorHandle: Int = 0    //顶点坐标索引
    var textureCoorHandle: Int = 0   //纹理坐标索引
    var textureHandle: Int = 0       //纹理索引
    var mvpMatrixHandle: Int = 0     //总变换矩阵索引

    var outWidth: Int = 0
    var outHeight: Int = 0

    init {
        initCoordinateData()
        initShadeProgram()
    }


    final override fun initCoordinateData() {
        vertexCoordBuffer = GlUtil.createFloatBuffer(dataVertexCoord)
        textureCoordBuffer = GlUtil.createFloatBuffer(dataTextureCoord)
        drawOrderBuffer = GlUtil.createShortBuffer(dataDrawOrder)
    }

    override fun getVertexShadeCode(): String {
        return """
            uniform mat4 $GL_NAME_MVP_MATRIX;
            attribute vec4 $GL_NAME_VERTEX_COORD;
            attribute vec2 $GL_NAME_TEXTURE_COORD;
            varying vec2 vTexCoordinate;
            
            void main(){
                vTexCoordinate = $GL_NAME_TEXTURE_COORD;
                gl_Position = $GL_NAME_MVP_MATRIX * $GL_NAME_VERTEX_COORD;
            }
        """.trimIndent()
    }

    override fun getFragmentShadeCode(): String {
        return """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;
            uniform sampler2D $GL_NAME_TEXTURE;
            varying vec2 vTexCoordinate;

            void main () {
                gl_FragColor = texture2D($GL_NAME_TEXTURE, vTexCoordinate);
            }
        """.trimIndent()
    }

    final override fun initShadeProgram() {
        val vertexShader: Int = GlUtil.loadVertexShader(getVertexShadeCode())
        val fragmentShader: Int = GlUtil.loadFragmentShader(getFragmentShadeCode())
        programHandle = GlUtil.loadProgram(listOf(vertexShader, fragmentShader))
        vertexCoorHandle = GLES31.glGetAttribLocation(programHandle, GL_NAME_VERTEX_COORD)
        textureCoorHandle = GLES31.glGetAttribLocation(programHandle, GL_NAME_TEXTURE_COORD)
        mvpMatrixHandle = GLES31.glGetUniformLocation(programHandle, GL_NAME_MVP_MATRIX)
        textureHandle = GLES31.glGetUniformLocation(programHandle, GL_NAME_TEXTURE)
    }

    override fun release() {
        GLES31.glDeleteProgram(programHandle)
        texture.release()
    }

    override fun onSizeChange(width: Int, height: Int) {
        outWidth = width
        outHeight = height
    }

    override fun onDraw() {
        //设置视图区域
        updateViewport()

        //要启用的着色器程序
        GLES31.glUseProgram(programHandle)

        //将矩阵数据传进渲染管线
        GLES31.glUniformMatrix4fv(mvpMatrixHandle, 1, false, dataMvpMatrix, 0)

        //将顶点坐标数据传进渲染管线
        GLES31.glVertexAttribPointer(
            vertexCoorHandle,            //顶点坐标索引
            GL_COORDINATE_SYSTEM_XY,     //每个顶点坐标的组成分量数，如(x,y) (x,y,z)等等，如果数据分量size=2,但是顶点变量attribute vec4，那么该方法会把z、w自动设置默认为0、1
            GLES31.GL_FLOAT,             //数据类型
            false,             //如果赋值给 attribute 的数据为int值,而 attribute 为float值的时候,是否需要被归一化,归一化就是带符号的值转化为(-1,1), 不带符号的值转化为(0,1)。不过我们传递的数据一般都与Attribute相对应
            0,                     //第五个参数 stride 是间隔的意思,举个例子, 刚才为了画三角形,需要传入 12 个值。那么可以直接创建一个 12 个值的数组传入,也可以创建一个 15 个值的数组传入,其中第 5、10、15 这 4 个值为无用值, 第 1234、6789、1112131415 这 12 个值是有用的,然后把 size 写为 4,stride 写 为 5,GPU 就知道 5 个值为一个单元进行读取,然后前四个为有效值,使用前四个对 attribute 进行赋值
            vertexCoordBuffer            //顶点坐标数据
        )
        //启用顶点坐标属性，这里会影响后续的顶点操作（纹理坐标映射、顶点绘制）
        GLES31.glEnableVertexAttribArray(vertexCoorHandle)

        //将纹理坐标数据传进渲染管线
        GLES31.glVertexAttribPointer(
            textureCoorHandle,
            GL_COORDINATE_SYSTEM_XY,
            GLES31.GL_FLOAT,
            false,
            0,
            textureCoordBuffer
        )
        //启用纹理坐标属性
        GLES31.glEnableVertexAttribArray(textureCoorHandle)

        //绑定纹理，这里会影响后续纹理操作
        texture.bindTexture()

        //绑定的纹理，关联到着色器的纹理属性上
        GLES31.glUniform1i(textureHandle, GLES31.GL_TEXTURE0)

        //按照绘制数据来绘制顶点
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,          //图元的形状（点、线、三角形）
            dataDrawOrder.size,           //顶点数量
            GLES31.GL_UNSIGNED_SHORT,     //元素类型
            drawOrderBuffer               //元素数据
        )

        //解绑纹理
        texture.unbindTexture()

        //关闭使用纹理坐标属性
        GLES31.glDisableVertexAttribArray(textureCoorHandle)

        //关闭使用顶点坐标属性
        GLES31.glDisableVertexAttribArray(vertexCoorHandle)
    }

    override fun updateViewport() {
        GLES31.glViewport(0, 0, outWidth, outHeight)
    }
}