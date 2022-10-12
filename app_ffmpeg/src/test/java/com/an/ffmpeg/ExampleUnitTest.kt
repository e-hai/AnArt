package com.an.ffmpeg

import kotlinx.coroutines.Dispatchers
import org.junit.Test

import org.junit.Assert.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    interface Driver {
        fun drive()
    }

    class CarDriver : Driver {
        override fun drive() {
            println("车的主人：我的车，但是喝酒了，不能开")
        }
    }


    @Test
    fun test() {
        val carDriver = CarDriver()

        val proxyDriver = Proxy.newProxyInstance(
            Driver::class.java.classLoader,
            arrayOf(Driver::class.java)
        ) { proxy, method, args ->
            println("代驾：老板，我是代驾")
            method?.invoke(carDriver)
            println("代驾：好的，我来开")
            null
        } as Driver

        proxyDriver.drive()
    }
}