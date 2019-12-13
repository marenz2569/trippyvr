package men.arkom.kl.vr.trippyvr

import android.os.Bundle
import android.util.Log
import com.google.vr.sdk.base.AndroidCompat
import com.google.vr.sdk.base.GvrActivity
import com.google.vr.sdk.base.GvrView
import men.arkom.kl.vr.trippyvr.renderer.TrippyVRRenderer

class TrippyVR : GvrActivity() {

    private companion object {
        val TAG = "TrippyVR"
    }

    /**
     * Sets the view to our GvrView and initializes the transformation matrices we will use
     * to render our scene.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeGvrView()
    }

    private fun initializeGvrView() {
        Log.i(TAG, "initializeGvrView")

        setContentView(R.layout.activity_main)

        val gvrView = findViewById<GvrView>(R.id.gvr_view)
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8)

        gvrView.setRenderer(
            TrippyVRRenderer(
                this
            )
        )
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

}