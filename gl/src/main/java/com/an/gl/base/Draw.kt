package com.an.gl.base


/**
 * opengl 绘制流程
 * **/
interface Draw {

    /**
     * 初始化坐标数据（顶点坐标、纹理坐标）
     * **/
    fun initCoordinateData()

    /**
     * 获取顶点着色器代码
     * **/
    fun getVertexShadeCode(): String

    /**
     * 获取片元着色器代码
     * **/
    fun getFragmentShadeCode(): String

    /**
     * 初始化着色器程序
     * **/
    fun initShadeProgram()

    /**
     * 显示区域大小更改回调
     * **/
    fun onSizeChange(width: Int, height: Int)

    /**
     * 绘制回调
     * **/
    fun onDraw()

    /**
     * 释放资源
     * **/
    fun release()
}