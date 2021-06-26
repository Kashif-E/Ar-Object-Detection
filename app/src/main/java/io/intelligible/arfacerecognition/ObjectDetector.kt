package io.intelligible.arfacerecognition

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.PredefinedCategory
import kotlinx.coroutines.runBlocking

class ObjectDetector(private val image: Bitmap, private val idAnalyxer: IdAnalyzer) {
    private val localModel by lazy {
        LocalModel.Builder()
            .setAssetFilePath("model.tflite")
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

    fun useCustomObjectDetector() {
        runBlocking {
            objectDetector.process(InputImage.fromBitmap(image, 0))
                .addOnSuccessListener { results ->

                    if (results.isEmpty().not()) {
                        idAnalyxer(results[0])
                    }
                }
                .addOnFailureListener { e ->

                    e.printStackTrace()

                }
        }

    }
    // [END process_image]


    // [END read_results_custom]
}
