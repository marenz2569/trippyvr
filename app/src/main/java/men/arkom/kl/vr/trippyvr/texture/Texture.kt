package men.arkom.kl.vr.trippyvr.texture

import android.opengl.GLES11Ext
import android.opengl.GLES20
import men.arkom.kl.vr.trippyvr.Util
import java.io.IOException

internal class Texture
@Throws(IOException::class)
constructor() {
    private val textureId = IntArray(1)

    init {
        GLES20.glGenTextures(1, textureId, 0)
        Util.checkGlError("glGenTexture")
        bind()
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR_MIPMAP_NEAREST
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        Util.checkGlError("glTexParameter")
    }

    /** Binds the texture to GL_TEXTURE0.  */
    fun bind() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0])
    }

    fun getTextureId(): Int {
        return textureId[0]
    }
}