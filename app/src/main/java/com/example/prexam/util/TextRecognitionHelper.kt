package com.example.prexam.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class OcrResult(
    val fullText: String,
    val candidateDrugNames: List<String>,
    val primaryCandidate: String
)

object TextRecognitionHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        onSuccess: (OcrResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        processInputImage(image, onSuccess, onFailure)
    }

    fun recognizeTextFromUri(
        context: Context,
        uri: Uri,
        onSuccess: (OcrResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            processInputImage(image, onSuccess, onFailure)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private fun processInputImage(
        image: InputImage,
        onSuccess: (OcrResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                val candidates = extractDrugNameCandidates(fullText)
                val primary = candidates.firstOrNull() ?: fullText.lines().firstOrNull()?.trim() ?: ""

                onSuccess(
                    OcrResult(
                        fullText = fullText,
                        candidateDrugNames = candidates,
                        primaryCandidate = primary
                    )
                )
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    private fun extractDrugNameCandidates(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val candidates = mutableListOf<String>()

        // Common dosage / unit noise words to strip
        val noisePattern = Regex("(?i)\\b(\\d+\\s*(mg|g|mcg|ml|tablets?|capsules?|solution|syrup|suspension|bp|usp|ip|film|coated|oral|daily|dose|rx|express))\\b")

        for (line in lines) {
            val cleanLine = line.replace(noisePattern, "").replace(Regex("[^a-zA-Z\\s-]"), "").trim()
            val words = cleanLine.split(Regex("\\s+")).filter { it.length >= 3 }
            for (word in words) {
                if (word.lowercase() !in setOf("tablets", "capsules", "pharma", "labs", "ltd", "inc", "group", "take", "keep")) {
                    candidates.add(word.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }

        return candidates.distinct()
    }
}
