package com.droidcon.workmanager.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.droidcon.workmanager.helper.ImageResizeHelper
import com.droidcon.workmanager.helper.NotificationHelper
import io.reactivex.Single

class ImageResizerWorkerRx(context: Context, workerParams: WorkerParameters) :
    RxWorker(context, workerParams) {

    override fun createWork(): Single<Result> {
        val inputImageId = inputData.getInt(KEY_INPUT_IMAGE_PATH, -1)

        if (inputImageId == -1) return Single.just(Result.failure())

        val resizedImagePath = ImageResizeHelper.resizeBitmap(
            applicationContext, inputImageId, 100, 100
        )

        NotificationHelper.createNotification(
            applicationContext,
            "Image saved at: $resizedImagePath"
        )

        return Single.just(
            Result.success()
        )
    }

    companion object {
        const val KEY_INPUT_IMAGE_PATH = "KEY_INPUT_IMAGE_PATH"
    }
}