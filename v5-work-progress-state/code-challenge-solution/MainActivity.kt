workManager.getWorkInfoByIdFlow(workId!!).onEach { workinfo ->
    when(workinfo?.state) {
        WorkInfo.State.ENQUEUED -> {}
        WorkInfo.State.RUNNING -> {
            val progress = workinfo?.progress?.getFloat(
                AppConstants.IMAGE_RESIZER_PROGRESS, 0f
            )
            onProgressUpdated(progress ?: 0f)
        }
        WorkInfo.State.SUCCEEDED -> {
            onProgressUpdated(100f)
        }
        WorkInfo.State.FAILED -> {
            onProgressUpdated(0f)
        }
        WorkInfo.State.BLOCKED -> {}
        WorkInfo.State.CANCELLED -> {}
        null -> {}
    }
}.collect()