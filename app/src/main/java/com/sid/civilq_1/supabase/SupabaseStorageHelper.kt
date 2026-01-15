package com.sid.civilq_1.supabase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sid.civilq_1.SupabaseModule.kt.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseStorageHelper {
    private const val BUCKET_NAME = "report-image"

    /**
     * Uploads an image to Supabase Storage and returns the public URL
     */
    suspend fun uploadReportImage(
        context: Context,
        imageUri: Uri,
        reportId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("SupabaseStorage", "Starting image upload for report: $reportId")

            // 1. Get file from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Unable to open image file"))

            val fileBytes = inputStream.readBytes()
            inputStream.close()

            Log.d("SupabaseStorage", "Read ${fileBytes.size} bytes from image")

            // 2. Create unique filename
            val fileName = "reports/$reportId/${System.currentTimeMillis()}.jpg"

            // 3. Upload to Supabase Storage
            Log.d("SupabaseStorage", "Uploading to bucket: $BUCKET_NAME, path: $fileName")
            SupabaseClient.client.storage.from(BUCKET_NAME).upload(
                path = fileName,
                data = fileBytes,
                upsert = false
            )

            Log.d("SupabaseStorage", "Upload successful")

            // 4. Get public URL
            val publicUrl = SupabaseClient.client.storage.from(BUCKET_NAME).publicUrl(fileName)

            Log.d("SupabaseStorage", "Public URL: $publicUrl")

            Result.success(publicUrl)

        } catch (e: Exception) {
            Log.e("SupabaseStorage", "Upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes an image from Supabase Storage
     */
    suspend fun deleteReportImage(imagePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("SupabaseStorage", "Deleting image: $imagePath")

            // Extract the path from the full URL if needed
            val path = if (imagePath.contains("/storage/v1/object/public/$BUCKET_NAME/")) {
                imagePath.substringAfter("/storage/v1/object/public/$BUCKET_NAME/")
            } else {
                imagePath
            }

            SupabaseClient.client.storage.from(BUCKET_NAME).delete(path)

            Log.d("SupabaseStorage", "Delete successful")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SupabaseStorage", "Delete failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

