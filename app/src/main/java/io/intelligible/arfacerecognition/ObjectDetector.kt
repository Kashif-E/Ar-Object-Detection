package io.intelligible.arfacerecognition

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class ObjectDetector(private val image: Bitmap, private val idAnalyxer: IdAnalyzer) {
    private val localModel by lazy {
        LocalModel.Builder()
            .setAssetFilePath(/*Insert your tflite model here"cus.tflite"*/)
            // or .setAbsoluteFilePath("absolute_file_path_to_tflite_model")
            .build()
    }

    private val options by lazy {
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()
    }

    // [END create_custom_options]
    private val objectDetector by lazy { ObjectDetection.getClient(options) }
    private var count = 0

    fun useCustomObjectDetector() {


        if (!Constants.iscRunning) {
            objectDetector.process(InputImage.fromBitmap(image, 0))
                .addOnSuccessListener { results ->

                    Constants.iscRunning = true
                    Log.e("labels", "${results.size}")
                    results?.forEach {
                        if (it.labels.size > 0) {
                            idAnalyxer(it)
                        }

                    }

                }
                .addOnFailureListener { e ->

                    Constants.iscRunning = false
                    e.printStackTrace()

                }.addOnCompleteListener {
                    Constants.iscRunning = false
                }
        }

    }


    // [END process_image]


    // [END read_results_custom]
}
