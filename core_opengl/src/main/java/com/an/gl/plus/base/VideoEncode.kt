/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.an.gl.plus.base

import android.media.MediaMuxer
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.File
import java.lang.RuntimeException

/**
 * 视频编码器
 */
class VideoEncode(
    private val originalWidth: Int,
    private val originalHeight: Int,
    private val originalFrameRate: Int,
    outputFile: File
) {
    /**
     * Returns the encoder's input surface.
     */
    private var mMuxer: MediaMuxer
    private var mEncoder: MediaCodec
    private val mBufferInfo: MediaCodec.BufferInfo
    private var mTrackIndex: Int
    private var mMuxerStarted: Boolean
    private var bitRate = 0
    val inputSurface: Surface
    var frameRate: Int = 0
    var width: Int = 0
    var height: Int = 0


    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    init {
        initPrefaceInfo()
        mBufferInfo = MediaCodec.BufferInfo()
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height)
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        if (VERBOSE) Log.d(TAG, "format: $format")

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE)
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = mEncoder.createInputSurface()
        mEncoder.start()

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        mMuxer = MediaMuxer(
            outputFile.toString(),
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
        mTrackIndex = -1
        mMuxerStarted = false
    }


    private fun initPrefaceInfo() {
        width = originalWidth
        height = originalHeight
        frameRate = originalFrameRate

        val inShort: Float
        val inLong: Float
        if (originalWidth > originalHeight) {
            inShort = originalHeight.toFloat()
            inLong = originalWidth.toFloat()
        } else {
            inShort = originalWidth.toFloat()
            inLong = originalHeight.toFloat()
        }

        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {
                continue
            }
            val types = codecInfo.supportedTypes
            for (type in types) {
                if (type.equals(MIME_TYPE, ignoreCase = true)) {
                    val capabilities = codecInfo.getCapabilitiesForType(MIME_TYPE)
                    val supportFrameRate = capabilities.videoCapabilities.supportedFrameRates
                    frameRate = when {
                        originalFrameRate > supportFrameRate.upper -> {
                            supportFrameRate.upper
                        }
                        originalFrameRate < supportFrameRate.lower -> {
                            supportFrameRate.lower
                        }
                        else -> {
                            originalFrameRate
                        }
                    }
                    Log.d(TAG, "FrameRate=$frameRate")

                    val supportMaxW = capabilities.videoCapabilities.supportedWidths.upper.toFloat()
                    val supportMaxH =
                        capabilities.videoCapabilities.supportedHeights.upper.toFloat()

                    Log.d(TAG, "supportMaxW=$supportMaxW supportMaxH=$supportMaxH")

                    val supportShort: Float
                    val supportLong: Float
                    if (supportMaxW > supportMaxH) {
                        supportLong = supportMaxW
                        supportShort = supportMaxH
                    } else {
                        supportLong = supportMaxH
                        supportShort = supportMaxW
                    }
                    Log.d(TAG, "supportLong=$supportLong supportShort=$supportShort")

                    var scale = 1f
                    if (inLong > supportLong) {
                        scale = inLong / supportLong
                        Log.d(TAG, "1")
                    } else if (inShort > supportShort) {
                        scale = inShort / supportShort
                        Log.d(TAG, "2")
                    }

                    Log.d(TAG, "start width=$width height=$height")
                    Log.d(TAG, "scale=$scale")
                    width = (originalWidth / scale).toInt()
                    height = (originalHeight / scale).toInt()
                    Log.d(TAG, "end width=$width height=$height")
                    break
                }
            }
        }

        bitRate = (frameRate * width * height)
    }

    /**
     * Releases encoder resources.
     */
    fun release() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects")
        mEncoder.stop()
        mEncoder.release()

        // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
        //       of frames submitted, and don't call stop() if we haven't written anything.
        mMuxer.stop()
        mMuxer.release()
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     *
     *
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     *
     *
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    fun drainEncoder(endOfStream: Boolean) {
        val TIMEOUT_USEC = 10000
        if (VERBOSE) Log.d(TAG, "drainEncoder($endOfStream)")
        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder")
            mEncoder.signalEndOfInputStream()
        }
        var encoderOutputBuffers = mEncoder.outputBuffers
        while (true) {
            val encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC.toLong())
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS")
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw RuntimeException("format changed twice")
                }
                val newFormat = mEncoder.outputFormat
                Log.d(TAG, "encoder output format changed: $newFormat")

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat)
                mMuxer.start()
                mMuxerStarted = true
            } else if (encoderStatus < 0) {
                Log.w(
                    TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus
                )
                // let's ignore it
            } else {
                val encodedData = encoderOutputBuffers[encoderStatus]
                    ?: throw RuntimeException(
                        "encoderOutputBuffer " + encoderStatus +
                                " was null"
                    )
                if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                    mBufferInfo.size = 0
                }
                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw RuntimeException("muxer hasn't started")
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset)
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size)
                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo)
                    if (VERBOSE) {
                        Log.d(
                            TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs
                        )
                    }
                }
                mEncoder.releaseOutputBuffer(encoderStatus, false)
                if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly")
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached")
                    }
                    break // out of while
                }
            }
        }
        if (VERBOSE) Log.d(TAG, "sending EOS to next")
    }

    companion object {
        private const val TAG = "VideoEncode"
        private const val VERBOSE = true

        // TODO: these ought to be configurable as well
        private const val MIME_TYPE = "video/avc" // H.264 Advanced Video Coding
        private const val FRAME_RATE = 30 // 30fps
        private const val IFRAME_INTERVAL = 2 // 5 seconds between I-frames
    }

}