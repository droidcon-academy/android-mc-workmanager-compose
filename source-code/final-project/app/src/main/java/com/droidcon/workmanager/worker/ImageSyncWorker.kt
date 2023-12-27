package com.droidcon.workmanager.worker

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.droidcon.workmanager.helper.ImageResizeHelper
import com.droidcon.workmanager.helper.NotificationHelper
import com.google.common.util.concurrent.ListenableFuture

class ImageSyncWorker(appContext: Context, params: WorkerParameters) :
    ListenableWorker(appContext, params) {
    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture {
            val imagePath =
                inputData.getString(ImageResizerWorkerCoroutine.KEY_RESIZED_IMAGE_PATH) ?: ""

            imagePath.ifEmpty {
                it.set(Result.failure())
            }

            ImageResizeHelper.scanFile(applicationContext, imagePath) { path, uri ->
                NotificationHelper.createNotification(
                    applicationContext,
                    "Image synced with Gallery"
                )

                it.set(Result.success())
            }
        }
    }
}