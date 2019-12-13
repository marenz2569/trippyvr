package men.arkom.kl.vr.trippyvr.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import men.arkom.kl.vr.trippyvr.Util
import men.arkom.kl.vr.trippyvr.texture.Texture
import java.util.*

internal class CameraPreview {

    companion object {
        private val TAG = "CameraPreview"

        private lateinit var cameraHandler: Handler
        private lateinit var cameraHandlerThread: HandlerThread

        val eyeSize = Size(2160, 2160)

        private lateinit var cameraRequestBuilder: CaptureRequest.Builder
        private lateinit var cameraManager: CameraManager
        private lateinit var cameraSizes: Array<Size>

        private lateinit var surface: Surface
        private lateinit var cameraTex: Texture
        private lateinit var surfaceTexture: SurfaceTexture

        private val encoder = Encoder()
    }

    private var CameraDeviceStateCallback = object : CameraDevice.StateCallback() {

        private val cameraCaptureSessionStateCallback =
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        /* We humbly set a repeating request for images.  i.e. a preview. */
                        session.setRepeatingRequest(cameraRequestBuilder.build(), null, Handler())
                    } catch (e: CameraAccessException) {
                        Log.e("CameraAccessException", e.message)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}

            }

        override fun onOpened(camera: CameraDevice) {
            if (!Arrays.stream(cameraSizes).anyMatch { size -> size.width >= eyeSize.width && size.height >= eyeSize.height })
                throw Exception("Camera size is not supported: ${eyeSize}")

            encoder.init()
            encoder.start()

            surfaceTexture.setDefaultBufferSize(
                eyeSize.width, eyeSize.height
            )
            surface = Surface(
                surfaceTexture
            )

            cameraRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            cameraRequestBuilder.addTarget(
                Encoder.surface
            )
            cameraRequestBuilder.addTarget(
                surface
            )

            camera.createCaptureSession(
                ArrayList<Surface>(
                    mutableListOf(
                        Encoder.surface,
                        surface
                    )
                ), cameraCaptureSessionStateCallback, Handler()
            )

        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
        }

    }

    fun start(context: Context) {
        cameraTex = Texture()
        surfaceTexture = SurfaceTexture(
            cameraTex.getTextureId()
        )
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraSizes =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(
                            SurfaceTexture::class.java
                        )
                    cameraHandlerThread = HandlerThread("CameraPreview")
                    cameraHandlerThread.start()
                    cameraHandler = Handler(
                        cameraHandlerThread.looper
                    )
                    cameraManager.openCamera(
                        cameraId, CameraDeviceStateCallback,
                        cameraHandler
                    )
                    break
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No permissions to open camera", e.cause)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open camera and/or start thread", e.cause)
            e.printStackTrace()
        }
    }

    fun release() {
        encoder.release()
        surface.release()
    }

    fun drawGLES(
        objectProgram: Int,
        objectSurfaceTextureParam: Int, objectModelViewProjectionParam: Int,
        surfaceTextureProjection: FloatArray, modelViewProjection: FloatArray
    ) {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(surfaceTextureProjection)
        GLES20.glUseProgram(objectProgram)
        GLES20.glUniformMatrix4fv(objectSurfaceTextureParam, 1, false, surfaceTextureProjection, 0)
        GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0)
        cameraTex.bind()
        Util.checkGlError("drawCamera")
    }

}