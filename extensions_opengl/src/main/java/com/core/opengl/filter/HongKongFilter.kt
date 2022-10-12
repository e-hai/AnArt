package com.core.opengl.filter

import com.an.gl.gpuimage.filter.*

/**
 * iphone参数
 * 曝光 -53
 * 鲜明度 12
 * 高光 19
 * 阴影 -28
 * 对比度 14
 * 亮度 -15
 * 黑点 -13
 * 饱和度 -13
 * 自然饱和度 18
 * 色温 39
 * 色调 -22
 * 锐度 14
 * 清晰度 15
 * 晕影 38
 * 滤镜 反差色
 * **/
class HongKongFilter : GPUImageFilterGroup() {

    //曝光
    private val exposureFilter = GPUImageExposureFilter(-53F / 100F * 10F)

    //鲜明度（又称色彩明度）
    private val vibranceFilter = GPUImageVibranceFilter(12F / 100F)

    //高光、阴影
    private val highlightShadowFilter = GPUImageHighlightShadowFilter(-28F, 19F)

    //对比度
    private val contrastFilter = GPUImageContrastFilter(14F)

    //亮度
    private val brightnessFilter = GPUImageBrightnessFilter(-15F)

    //黑点


    //饱和度
    private val saturationFilter = GPUImageSaturationFilter(-13F)

    //自然饱和度

    //色温（又称白平衡）
    private val whiteBalanceFilter = GPUImageWhiteBalanceFilter(39F / 100F * 5000F + 5000F, 0.0F)

    //色调
    private val hueFilter = GPUImageHueFilter(-22F)

    //锐度
    private val sharpenFilter = GPUImageSharpenFilter(14F)

    //清晰度


    //反差色
    private val colorInvertFilter = GPUImageColorInvertFilter()
}