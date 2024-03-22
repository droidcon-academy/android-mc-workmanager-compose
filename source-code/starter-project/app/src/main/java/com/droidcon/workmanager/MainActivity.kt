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
import coil.compose.AsyncImage
import com.droidcon.workmanager.helper.NotificationHelper
import com.droidcon.workmanager.helper.WorkerType
import com.droidcon.workmanager.ui.theme.WorkManagerTheme
import kotlinx.coroutines.CoroutineScope

class MainActivity : ComponentActivity() {
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
                when (workerType) {
                    is WorkerType.Worker -> {
                        startImageResizer()
                    }

                    is WorkerType.CoroutineWorker -> {
                        startImageResizerCoroutine()
                    }

                    is WorkerType.ListenableFutureWorker -> {
                        startImageResizerListenableFuture()
                    }

                    is WorkerType.RxWorker -> {
                        startImageResizerRx()
                    }

                    is WorkerType.ChainedWork -> {
                        startImageResizerChainedWork()
                    }

                    is WorkerType.RetryingWork -> {
                        startImageResizerWithRetry()
                    }

                    is WorkerType.ConstrainedWork -> {
                        startImageResizerWithConstraints()
                    }

                    is WorkerType.PeriodicWork -> {
                        startImageResizerPeriodically()
                    }

                    is WorkerType.CancelWork -> {
                        cancelImageResizerWorker()
                    }

                    is WorkerType.ExpeditedWork -> {
                        startImageResizerExpedited()
                    }

                    is WorkerType.ForegroundWork -> {
                        startImageResizerForeground()
                    }

                    is WorkerType.ObservableWork -> {
                        if (isTaskCompleted) {
                            return@ImageResizer
                        }

                        startImageResizerObservable(coroutineScope) { progress ->
                            taskProgress = progress
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ImageResizer(
        modifier: Modifier = Modifier,
        progress: Float,
        onResizeClick: (workerType: WorkerType) -> Unit
    ) {
        val workerTypesList = listOf(
            WorkerType.Worker(),
            WorkerType.CoroutineWorker(),
            WorkerType.ListenableFutureWorker(),
            WorkerType.RxWorker(),
            WorkerType.ChainedWork(),
            WorkerType.RetryingWork(),
            WorkerType.ConstrainedWork(),
            WorkerType.PeriodicWork(),
            WorkerType.CancelWork(),
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
    }

    private fun startImageResizerCoroutine() {
    }

    private fun startImageResizerListenableFuture() {
    }

    private fun startImageResizerRx() {
    }

    private fun startImageResizerChainedWork() {
    }

    private fun startImageResizerWithRetry() {
    }

    private fun startImageResizerWithConstraints() {
    }

    private fun startImageResizerPeriodically() {
    }

    private fun cancelImageResizerWorker() {
    }

    private fun startImageResizerExpedited() {
    }

    private fun startImageResizerForeground() {
    }

    private fun startImageResizerObservable(
        coroutineScope: CoroutineScope,
        onProgressUpdated: (Float) -> Unit
    ) {
    }
}