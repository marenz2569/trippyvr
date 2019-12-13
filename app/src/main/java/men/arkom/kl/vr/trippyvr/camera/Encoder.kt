package men.arkom.kl.vr.trippyvr.camera

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.util.Log
import android.view.Surface

internal class Encoder {

    companion object {
        private val TAG = "Encoder"
        private val MIME_TYPE = "video/avc"

        private lateinit var encoder: MediaCodec
        lateinit var surface: Surface
    }

    private var encoderStateCallback = object : MediaCodec.Callback() {

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {}

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            val buffer = codec.getOutputBuffer(index)
            buffer!!.position(info.offset)
            buffer.limit(info.offset + info.size)
            codec.releaseOutputBuffer(index, false)
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}

    }

    fun init() {
        encoder = MediaCodec.createEncoderByType(
            MIME_TYPE
        )
        val mediaFormat =
            MediaFormat.createVideoFormat(
                MIME_TYPE,
                CameraPreview.eyeSize.width,
                CameraPreview.eyeSize.height
            )
        val videoCapabilities = encoder.codecInfo.getCapabilitiesForType(
            MIME_TYPE
        ).videoCapabilities
        val maxFps = videoCapabilities.getAchievableFrameRatesFor(
            CameraPreview.eyeSize.width,
            CameraPreview.eyeSize.height
        )!!.upper.toInt()
        val maxBitrate = videoCapabilities.bitrateRange.upper
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, maxBitrate)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, maxFps)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        Log.d(TAG, "format: $mediaFormat")
        encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = encoder.createInputSurface()
        encoder.setCallback(encoderStateCallback, Handler())
    }

    fun start() {
        encoder.start()
    }

    fun release() {
        encoder.release()
    }

}