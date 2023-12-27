package com.droidcon.workmanager.worker

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.droidcon.workmanager.helper.ImageResizeHelper
import com.droidcon.workmanager.helper.NotificationHelper
import com.google.common.util.concurrent.ListenableFuture

class ImageResizerListenableFuture(context: Context, workerParams: WorkerParameters) :
    ListenableWorker(context, workerParams) {

    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture {
            val inputImageId = inputData.getInt(KEY_INPUT_IMAGE_PATH, -1)

            if (inputImageId == -1) it.set(Result.failure())

            val resizedImagePath = ImageResizeHelper.resizeBitmap(
                applicationContext, inputImageId, 100, 100
            )

            NotificationHelper.createNotification(
                applicationContext,
                "Image saved at: $resizedImagePath"
            )

            it.set(Result.success())
        }
    }

    companion object {
        const val KEY_INPUT_IMAGE_PATH = "KEY_INPUT_IMAGE_PATH"
    }
}