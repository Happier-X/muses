package com.muses.player

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

@CapacitorPlugin(
    name = "AudioPlayer",
    permissions = [
        Permission(
            strings = [Manifest.permission.POST_NOTIFICATIONS],
            alias = "notifications",
        ),
    ],
)
class AudioPlayerPlugin : Plugin() {
    @PluginMethod
    fun ensureNotificationPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            call.resolve(JSObject().put("granted", true))
            return
        }
        if (getPermissionState("notifications") == PermissionState.GRANTED) {
            call.resolve(JSObject().put("granted", true))
            return
        }
        requestPermissionForAlias("notifications", call, "onNotificationPermissionResult")
    }

    @PermissionCallback
    private fun onNotificationPermissionResult(call: PluginCall) {
        val granted = getPermissionState("notifications") == PermissionState.GRANTED
        call.resolve(JSObject().put("granted", granted))
    }

    @PluginMethod
    fun prepareLocalAudioFile(call: PluginCall) {
        val uriValue = call.getString("uri")
        if (uriValue.isNullOrBlank()) {
            call.reject("缺少本地音频地址。", "missingUri")
            return
        }

        if (!uriValue.startsWith("content://")) {
            call.resolve(JSObject().put("uri", uriValue))
            return
        }

        bridge.execute {
            try {
                val preparedUri = copyContentUriToPlaybackCache(Uri.parse(uriValue), call.getString("songId") ?: uriValue)
                call.resolve(JSObject().put("uri", preparedUri))
            } catch (exception: Exception) {
                call.reject("本地音频文件不可访问，请重新扫描或重新授权。", "contentUriNotFound", exception)
            }
        }
    }

    private fun copyContentUriToPlaybackCache(uri: Uri, cacheKey: String): String {
        val cacheDir = File(context.cacheDir, "native-audio-playback").apply { mkdirs() }
        val cachedFile = File(cacheDir, "${sha256(cacheKey)}.audio")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cachedFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("contentUriNotFound")

        if (cachedFile.length() <= 0L) {
            throw IllegalArgumentException("contentUriNotFound")
        }

        return Uri.fromFile(cachedFile).toString()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * 缓存封面多为 file://.../cache/covers/*.jpg。
     * 部分机型 ContentResolver 打开 file:// 会失败，优先 FileInputStream；
     * content:// 继续走 ContentResolver。失败 resolve dataUrl=null，由前端强制清空旧封面。
     */
    private fun openArtworkInputStream(uri: Uri): InputStream? {
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path
                if (path.isNullOrBlank()) {
                    null
                } else {
                    val file = File(path)
                    if (file.isFile && file.canRead()) FileInputStream(file) else null
                }
            }
            else -> context.contentResolver.openInputStream(uri)
        }
    }

    @PluginMethod
    fun prepareArtworkDataUrl(call: PluginCall) {
        val uriValue = call.getString("uri")
        if (uriValue.isNullOrBlank()) {
            call.resolve(JSObject().put("dataUrl", null as String?))
            return
        }

        bridge.execute {
            try {
                val uri = Uri.parse(uriValue)
                openArtworkInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                        ?: throw IllegalArgumentException("unsupportedImage")
                    val outputStream = ByteArrayOutputStream()
                    if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)) {
                        throw IllegalArgumentException("compressFailed")
                    }
                    val encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    call.resolve(JSObject().put("dataUrl", "data:image/jpeg;base64,$encoded"))
                } ?: throw IllegalArgumentException("artworkNotFound")
            } catch (exception: Exception) {
                call.resolve(JSObject().put("dataUrl", null as String?))
            }
        }
    }
}
