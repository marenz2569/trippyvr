package men.arkom.kl.vr.trippyvr

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import java.lang.Exception
import java.util.*

internal class Camera {

    private companion object {
        val TAG = "Camera"

        lateinit var cameraHandler: Handler
        lateinit var cameraThread : HandlerThread

        lateinit var cameraSizes : Array<Size>
        lateinit var cameraRequestBuilder: CaptureRequest.Builder

        lateinit var surface : Surface
        lateinit var cameraTex : Texture
        lateinit var surfaceTexture : SurfaceTexture
    }

    private var cameraStateCallback = object : CameraDevice.StateCallback() {

        private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {


            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    /* We humbly set a repeating request for images.  i.e. a preview. */
                    session.setRepeatingRequest(cameraRequestBuilder.build(), null, Handler())
                } catch (e: CameraAccessException) {
                    Log.e("Camera Exception", e.message)
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }

        override fun onOpened(camera: CameraDevice)
        {
            // use largest camera size
            Arrays.sort(cameraSizes,  object : Comparator<Size> {
                override fun compare(entry1: Size, entry2: Size) : Int {
                    return (entry2.height * entry2.width).compareTo(entry1.height * entry1.width)
                }
            })
            surfaceTexture.setDefaultBufferSize(cameraSizes[0].width, cameraSizes[0].height)
            surface = Surface(surfaceTexture)

            cameraRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            cameraRequestBuilder.addTarget(surface)

            camera.createCaptureSession(ArrayList<Surface>(mutableListOf(surface)), cameraCaptureSessionStateCallback, Handler())
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
        surfaceTexture = SurfaceTexture(cameraTex.getTextureId())
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(
                        SurfaceTexture::class.java)
                    cameraThread = HandlerThread("CameraPreview")
                    cameraThread.start()
                    cameraHandler = Handler(cameraThread.looper)
                    cameraManager.openCamera(cameraId, cameraStateCallback, cameraHandler)
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

    fun drawGLES(objectProgram : Int,
                 objectSurfaceTextureParam : Int, objectModelViewProjectionParam: Int,
                 surfaceTextureProjection : FloatArray , modelViewProjection : FloatArray) {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(surfaceTextureProjection)
        GLES20.glUseProgram(objectProgram)
        GLES20.glUniformMatrix4fv(objectSurfaceTextureParam, 1, false, surfaceTextureProjection, 0)
        GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0)
        cameraTex.bind()
        Util.checkGlError("drawCamera")
    }
}

