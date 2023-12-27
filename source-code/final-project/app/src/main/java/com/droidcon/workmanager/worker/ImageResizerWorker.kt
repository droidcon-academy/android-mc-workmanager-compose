package com.droidcon.workmanager.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.droidcon.workmanager.helper.ImageResizeHelper
import com.droidcon.workmanager.helper.NotificationHelper

class ImageResizerWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val inputImageId = inputData.getInt(KEY_INPUT_IMAGE_PATH, -1)

        if (inputImageId == -1) return Result.failure()

        val resizedImagePath = ImageResizeHelper.resizeBitmap(
            applicationContext, inputImageId, 100, 100
        )

        NotificationHelper.createNotification(
            applicationContext,
            "Image saved at: $resizedImagePath"
        )

        return Result.success()
    }


    companion object {
        const val KEY_INPUT_IMAGE_PATH = "KEY_INPUT_IMAGE_PATH"
    }
}