package men.arkom.kl.vr.trippyvr

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.IOException
import java.lang.Exception
import java.util.*
import javax.microedition.khronos.egl.EGLConfig

class TrippyVR : GvrActivity(), GvrView.Renderer {

    private var objectProgram: Int = 0

    private var objectSurfaceTextureParam: Int = 0
    private var objectModelViewProjectionParam: Int = 0

    private var room: TexturedMesh? = null
    private var roomTex: Texture? = null

    private var camera: FloatArray? = null
    private var view: FloatArray? = null
    private var surfaceTextureProjection: FloatArray? = null
    private var modelViewProjection: FloatArray? = null
    private var modelView: FloatArray? = null

    private var modelRoom: FloatArray? = null

    private var objectPositionParam: Int = 0
    private var objectUvParam: Int = 0

    private var aspectRatio: Float = 0f

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        camera = FloatArray(16)
        view = FloatArray(16)
        surfaceTextureProjection = FloatArray(16)
        modelViewProjection = FloatArray(16)
        modelView = FloatArray(16)

        modelRoom = FloatArray(16)

        val display = windowManager.defaultDisplay
        screenParams = ScreenParams(display)
        aspectRatio = screenParams.width.toFloat() / 2f / screenParams.height.toFloat()

        initializeGvrView()

        initializeCamera()
    }

    private fun initializeGvrView() {
        setContentView(R.layout.activity_main)

        val gvrView = findViewById(R.id.gvr_view) as GvrView
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8)

        gvrView.setRenderer(this)
        gvrView.setTransitionViewEnabled(false)

        // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
        // Daydream controller input for basic interactions using the existing Cardboard trigger API.
        gvrView.enableCardboardTriggerEmulation()

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true)
        }

        setGvrView(gvrView)
    }

    private fun initializeCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
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

    private val cameraStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice)
        {
            roomTex = Texture(this@MainActivity)
            surfaceTexture = SurfaceTexture(roomTex!!.getTextureId())
            // use largest camera size
            Arrays.sort(cameraSizes,  object : Comparator<Size> {
                override fun compare(entry1: Size, entry2: Size) : Int {
                    return (entry2.height * entry2.width).compareTo(entry1.height * entry1.width)
                }
            })
            surfaceTexture.setDefaultBufferSize(cameraSizes[0].width, cameraSizes[0].height)
            val surfaces = ArrayList<Surface>()
            val surface = Surface(surfaceTexture)
            surfaces.add(surface)
            cameraRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            cameraRequestBuilder.addTarget(surface)

            cameraDevice.createCaptureSession(surfaces, cameraCaptureSessionStateCallback, Handler())
        }

        override fun onDisconnected(cameraDevice: CameraDevice)
        {
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, error: Int)
        {
            cameraDevice.close()
        }
    }

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

    override fun onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown")
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged")
    }

    override fun onSurfaceCreated(config: EGLConfig) {
        Log.i(TAG, "onSurfaceCreated")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        objectProgram = Util.compileProgram(OBJECT_VERTEX_SHADER_CODE, OBJECT_FRAGMENT_SHADER_CODE)

        objectPositionParam = GLES20.glGetAttribLocation(objectProgram, "a_Position")
        objectUvParam = GLES20.glGetAttribLocation(objectProgram, "a_UV")
        objectSurfaceTextureParam = GLES20.glGetUniformLocation(objectProgram, "u_ST")
        objectModelViewProjectionParam = GLES20.glGetUniformLocation(objectProgram, "u_MVP")

        Util.checkGlError("Object program params")

        Matrix.setIdentityM(modelRoom, 0)
        Matrix.translateM(modelRoom, 0, 0f, 0f, 0f)

        Util.checkGlError("onSurfaceCreated")

        try {
            room = TexturedMesh(this, "Screen.obj", objectPositionParam, objectUvParam)
            //roomTex = Texture(this, "CubeRoom.png")
        } catch (e: IOException) {
            Log.e(TAG, "Unable to initialize objects", e)
        }
    }

    override fun onDrawFrame(headTransform: HeadTransform, leftEye: Eye, rightEye: Eye) {

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(surfaceTextureProjection)

        Matrix.setLookAtM(camera, 0, 0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // The clear color doesn't matter here because it's completely obscured by
        // the room. However, the color buffer is still cleared because it may
        // improve performance.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        setGlViewportFromEye(leftEye)

        // Apply the eye transformation to the camera.
        //Matrix.multiplyMM(view, 0, rightEye.eyeView, 0, camera, 0)
        view = camera
        var headView = FloatArray(16)
        headTransform.getHeadView(headView, 0)
        //Matrix.multiplyMM(view, 0, headView, 0, camera, 0)

        // Build the ModelView and ModelViewProjection matrices
        // for calculating the position of the target object.
        //val perspective = rightEye.getPerspective(Z_NEAR, Z_FAR)
        val perspective = FloatArray(16)
        var fov = FieldOfView()
        val topBottom = 60f
        val leftRight = topBottom * aspectRatio
        val tb = topBottom/2
        val lr = leftRight/2
        fov.setAngles(tb, tb, lr, lr)
        fov.toPerspectiveMatrix(Z_NEAR, Z_FAR,perspective, 0)

        // Set modelView for the room, so it's drawn in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelRoom, 0)
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0)
        drawRoom()

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f)

        setGlViewportFromEye(rightEye)

        // Apply the eye transformation to the camera.
        //Matrix.multiplyMM(view, 0, rightEye.eyeView, 0, camera, 0)
        //view = camera
        headView = FloatArray(16)
        headTransform.getHeadView(headView, 0)
        Matrix.multiplyMM(view, 0, headView, 0, camera, 0)

        // Set modelView for the room, so it's drawn in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelRoom, 0)
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0)
        drawRoom()
    }

    private fun setGlViewportFromEye(eye: Eye) {
        val viewport = eye.viewport
        GLES20.glViewport(viewport.x, viewport.y, viewport.width, viewport.height)
    }

    /** Draw the room.  */
    private fun drawRoom() {
        GLES20.glUseProgram(objectProgram)
        GLES20.glUniformMatrix4fv(objectSurfaceTextureParam, 1, false, surfaceTextureProjection, 0)
        GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0)
        roomTex!!.bind()
        room!!.draw()
        Util.checkGlError("drawRoom")
    }

    override fun onFinishFrame(viewport: Viewport) {}

    companion object {
        private val TAG = "TrippyVR"

        lateinit var screenParams: ScreenParams

        lateinit var cameraSizes: Array<Size>
        lateinit var cameraThread: HandlerThread
        lateinit var cameraHandler: Handler
        lateinit var cameraRequestBuilder: CaptureRequest.Builder

        lateinit var surfaceTexture: SurfaceTexture

        private val Z_NEAR = 0.01f
        private val Z_FAR = 10.0f

        private val OBJECT_VERTEX_SHADER_CODE = arrayOf(
            "uniform mat4 u_MVP;",
            "uniform mat4 u_ST;",
            "attribute vec4 a_Position;",
            "attribute vec4 a_UV;",
            "varying vec2 v_UV;",
            "",
            "void main() {",
            "  gl_Position = u_MVP * a_Position;",
            "  v_UV = (u_ST * a_UV).xy;",
            "}"
        )
        private val OBJECT_FRAGMENT_SHADER_CODE = arrayOf(
            "#extension GL_OES_EGL_image_external : require",
            "precision mediump float;",
            "varying vec2 v_UV;",
            //"uniform sampler2D u_Texture;",
            "uniform samplerExternalOES u_Texture;",
            "",
            "void main() {",
            "  gl_FragColor = texture2D(u_Texture, v_UV);",
            "}"
        )
    }
}