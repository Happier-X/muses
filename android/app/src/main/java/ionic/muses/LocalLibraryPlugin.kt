package ionic.muses

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "LocalLibrary")
class LocalLibraryPlugin : Plugin() {
    // Android SAF 的 content://tree URI 不是普通文件系统路径；这里用 DocumentFile 作为窄桥接层。
    // 前端只依赖递归枚举音频文件和读取元数据两个能力，便于未来替换为成熟插件。
    @PluginMethod
    fun scanDirectory(call: PluginCall) {
        val treeUriValue = call.getString("treeUri")
        if (treeUriValue.isNullOrEmpty()) {
            call.reject("缺少本地目录地址。", "missingTreeUri")
            return
        }

        bridge.execute {
            try {
                val treeUri = Uri.parse(treeUriValue)
                val root = DocumentFile.fromTreeUri(context, treeUri)
                if (root == null || !root.isDirectory) {
                    call.reject("本地目录不可访问。", "invalidDirectory")
                    return@execute
                }

                val files = JSArray()
                collectAudioFiles(root, root.name ?: "", files)

                val result = JSObject()
                result.put("files", files)
                call.resolve(result)
            } catch (exception: Exception) {
                call.reject(exception.message, exception)
            }
        }
    }

    @PluginMethod
    fun readMetadata(call: PluginCall) {
        val uriValue = call.getString("uri")
        if (uriValue.isNullOrEmpty()) {
            call.reject("缺少音频文件地址。", "missingUri")
            return
        }

        bridge.execute {
            try {
                val uri = Uri.parse(uriValue)
                val cacheKey = call.getString("songId") ?: uriValue
                val result = AudioMetadataReader(context).readFromContentUri(uri, cacheKey)
                readSidecarLyrics(uri)?.let { lyrics ->
                    if (!result.has("lyrics")) {
                        result.put("lyrics", lyrics)
                        result.put("lyricsSource", "sidecar")
                    }
                }
                result.put("tagsScanned", true)
                call.resolve(result)
            } catch (exception: Exception) {
                call.reject(exception.message, exception)
            }
        }
    }

    private fun collectAudioFiles(directory: DocumentFile, relativePath: String, files: JSArray) {
        directory.listFiles().forEach { child ->
            val childName = child.name ?: return@forEach
            val childPath = listOf(relativePath, childName).filter { it.isNotBlank() }.joinToString("/")
            when {
                child.isDirectory -> collectAudioFiles(child, childPath, files)
                child.isFile && isSupportedAudioFile(childName) -> {
                    val item = JSObject()
                    item.put("path", childPath)
                    item.put("uri", child.uri.toString())
                    item.put("name", childName)
                    files.put(item)
                }
            }
        }
    }

    private fun readSidecarLyrics(audioUri: Uri): String? {
        val audioFile = DocumentFile.fromSingleUri(context, audioUri) ?: return null
        val audioName = audioFile.name ?: return null
        val parentUri = runCatching { Uri.parse(audioUri.toString().substringBeforeLast("/")) }.getOrNull() ?: return null
        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return null
        val lyricName = audioName.substringBeforeLast('.', audioName) + ".lrc"
        val lyricFile = parent.findFile(lyricName) ?: return null
        return context.contentResolver.openInputStream(lyricFile.uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
        }
    }

    private fun isSupportedAudioFile(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_AUDIO_EXTENSIONS
    }

    private companion object {
        val SUPPORTED_AUDIO_EXTENSIONS = setOf("aac", "aiff", "alac", "ape", "flac", "m4a", "m4b", "mp3", "ogg", "opus", "wav", "wma")
    }
}
