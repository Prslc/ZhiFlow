package com.prslc.zhiflow.core.utils.platform

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Immutable
object ImageHelper {

    /**
     * Intercepts the Coil 3 disk cache pipeline to retrieve the downloaded file
     * from the internal sandbox directory.
     *
     * @param context The application or activity context.
     * @param url The target image URL.
     * @return The cached [File], or null if the image is not found in the disk cache.
     */
    private suspend fun getCacheFile(context: Context, url: String): File? =
        withContext(Dispatchers.IO) {
            val loader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .build()

            val result = loader.execute(request)

            if (result is SuccessResult) {
                val diskCache = loader.diskCache
                val cacheKey = result.diskCacheKey

                if (diskCache != null && cacheKey != null) {
                    diskCache.openSnapshot(cacheKey)?.use { snapshot ->
                        val file = snapshot.data.toFile()
                        if (file.exists()) return@withContext file
                    }
                }
            }
            null
        }

    /**
     * Saves an image from the web URL directly into the system's public gallery.
     *
     * @param context The application context.
     * @param url The web image URL used to locate the cache.
     * @return A [Result] indicating success or containing the thrown exception.
     */
    suspend fun saveImageToGallery(context: Context, url: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile(context, url)
                if (cacheFile != null) {
                    saveFileToMediaStore(context, cacheFile, url)
                } else {
                    Result.failure(Exception("Unable to find cache file"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Shares a cached image securely via [FileProvider] to other target apps.
     *
     * To prevent external apps from rejecting the image due to Coil's extensionless
     * cache file naming scheme, this copies the image into a temporary file with a
     * valid extension first.
     *
     * @param context The current context, preferably an Activity context for starting the chooser.
     * @param url The image URL used to find the file and evaluate its MIME type.
     * @return A [Result] indicating success or containing the thrown exception.
     */
    suspend fun shareImage(context: Context, url: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = getCacheFile(context, url)
                if (cacheFile != null) {
                    val extension = if (url.contains(".gif", ignoreCase = true)) "gif" else "jpg"
                    val mimeType = if (extension == "gif") "image/gif" else "image/jpeg"

                    val shareDir = File(context.cacheManagerDir(), "shared_images")
                    if (shareDir.exists()) {
                        shareDir.listFiles()?.forEach { file ->
                            try {
                                file.delete()
                            } catch (e: Exception) {
                                // Do nothing
                            }
                        }
                    } else {
                        if (!shareDir.mkdirs()) {
                            return@withContext Result.failure(Exception("Failed to create shared images directory"))
                        }
                    }
                    val shareFile =
                        File(shareDir, "shared_image_${System.currentTimeMillis()}.$extension")

                    FileInputStream(cacheFile).use { input ->
                        FileOutputStream(shareFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val authority = "${context.applicationContext.packageName}.fileprovider"
                    val shareUri: Uri = FileProvider.getUriForFile(context, authority, shareFile)

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        type = mimeType
                        clipData = ClipData.newRawUri("", shareUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    val shareIntent = Intent.createChooser(sendIntent, null).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startActivity(shareIntent)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Unable to find cache file to share"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Internal abstraction to persist raw binary files into the MediaStore collections database.
     */
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

    /**
     * Helper extension to safely fetch the base cache directory across varied Context wrappers.
     */
    private fun Context.cacheManagerDir(): File = this.cacheDir ?: this.filesDir
}