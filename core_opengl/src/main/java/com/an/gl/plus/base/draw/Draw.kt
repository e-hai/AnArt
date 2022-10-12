package com.an.gl.plus.base.draw


/**
 * opengl 绘制流程
 * **/
interface Draw {

    /**
     * 获取顶点着色器代码
     * **/
    fun getVertexShadeCode(): String

    /**
     * 获取片元着色器代码
     * **/
    fun getFragmentShadeCode(): String

    /**
     * 初始化坐标数据（顶点坐标、纹理坐标）
     * **/
    fun initCoordinateData()

    /**
     * 初始化着色器程序
     * **/
    fun initShadeProgram()

    /**
     * 最大的显示区域更改回调
     * **/
    fun onSizeChange(width: Int, height: Int)

    /**
     * 更新视图区域
     * */
    fun updateViewport()

    /**
     * 绘制回调
     * **/
    fun onDraw()

    /**
     * 释放资源
     * **/
    fun release()
}