package com.sid.civilq_1.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.sid.civilq_1.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class GeminiHelper {

    private val config = generationConfig {
        temperature = 0.4f
        topK = 32
        topP = 1f
        maxOutputTokens = 150
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash", // Use stable model naming
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = config
    )

    suspend fun generateReportDescription(
        context: Context,
        imageUri: Uri,
        department: String,
        title: String,
        location: Location
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.GEMINI_API_KEY == "null") {
                return@withContext Result.failure(Exception("API Key missing"))
            }

            val imageBitmap = getDownscaledBitmap(context, imageUri)
                ?: return@withContext Result.failure(Exception("Image processing failed"))

            val prompt = """
                Describe this civic issue for a formal complaint.
                Title: $title
                Dept: $department
                Language: Use the local language for coordinates ${location.latitude}, ${location.longitude}.
                Constraint: 30-50 words. Objective tone. No greetings.
            """.trimIndent()

            val inputContent = content {
                image(imageBitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val resultText = response.text

            if (!resultText.isNullOrBlank()) {
                Result.success(resultText)
            } else {
                Result.failure(Exception("AI returned no text"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDownscaledBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val targetSize = 1024
            var inSampleSize = 1
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                    inSampleSize *= 2
                }
            }

            val finalStream = context.contentResolver.openInputStream(uri)
            val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val bitmap = BitmapFactory.decodeStream(finalStream, null, finalOptions)
            finalStream?.close()

            bitmap
        } catch (e: Exception) {
            null
        }
    }
}