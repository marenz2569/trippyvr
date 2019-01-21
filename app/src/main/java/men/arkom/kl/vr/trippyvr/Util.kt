package men.arkom.kl.vr.trippyvr

import android.opengl.GLU.gluErrorString
import android.opengl.GLES20
import android.text.TextUtils
import android.util.Log

internal object Util {

    private val TAG = "Util"

    private val HALT_ON_GL_ERROR = true

    fun checkGlError(label: String) {
        var error = GLES20.glGetError()
        var lastError: Int
        if (error != GLES20.GL_NO_ERROR) {
            do {
                lastError = error
                Log.e(TAG, label + ": glError " + gluErrorString(lastError))
                error = GLES20.glGetError()
            } while (error != GLES20.GL_NO_ERROR)

            if (HALT_ON_GL_ERROR) {
                throw RuntimeException("glError " + gluErrorString(lastError))
            }
        }
    }

    fun compileProgram(vertexCode: Array<String>, fragmentCode: Array<String>): Int {
        checkGlError("Start of compileProgram")
        // prepare shaders and OpenGL program
        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexShader, TextUtils.join("\n", vertexCode))
        GLES20.glCompileShader(vertexShader)
        checkGlError("Compile vertex shader")

        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentShader, TextUtils.join("\n", fragmentCode))
        GLES20.glCompileShader(fragmentShader)
        checkGlError("Compile fragment shader")

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)

        // Link and check for errors.
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val errorMsg = "Unable to link shader program: \n" + GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, errorMsg)
            if (HALT_ON_GL_ERROR) {
                throw RuntimeException(errorMsg)
            }
        }
        checkGlError("End of compileProgram")

        return program
    }
}