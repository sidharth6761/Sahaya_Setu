package com.sid.civilq_1.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.sid.civilq_1.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiHelper {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateReportDescription(
        context: Context,
        imageUri: Uri,
        department: String,
        title: String,
        location: Location
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if the API key is missing. This is the most common cause of errors.
            if (BuildConfig.GEMINI_API_KEY.isBlank() ) {
                return@withContext Result.failure(Exception("API Key is missing. Please add it to your local.properties file."))
            }

            val imageBitmap = uriToBitmap(context, imageUri)

            val prompt = """
            You are an expert civic issue reporter. Based on the attached image, the report title, and the selected department and the selected location, generate a concise and clear description for a formal complaint in the local language used in their state .
            - The description should be between 30 to 50 words.
            
            - Department: $department
            - Title: $title
            - Location: Latitude ${location.latitude}, Longitude ${location.longitude}
            
            Focus on describing the problem visible in the image objectively. Do not add any conversational text or greetings. Just provide the description.
            """

            val inputContent = content {
                image(imageBitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)

            if (response.text != null) {
                Result.success(response.text!!)
            } else {
                Result.failure(Exception("The AI returned an empty response."))
            }

        } catch (e: Exception) {
            // Provide a more detailed error message to help with debugging
            Result.failure(Exception( "${e.message}", e))
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }
}
