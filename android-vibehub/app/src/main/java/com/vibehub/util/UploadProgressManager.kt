package com.vibehub.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks active background uploads and emits state changes to any observer.
 * ViewModels collect [uploads] to show per-item progress bars in the UI.
 */
@Singleton
class UploadProgressManager @Inject constructor() {

    enum class UploadStatus { QUEUED, UPLOADING, PROCESSING, SUCCESS, FAILED }

    data class Upload(
        val id: String = UUID.randomUUID().toString(),
        val label: String,
        val status: UploadStatus = UploadStatus.QUEUED,
        val progressPercent: Int = 0,
        val errorMessage: String? = null,
        val resultUrl: String? = null,
    )

    private val _uploads = MutableStateFlow<List<Upload>>(emptyList())
    val uploads: StateFlow<List<Upload>> = _uploads.asStateFlow()

    fun enqueue(label: String): String {
        val upload = Upload(label = label, status = UploadStatus.QUEUED)
        _uploads.update { it + upload }
        return upload.id
    }

    fun setUploading(id: String, percent: Int) {
        _uploads.update { list ->
            list.map { if (it.id == id) it.copy(status = UploadStatus.UPLOADING, progressPercent = percent.coerceIn(0, 100)) else it }
        }
    }

    fun setProcessing(id: String) {
        _uploads.update { list ->
            list.map { if (it.id == id) it.copy(status = UploadStatus.PROCESSING, progressPercent = 100) else it }
        }
    }

    fun setSuccess(id: String, resultUrl: String) {
        _uploads.update { list ->
            list.map { if (it.id == id) it.copy(status = UploadStatus.SUCCESS, progressPercent = 100, resultUrl = resultUrl) else it }
        }
    }

    fun setFailed(id: String, error: String) {
        _uploads.update { list ->
            list.map { if (it.id == id) it.copy(status = UploadStatus.FAILED, errorMessage = error) else it }
        }
    }

    fun dismiss(id: String) {
        _uploads.update { it.filter { u -> u.id != id } }
    }

    fun dismissCompleted() {
        _uploads.update { it.filter { u -> u.status == UploadStatus.UPLOADING || u.status == UploadStatus.QUEUED || u.status == UploadStatus.PROCESSING } }
    }
}
