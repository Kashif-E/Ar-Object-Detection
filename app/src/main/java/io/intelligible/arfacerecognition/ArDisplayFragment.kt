package io.intelligible.arfacerecognition

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.filament.ColorGrading
import com.google.ar.core.Pose
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.ArFragment.OnViewCreatedListener
import com.google.mlkit.vision.objects.DetectedObject
import io.intelligible.arfacerecognition.databinding.ArFragmentBinding


typealias IdAnalyzer = (detectedObject: DetectedObject) -> Unit

class ArDisplayFragment : Fragment(R.layout.ar_fragment),
    OnViewCreatedListener, Scene.OnUpdateListener {

    private lateinit var anchorNode: AnchorNode
    private lateinit var binding: ArFragmentBinding
    private var renderable: Renderable? = null
    private var viewRenderable: ViewRenderable? = null
    private var detected = false


    private var arFragment: ArFragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = ArFragmentBinding.bind(view)
        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

        loadModels()
        binding.textView2.setOnClickListener {
            arFragment!!.arSceneView.scene.addOnUpdateListener(this)
            binding.trackingState.visibility = View.VISIBLE
        }
        binding.textView3.setOnClickListener {
            arFragment!!.arSceneView.scene.removeOnUpdateListener(this)
        }
        arFragment!!.setOnViewCreatedListener(this)

    }


    override fun onViewCreated(arFragment: ArFragment?, arSceneView: ArSceneView?) {

        val renderer = arSceneView!!.renderer

        if (renderer != null) {
            renderer.filamentView.colorGrading = ColorGrading.Builder()
                .toneMapping(ColorGrading.ToneMapping.FILMIC)
                .build(EngineInstance.getEngine().filamentEngine)
        }
    }


    private fun loadModels() {

        ViewRenderable.builder()
            .setView(requireContext(), R.layout.viewnode_render)
            .build()
            .thenAccept { view: ViewRenderable ->
                viewRenderable = view
                viewRenderable!!.isShadowCaster = false
                viewRenderable!!.isShadowReceiver = false
            }
            .exceptionally {
                Toast.makeText(requireContext(), "Unable to load model", Toast.LENGTH_LONG).show()
                null
            }
    }

    override fun onUpdate(frameTime: FrameTime?) {
        val view = arFragment?.arSceneView

        if (!detected) {

            //Create a bitmap the size of the scene view.
            if (view!!.width > 0 && view.height > 0) {

                val bitmap = Bitmap.createBitmap(
                    view.width, view.height,
                    Bitmap.Config.ARGB_8888
                )

                val handlerThread = HandlerThread("PixelCopier")

                handlerThread.start()

                PixelCopy.request(view, bitmap, { copyResult ->

                    if (copyResult == PixelCopy.SUCCESS) {
                        val dd = ObjectDetector(bitmap) {

                            if (it.trackingId == 1) {
                                val pos = floatArrayOf(0f, 0f, -1f)
                                val rotation = floatArrayOf(0f, 0f, 0f, 0f)
                                val coords = it.boundingBox.exactCenterX()
                                    .toInt() to it.boundingBox.exactCenterY().toInt()
                                val (atX, atY) = coords


                                val anchor = arFragment!!.arSceneView.session!!.createAnchor(
                                    Pose(
                                        pos,
                                        rotation
                                    )
                                )
                                anchorNode = AnchorNode(anchor).apply {
                                    localPosition.x = atX.toFloat()
                                    localPosition.y = atY.toFloat()
                                }
                                val faceInfo = Node()
                                faceInfo.setParent(anchorNode)
                                faceInfo.isEnabled = false
                                faceInfo.localPosition = Vector3(0.0f, 0.0f, 0.0f)
                                faceInfo.renderable = viewRenderable
                                faceInfo.isEnabled = true
                                anchorNode.setParent(arFragment!!.arSceneView.scene)
                                detected = true
                                arFragment!!.arSceneView.scene.removeOnUpdateListener(this)
                                Toast.makeText(requireContext(), "kate", Toast.LENGTH_SHORT).show()
                            } else if (it.trackingId == 2) {

                                detected = true
                                Toast.makeText(requireContext(), "Kashif", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        dd.useCustomObjectDetector()

                    } else {
                        Log.e("failed to copy pixel", copyResult.toString())

                    }

                    handlerThread.quitSafely()

                }, Handler(handlerThread.looper))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        arFragment?.onResume()
    }

    override fun onPause() {
        super.onPause()
        arFragment?.onPause()
    }

    override fun onDetach() {
        super.onDetach()
        arFragment?.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
        arFragment?.onDetach()
    }
}




