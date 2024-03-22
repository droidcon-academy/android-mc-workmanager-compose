package com.droidcon.workmanager.worker

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.droidcon.workmanager.helper.AppConstants
import com.droidcon.workmanager.helper.ImageResizeHelper
import com.droidcon.workmanager.helper.NotificationHelper
import io.reactivex.Single

class ImageResizerRxWorker(appContext: Context, workerParams: WorkerParameters) :
    RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        val imageId = inputData.getInt(AppConstants.IMAGE_ID, -1)
        val resizedImagePath = ImageResizeHelper.resizeBitmap(
            applicationContext, imageId, 500, 500
        )
        NotificationHelper.createNotification(
            applicationContext,
            text = "Image resized at : $resizedImagePath"
        )

        return Single.just(Result.success())
    }
}