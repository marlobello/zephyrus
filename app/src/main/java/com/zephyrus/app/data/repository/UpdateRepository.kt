package com.zephyrus.app.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.longPreferencesKey
import com.zephyrus.app.BuildConfig
import com.zephyrus.app.data.model.GitHubRelease
import com.zephyrus.app.data.remote.GitHubApiService
import com.zephyrus.app.domain.model.UpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val gitHubApi: GitHubApiService,
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val THROTTLE_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val APK_FILE_NAME = "zephyrus-update.apk"
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_CHECK = "last_update_check"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check GitHub for a newer release. Returns [UpdateInfo] if available, null if up-to-date.
     * Respects a 24h throttle unless [forceCheck] is true.
     */
    suspend fun checkForUpdate(forceCheck: Boolean = false): Result<UpdateInfo?> {
        if (!forceCheck) {
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
            val elapsed = System.currentTimeMillis() - lastCheck
            if (elapsed < THROTTLE_MS) {
                Timber.d("Update check throttled — last check %d ms ago", elapsed)
                return Result.success(null)
            }
        }

        return try {
            Timber.d("Checking GitHub for updates (force=%s)", forceCheck)
            val release = gitHubApi.getLatestRelease()
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            val remoteVersion = release.tagName.removePrefix("v")
            val localVersion = BuildConfig.VERSION_NAME

            if (isNewerVersion(remoteVersion, localVersion)) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    val info = UpdateInfo(
                        version = remoteVersion,
                        releaseNotes = release.body.take(500),
                        apkUrl = apkAsset.browserDownloadUrl,
                        htmlUrl = release.htmlUrl,
                    )
                    Timber.i("Update available: %s → %s", localVersion, remoteVersion)
                    Result.success(info)
                } else {
                    Timber.w("Release %s found but no APK asset attached", remoteVersion)
                    Result.success(null)
                }
            } else {
                Timber.d("App is up to date (%s)", localVersion)
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for updates")
            Result.failure(e)
        }
    }

    /**
     * Download the APK via DownloadManager and trigger system install when complete.
     */
    fun downloadAndInstall(apkUrl: String) {
        Timber.d("Starting APK download: %s", apkUrl)

        // Clean up any previous download
        val updatesDir = File(context.getExternalFilesDir(null), "updates")
        updatesDir.mkdirs()
        val apkFile = File(updatesDir, APK_FILE_NAME)
        if (apkFile.exists()) apkFile.delete()

        val downloadManager = context.getSystemService<DownloadManager>()
            ?: run {
                Timber.e("DownloadManager not available")
                return
            }

        val request = DownloadManager.Request(android.net.Uri.parse(apkUrl))
            .setTitle("Zephyrus Update")
            .setDescription("Downloading new version…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "updates/$APK_FILE_NAME")

        val downloadId = downloadManager.enqueue(request)
        Timber.d("Download enqueued with ID: %d", downloadId)

        // Register receiver for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Timber.d("Download complete, launching installer")
                    ctx.unregisterReceiver(this)
                    launchInstaller(apkFile)
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED,
        )
    }

    private fun launchInstaller(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch installer")
        }
    }

    /**
     * Semantic version comparison. Returns true if [remote] is newer than [local].
     */
    internal fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
