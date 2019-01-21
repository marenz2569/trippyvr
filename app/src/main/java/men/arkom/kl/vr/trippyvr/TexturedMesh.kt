package men.arkom.kl.vr.trippyvr

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal class TexturedMesh
@Throws(IOException::class)
//constructor(context: Context, private val positionAttrib: Int, private val uvAttrib: Int) {
constructor(context: Context, private val objFilePath: String, val positionAttrib: Int, private val uvAttrib: Int) {

    private val vertices: FloatBuffer
    private val uv: FloatBuffer
    private val indices: ShortBuffer

    init {
        /*
        val intIndi = intArrayOf(0,1,2,3,4,5,6)
        val intIndices = IntBuffer.allocate(intIndi.lastIndex + 1)
        intIndices.put(intIndi)
        val vert = floatArrayOf(-1f,-1f,-0f,
            1f,-1f,-0f,
            -1f,1f,-0f,
            -1f,1f,-0f,
            1f,-1f,-0f,
            1f,1f,-0f)
        vertices = FloatBuffer.allocate(vert.lastIndex + 1)
        vertices.put(vert)
        val uv_ = floatArrayOf(0f,0f)
        uv = FloatBuffer.allocate(uv_.lastIndex + 1)
        uv.put(uv_)
        */
        val objInputStream = context.assets.open(objFilePath)
        val obj = ObjUtils.convertToRenderable(ObjReader.read(objInputStream))
        objInputStream.close()

        val intIndices = ObjData.getFaceVertexIndices(obj, 3)
        vertices = ObjData.getVertices(obj)
        uv = ObjData.getTexCoords(obj, 2)
        Log.i(TAG, "verticies: "+vertices)
        Log.i(TAG,"uv: "+uv)
        Log.i(TAG, "indicies: "+ intIndices)


        // Convert int indices to shorts (GLES doesn't support int indices)
        indices = ByteBuffer.allocateDirect(2 * intIndices.limit())
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        while (intIndices.hasRemaining()) {
            indices.put(intIndices.get().toShort())
        }
        indices.rewind()
    }

    /**
     * Draws the mesh. Before this is called, u_MVP should be set with glUniformMatrix4fv(), and a
     * texture should be bound to GL_TEXTURE0.
     */
    fun draw() {
        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glEnableVertexAttribArray(uvAttrib)
        GLES20.glVertexAttribPointer(uvAttrib, 2, GLES20.GL_FLOAT, false, 0, uv)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.limit(), GLES20.GL_UNSIGNED_SHORT, indices)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    companion object {
        private val TAG = "TexturedMesh"
    }
}