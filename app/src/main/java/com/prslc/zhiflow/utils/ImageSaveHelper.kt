package com.prslc.zhiflow.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object ImageSaveHelper {

    suspend fun saveImageToGallery(context: Context, url: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val loader = SingletonImageLoader.get(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()

                val result = loader.execute(request)

                if (result is SuccessResult) {
                    val diskCache = loader.diskCache
                    val cacheKey = result.diskCacheKey

                    val cacheFile = if (diskCache != null && cacheKey != null) {
                        diskCache.openSnapshot(cacheKey)?.use { snapshot ->
                            snapshot.data.toFile().takeIf { it.exists() }
                        }
                    } else null

                    if (cacheFile != null) {
                        saveFileToMediaStore(context, cacheFile, url)
                    } else {
                        Result.failure(Exception("Unable to find cache file"))
                    }
                } else {
                    Result.failure(Exception("Image failed to load"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun saveFileToMediaStore(
        context: Context,
        sourceFile: File,
        url: String
    ): Result<Unit> {
        val extension = if (url.contains(".gif", ignoreCase = true)) "gif" else "jpg"
        val mimeType = if (extension == "gif") "image/gif" else "image/jpeg"
        val fileName = "ZhiFlow_${System.currentTimeMillis()}.$extension"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ZhiFlow"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val imageUri: Uri? = resolver.insert(collection, values)

        return try {
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(output)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                Result.success(Unit)
            } ?: Result.failure(Exception("Unable to create MediaStore record"))
        } catch (e: Exception) {
            imageUri?.let { resolver.delete(it, null, null) }
            Result.failure(e)
        }
    }
}