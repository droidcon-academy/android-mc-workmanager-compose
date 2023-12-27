package com.droidcon.workmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.droidcon.workmanager.helper.NotificationHelper
import com.droidcon.workmanager.helper.WorkerType
import com.droidcon.workmanager.ui.theme.WorkManagerTheme
import com.droidcon.workmanager.worker.ImageResizerFailingWorker
import com.droidcon.workmanager.worker.ImageResizerForegroundWorker
import com.droidcon.workmanager.worker.ImageResizerListenableFuture
import com.droidcon.workmanager.worker.ImageResizerObservableWorker
import com.droidcon.workmanager.worker.ImageResizerWorker
import com.droidcon.workmanager.worker.ImageResizerWorkerCoroutine
import com.droidcon.workmanager.worker.ImageResizerWorkerRx
import com.droidcon.workmanager.worker.ImageSyncWorker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val workManager by lazy {
        WorkManager.getInstance(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Resizer()
                }
            }
        }

        NotificationHelper.checkAndAskForNotificationPermission(this)
    }

    @Composable
    private fun Resizer() {
        var taskProgress by remember {
            mutableStateOf(0f)
        }

        var outputPath by remember {
            mutableStateOf("")
        }

        var isTaskCompleted by remember {
            mutableStateOf(false)
        }

        val coroutineScope = rememberCoroutineScope()

        if (isTaskCompleted) {
            ImageOutputPreview(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                outputPath = outputPath
            ) {
                taskProgress = 0f
                outputPath = ""
                isTaskCompleted = false
            }
        } else {
            ImageResizer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                progress = taskProgress
            ) { workerType ->
                    when(workerType) {
                        is WorkerType.CoroutineWorker -> {
                            startImageResizerCoroutine()
                        }
                        is WorkerType.ExpeditedWork -> {
                            startImageResizerExpedited()
                        }
                        is WorkerType.ForegroundWork -> {
                            startImageResizerForeground()
                        }
                        is WorkerType.ListenableFutureWorker -> {
                            startImageResizerListenableFuture()
                        }
                        is WorkerType.ChainedWork -> {
                            startImageResizerChainedWork()
                        }
                        is WorkerType.PeriodicWork ->  {
                            startImageResizerPeriodically()
                        }
                        is WorkerType.RetryingWork ->  {
                            startImageResizerWithRetry()
                        }
                        is WorkerType.ConstrainedWork ->  {
                            startImageResizerWithConstraints()
                        }
                        is WorkerType.RxWorker ->  {
                            startImageResizerRx()
                        }
                        is WorkerType.Worker ->  {
                            startImageResizer()
                        }
                        is WorkerType.ObservableWork -> {
                            if (isTaskCompleted) {
                                return@ImageResizer
                            }

                            coroutineScope.launch {
                                startImageResizerObservable({ progress ->
                                    taskProgress = progress
                                }) { path, isCompleted ->
                                    if (isCompleted && !path.isNullOrBlank()) {
                                        outputPath = path
                                    }

                                    isTaskCompleted = isCompleted
                                }
                            }
                        }
                    }
            }
        }
    }

    @Composable
    fun ImageResizer(modifier: Modifier = Modifier, progress: Float, onResizeClick: (workerType: WorkerType) -> Unit) {
        val workerTypesList = listOf(
            WorkerType.Worker(),
            WorkerType.CoroutineWorker(),
            WorkerType.ListenableFutureWorker(),
            WorkerType.RxWorker(),
            WorkerType.ChainedWork(),
            WorkerType.RetryingWork(),
            WorkerType.ConstrainedWork(),
            WorkerType.PeriodicWork(),
            WorkerType.ExpeditedWork(),
            WorkerType.ForegroundWork(),
            WorkerType.ObservableWork(),
        )

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                text = "Resize Image",
                textAlign = TextAlign.Center,
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, true)
                    .padding(top = 16.dp, bottom = 16.dp),
                painter = painterResource(id = R.drawable.image),
                contentDescription = "Image To Resize"
            )
            Column {
                workerTypesList.forEach { workerType ->
                    ElevatedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 2.dp, bottom = 2.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            onResizeClick(workerType)
                        }
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            text = workerType.ctaText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                        top = 16.dp, bottom = 16.dp
                    ),
                progress = progress / 100f
            )
        }
    }

    @Composable
    fun ImageOutputPreview(
        modifier: Modifier = Modifier,
        outputPath: String,
        onBackClicked: () -> Unit
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                text = "Output Image",
                textAlign = TextAlign.Center,
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
            AsyncImage(
                model = outputPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, true)
                    .padding(top = 16.dp, bottom = 16.dp),
                contentDescription = "Resized Image"
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = outputPath,
                textAlign = TextAlign.Center
            )
            ElevatedButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
                onClick = onBackClicked
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    text = "Go Back",
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun startImageResizer() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerWorker>()
            .setInputData(
                workDataOf(
                    ImageResizerWorker.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private suspend fun startImageResizerObservable(
        onProgressUpdated: (Float) -> Unit,
        onStatusChange: (String?, Boolean) -> Unit
    ) {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerObservableWorker>()
            .setInputData(
                workDataOf(
                    ImageResizerObservableWorker.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )

        workManager.getWorkInfoByIdFlow(workId).onEach { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getFloat(
                        ImageResizerObservableWorker.KEY_RESIZE_PROGRESS,
                        0f
                    )

                    onProgressUpdated(progress)
                }

                WorkInfo.State.SUCCEEDED -> {
                    val outputPath = workInfo.outputData.getString(
                        ImageResizerObservableWorker.KEY_RESIZED_IMAGE_PATH
                    ).toString()

                    val isCompleted = true

                    onStatusChange(
                        outputPath,
                        isCompleted
                    )
                }

                WorkInfo.State.CANCELLED -> {
                    onProgressUpdated(0f)
                }

                else -> {}
            }
        }.collect()
    }

    private fun startImageResizerChainedWork() {
        val workId = UUID.randomUUID()
        val syncWorkId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerWorkerCoroutine>()
            .setInputData(
                workDataOf(
                    ImageResizerWorkerCoroutine.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setId(workId)
            .build()

        val imageSyncWorkRequest = OneTimeWorkRequestBuilder<ImageSyncWorker>()
            .setId(syncWorkId)
            .build()

        workManager.beginWith(
            imageResizeWorkRequest
        ).then(imageSyncWorkRequest)
            .enqueue()
    }

    private fun startImageResizerPeriodically() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = PeriodicWorkRequestBuilder<ImageResizerWorker>(
            repeatInterval = 20,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        ).setInputData(
            workDataOf(
                ImageResizerWorker.KEY_INPUT_IMAGE_PATH to R.drawable.image
            )
        ).setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private fun startImageResizerExpedited() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerWorker>()
            .setInputData(
                workDataOf(
                    ImageResizerWorker.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            ).setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private fun startImageResizerForeground() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerForegroundWorker>()
            .setInputData(
                workDataOf(
                    ImageResizerForegroundWorker.KEY_INPUT_IMAGE_PATH to R.drawable.image,
                )
            )
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private fun startImageResizerWithRetry() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerFailingWorker>()
            .setInputData(
                workDataOf(
                    ImageResizerFailingWorker.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setId(workId)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                2,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private fun startImageResizerWithConstraints() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerWorker>()
            .setInputData(
                workDataOf(
                    ImageResizerWorker.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresCharging(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private fun startImageResizerRx() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerWorkerRx>()
            .setInputData(
                workDataOf(
                    ImageResizerWorkerRx.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private fun startImageResizerListenableFuture() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerListenableFuture>()
            .setInputData(
                workDataOf(
                    ImageResizerListenableFuture.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }

    private fun startImageResizerCoroutine() {
        val workId = UUID.randomUUID()

        val imageResizeWorkRequest = OneTimeWorkRequestBuilder<ImageResizerWorkerCoroutine>()
            .setInputData(
                workDataOf(
                    ImageResizerWorkerCoroutine.KEY_INPUT_IMAGE_PATH to R.drawable.image
                )
            )
            .setId(workId)
            .build()

        workManager.enqueue(
            imageResizeWorkRequest
        )
    }
}