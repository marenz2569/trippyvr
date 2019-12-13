package men.arkom.kl.vr.trippyvr.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.google.vr.sdk.base.*
import men.arkom.kl.vr.trippyvr.Util
import men.arkom.kl.vr.trippyvr.camera.CameraPreview
import men.arkom.kl.vr.trippyvr.texture.TexturedMesh
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig

class TrippyVRRenderer(val context: Context) : GvrView.Renderer {

    private companion object {
        const val TAG = "TrippyVRRenderer"

        var cameraPreview = CameraPreview()

        var objectProgram: Int = 0

        var objectSurfaceTextureParam: Int = 0
        var objectModelViewProjectionParam: Int = 0

        lateinit var room: TexturedMesh

        var camera = FloatArray(16)
        var view = FloatArray(16)
        var surfaceTextureProjection = FloatArray(16)
        var modelViewProjection = FloatArray(16)
        var modelView = FloatArray(16)

        var modelRoom = FloatArray(16)

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

    init {
        // create the camera preview session
        cameraPreview.start(context)
    }

    override fun onSurfaceCreated(config: EGLConfig) {

        Log.i(TAG, "onSurfaceCreated")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        objectProgram =
            Util.compileProgram(
                OBJECT_VERTEX_SHADER_CODE,
                OBJECT_FRAGMENT_SHADER_CODE
            )

        objectPositionParam = GLES20.glGetAttribLocation(
            objectProgram, "a_Position"
        )
        objectUvParam = GLES20.glGetAttribLocation(
            objectProgram, "a_UV"
        )
        objectSurfaceTextureParam = GLES20.glGetUniformLocation(
            objectProgram, "u_ST"
        )
        objectModelViewProjectionParam = GLES20.glGetUniformLocation(
            objectProgram, "u_MVP"
        )

        Util.checkGlError("Object program params")

        Matrix.setIdentityM(modelRoom, 0)
        Matrix.translateM(modelRoom, 0, 0f, 0f, 0f)

        Util.checkGlError("onSurfaceCreated")

        try {
            room =
                TexturedMesh(
                    context, "Screen.obj",
                    objectPositionParam,
                    objectUvParam
                )
            //roomTex = Texture(this, "CubeRoom.png")
        } catch (e: IOException) {
            Log.e(TAG, "Unable to initialize objects", e)
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged")
    }

    override fun onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown")
        cameraPreview.release()

        TODO("release everything")
    }

    override fun onDrawFrame(headTransform: HeadTransform, leftEye: Eye, rightEye: Eye) {

        val angle = 30f
        val perspective = FloatArray(16)
        val fov = FieldOfView()
        fov.setAngles(angle, angle, angle, angle)
        fov.toPerspectiveMatrix(
            Z_NEAR,
            Z_FAR, perspective, 0
        )


        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // The clear color doesn't matter here because it's completely obscured by
        // the room. However, the color buffer is still cleared because it may
        // improve performance.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(1f, 0f, 0f, 1f)

        // draw the left eye
        setGlViewportFromEye(leftEye)
        Matrix.setLookAtM(camera, 0, 0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)
        drawToEye(perspective)

        // draw the right eye
        setGlViewportFromEye(rightEye)
        //Matrix.setLookAtM(camera, 0, 0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)
        var headView = FloatArray(16)
        headTransform.getHeadView(headView, 0)
        Matrix.multiplyMM(
            view, 0, headView, 0,
            camera, 0
        )
        drawToEye(perspective)

        // draw the whole room
        room.draw()
    }

    private fun setGlViewportFromEye(eye: Eye) {
        val viewport = eye.viewport
        GLES20.glViewport(viewport.x, viewport.y, viewport.width, viewport.height)
    }

    private fun drawToEye(perspective: FloatArray) {
        // Set modelView for the room, so it's drawn in the correct location
        Matrix.multiplyMM(
            modelView, 0,
            camera, 0,
            modelRoom, 0
        )
        Matrix.multiplyMM(
            modelViewProjection, 0, perspective, 0,
            modelView, 0
        )
        // draw the frame from the camera preview
        cameraPreview.drawGLES(
            objectProgram,
            objectSurfaceTextureParam,
            objectModelViewProjectionParam,
            surfaceTextureProjection,
            modelViewProjection
        )
    }

    override fun onFinishFrame(viewport: Viewport) {}
}