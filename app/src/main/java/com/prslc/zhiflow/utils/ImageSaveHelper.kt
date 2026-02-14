package com.prslc.zhiflow.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageSaveHelper {
    suspend fun saveImageToGallery(context: Context, url: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 获取 Bitmap (优先从缓存获取)
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false) // 必须关闭硬件位图，否则无法写入 MediaStore
                    .build()

                val result = loader.execute(request)
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    ?: return@withContext Result.failure(Exception("无法获取图片数据"))

                // 2. 准备 MediaStore 插入参数
                val filename = "zhiflow_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    // Android 10 及以上可以指定文件夹
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ZhiFlow")
                        put(MediaStore.MediaColumns.IS_PENDING, 1) // 先标记为处理中
                    }
                }

                // 3. 执行写入
                val contentResolver = context.contentResolver
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("创建 MediaStore 条目失败"))

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // 4. 完成写入
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}