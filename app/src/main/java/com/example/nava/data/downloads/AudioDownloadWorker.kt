package com.example.nava.data.downloads

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AudioDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val trackId = inputData.getString(KEY_TRACK_ID) ?: return Result.failure()
        val signedUrl = inputData.getString(KEY_SIGNED_URL) ?: return Result.failure()
        val destination = File(applicationContext.filesDir, "offline-audio/$trackId.mp3").apply { parentFile?.mkdirs() }
        val temp = File(destination.parentFile, "${destination.name}.part")
        val connection = URL(signedUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.inputStream.use { input -> temp.outputStream().use { input.copyTo(it) } }
        if (!temp.renameTo(destination)) error("Unable to save offline audio")
        val database = Room.databaseBuilder(applicationContext, NavaDatabase::class.java, "nava.db").build()
        database.offlineTrackDao().upsert(
            OfflineTrackEntity(
                trackId = trackId,
                title = inputData.getString(KEY_TITLE).orEmpty(),
                artistName = inputData.getString(KEY_ARTIST).orEmpty(),
                coverImageUrl = inputData.getString(KEY_COVER).orEmpty(),
                audioPath = destination.absolutePath,
                downloadedAt = System.currentTimeMillis(),
                byteCount = destination.length(),
            ),
        )
        database.close()
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val KEY_TRACK_ID = "track_id"; const val KEY_SIGNED_URL = "signed_url"
        const val KEY_TITLE = "title"; const val KEY_ARTIST = "artist"; const val KEY_COVER = "cover"
    }
}
