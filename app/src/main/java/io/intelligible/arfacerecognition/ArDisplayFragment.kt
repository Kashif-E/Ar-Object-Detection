package io.intelligible.arfacerecognition

import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.filament.ColorGrading
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.ArFragment.OnViewCreatedListener
import com.google.ar.sceneform.ux.TransformableNode
import com.google.mlkit.vision.objects.DetectedObject
import io.intelligible.arfacerecognition.databinding.ArFragmentBinding
import io.intelligible.arfacerecognition.databinding.ViewnodeRenderBinding
import kotlinx.coroutines.runBlocking


typealias IdAnalyzer = (detectedObject: DetectedObject) -> Unit

class ArDisplayFragment : Fragment(R.layout.ar_fragment),
    OnViewCreatedListener, Scene.OnUpdateListener {

    var count = 0;
    private lateinit var anchorNode: AnchorNode
    private val TAG = "arfragment"
    private val faceInfo = Node()
    private lateinit var binding: ArFragmentBinding
    private var renderable: Renderable? = null
    private var callbackThread = HandlerThread("callback-worker")
    private lateinit var callbackHandler: Handler
    private var viewRenderable: ViewRenderable? = null
    private var detected = false
    private var IsInitialised = false


    private var arFragment: ArFragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = ArFragmentBinding.bind(view)
        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        if (!IsInitialised) {
            callbackThread.start()
            callbackHandler = Handler(callbackThread.looper)
            IsInitialised = true
        }


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


    override fun onUpdate(frameTime: FrameTime?) {


        onUpdateFrame(frameTime)

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


    private fun onUpdateFrame(frameTime: FrameTime?) {
        arFragment!!.arSceneView.arFrame ?: return

        copyPixelFromView(arFragment!!.arSceneView) { bitmap ->

            val targetBitmap = Bitmap.createBitmap(bitmap)


            val dd = ObjectDetector(image = targetBitmap) {

                Toast.makeText(
                    requireContext(),
                    "tracking ${it.trackingId}  ${it.labels[0].text}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("tracking", it.trackingId.toString())

                arFragment!!.arSceneView.scene.removeOnUpdateListener(this)

                    loadModels(it.labels[0].text)



            }
            dd.useCustomObjectDetector()

        }

    }

    private fun copyPixelFromView(views: SurfaceView, callback: (Bitmap) -> Unit) {
        var bitmap = Bitmap.createBitmap(
            views!!.width,
            views!!.height,
            Bitmap.Config.ARGB_8888
        )
        // val view = arFragment?.arSceneView
        PixelCopy.request(views, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                Log.i(TAG, "Copying ArFragment view.")
                callback(bitmap)
                Log.i(TAG, "Copied ArFragment view.")

            } else {
                Log.e(TAG, "Failed to copy ArFragment view.")
            }
        }, callbackHandler)
    }

    private fun loadModels(text: String) {

        val root = ViewnodeRenderBinding.inflate(layoutInflater)
        root.label.text = text
        ViewRenderable.builder()
            .setView(requireContext(), root.root)
            .build()
            .thenAccept { view: ViewRenderable ->
                viewRenderable = view
                viewRenderable!!.isShadowCaster = false
                viewRenderable!!.isShadowReceiver = false
                faceInfo.renderable = viewRenderable
                Toast.makeText(requireContext(), "Model Loaded", Toast.LENGTH_SHORT).show()
                addDrawable()
            }
            .exceptionally {
                Toast.makeText(requireContext(), "Unable to load model", Toast.LENGTH_LONG).show()
                null
            }
    }

    private fun addDrawable() {

        val frame = arFragment!!.arSceneView.arFrame!!
        val hitTest = frame.hitTest(
            frame.screenCenter().x,
            frame.screenCenter().y
        )

        val hitResult = hitTest[0]
        /*     Log.i(
                 TAG, "${hitResult.distance}, " +
                         "${hitResult.hitPose.xAxis.asList()}, " +
                         "${hitResult.hitPose.yAxis.asList()}, " +
                         "${hitResult.hitPose.zAxis.asList()}"
             )*/

        //Create an anchor at the plane hit
        val modelAnchor = arFragment!!
            .arSceneView
            .session!!
            .createAnchor(hitResult.hitPose)

        //Attach a node to this anchor with the scene as the parent
        val anchorNode = AnchorNode(modelAnchor)
        anchorNode.setParent(arFragment!!.arSceneView.scene)

        //create a new TranformableNode that will carry our object
        val transformableNode = TransformableNode(arFragment!!.transformationSystem)
        transformableNode.scaleController.maxScale = 0.5f;
        transformableNode.scaleController.minScale = 0.1f;
        transformableNode.setParent(anchorNode)
        transformableNode.renderable = viewRenderable

        //Alter the real world position to ensure object renders on the table top. Not somewhere inside.
        transformableNode.worldPosition = Vector3(
            modelAnchor.pose.tx(),
            modelAnchor.pose.ty(),
            modelAnchor.pose.tz()
        )
    }

    private fun Frame.screenCenter(): Vector3 {
        val vw = binding.root
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }


}




