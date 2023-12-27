package com.droidcon.workmanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.droidcon.workmanager.helper.ImageResizeHelper
import com.droidcon.workmanager.helper.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageResizerFailingWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val inputImageId = inputData.getInt(KEY_INPUT_IMAGE_PATH, -1)

            if (inputImageId == -1) return@withContext Result.failure()

            val resizedImagePath = ImageResizeHelper.resizeBitmap(
                applicationContext, inputImageId, 100, 100
            )

            if (ImageResizeHelper.failureCount < 2) {
                ImageResizeHelper.failureCount++

                NotificationHelper.createNotification(
                    applicationContext,
                    "Worker failed and Retrying ${ImageResizeHelper.failureCount} times"
                )

                return@withContext Result.retry()
            }

            NotificationHelper.createNotification(
                applicationContext,
                "Image saved at: $resizedImagePath"
            )

            return@withContext Result.success(
                workDataOf(
                    KEY_RESIZED_IMAGE_PATH to resizedImagePath
                )
            )
        }
    }

    companion object {
        const val KEY_INPUT_IMAGE_PATH = "KEY_INPUT_IMAGE_PATH"
        const val KEY_RESIZED_IMAGE_PATH = "KEY_RESIZED_IMAGE_PATH"
        const val KEY_RESIZE_PROGRESS = "KEY_RESIZE_PROGRESS"
    }
}