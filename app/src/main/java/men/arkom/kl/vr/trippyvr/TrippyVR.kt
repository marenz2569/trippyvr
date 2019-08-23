package men.arkom.kl.vr.trippyvr

import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import com.google.vr.sdk.base.*
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig

class TrippyVR : GvrActivity(), GvrView.Renderer {

    private companion object {
        val TAG = "TrippyVR"

        var objectProgram: Int = 0

        var objectSurfaceTextureParam: Int = 0
        var objectModelViewProjectionParam: Int = 0

        lateinit var room: TexturedMesh

        var cameraObj = CameraPreview()

        var camera                   = FloatArray(16)
        var view                     = FloatArray(16)
        var surfaceTextureProjection = FloatArray(16)
        var modelViewProjection      = FloatArray(16)
        var modelView                = FloatArray(16)

        var modelRoom                = FloatArray(16)

        var objectPositionParam: Int = 0
        var objectUvParam: Int = 0

        const val Z_NEAR = 0.01f
        const val Z_FAR = 10.0f

        val OBJECT_VERTEX_SHADER_CODE = arrayOf(
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
        val OBJECT_FRAGMENT_SHADER_CODE = arrayOf(
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

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraObj.start(this)

        initializeGvrView()
    }

    private fun initializeGvrView() {
        setContentView(R.layout.activity_main)

        val gvrView = findViewById<GvrView>(R.id.gvr_view)
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

    override fun onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown")
        cameraObj.release()

        TODO("release everything")
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

        val perspective = FloatArray(16)
        val fov = FieldOfView()
        fov.setAngles(30f,30f,30f,30f)
        fov.toPerspectiveMatrix(Z_NEAR, Z_FAR, perspective, 0)


        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // The clear color doesn't matter here because it's completely obscured by
        // the room. However, the color buffer is still cleared because it may
        // improve performance.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(1f,0f,0f,1f)

        Matrix.setLookAtM(camera, 0, 0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)

        setGlViewportFromEye(leftEye)

        // Set modelView for the room, so it's drawn in the correct location
        Matrix.multiplyMM(modelView, 0, camera, 0, modelRoom, 0)
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0)
        cameraObj.drawGLES(objectProgram, objectSurfaceTextureParam, objectModelViewProjectionParam, surfaceTextureProjection, modelViewProjection)
        room.draw()

        //Matrix.setLookAtM(camera, 0, 0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)

        setGlViewportFromEye(rightEye)

        var headView = FloatArray(16)
        headTransform.getHeadView(headView, 0)
        Matrix.multiplyMM(view, 0, headView, 0, camera, 0)

        // Set modelView for the room, so it's drawn in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelRoom, 0)
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0)
        cameraObj.drawGLES(objectProgram, objectSurfaceTextureParam, objectModelViewProjectionParam, surfaceTextureProjection, modelViewProjection)
        room.draw()
    }

    private fun setGlViewportFromEye(eye: Eye) {
        val viewport = eye.viewport
        GLES20.glViewport(viewport.x, viewport.y, viewport.width, viewport.height)
    }

    override fun onFinishFrame(viewport: Viewport) {}

}