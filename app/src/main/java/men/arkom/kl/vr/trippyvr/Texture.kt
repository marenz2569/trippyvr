package men.arkom.kl.vr.trippyvr

import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.IOException
import java.net.URL

internal class TextureFromBitmap
@Throws(IOException::class)
constructor() {
    private val textureId = IntArray(1)

    init {
        GLES20.glGenTextures(1, textureId, 0)
        bind()
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        val stream = URL("https://http.cat/401").openConnection().getInputStream()
        val textureBitmap = BitmapFactory.decodeStream(stream)
        //val textureBitmap = BitmapFactory.decodeStream(context.assets.open(texturePath))
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0)
        textureBitmap.recycle()
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
    }

    /** Binds the texture to GL_TEXTURE0.  */
    fun bind() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
    }
}

internal class Texture
@Throws(IOException::class)
constructor() {
    private val textureId = IntArray(1)

    init {
        GLES20.glGenTextures(1, textureId, 0)
        Util.checkGlError("glGenTexture")
        bind()
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        Util.checkGlError("glTexParameter")
    }

    /** Binds the texture to GL_TEXTURE0.  */
    fun bind() {
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0])
    }

    fun getTextureId() : Int {
        return textureId[0]
    }
}