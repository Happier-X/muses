package com.example.muses.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads a downsampled bitmap from a file URI asynchronously off the main thread.
 *
 * Uses [BitmapFactory.decodeFile] with [BitmapFactory.Options.inSampleSize] to
 * avoid loading full-resolution images into memory. A 3000x3000 album art JPEG
 * decoded at full resolution would consume ~36 MB (ARGB_8888); downsampling
 * to the target display size keeps memory usage negligible.
 *
 * The loading runs on [Dispatchers.IO] and the resulting bitmap is delivered
 * via Compose state, causing at most one additional recomposition once loaded.
 *
 * Falls back to null on any decode error.
 *
 * @param albumArtUri  The file URI to load (e.g. "file:///data/.../album_art/{hash}.jpg").
 *                     If null, returns null immediately.
 * @param targetSizePx The target display size in pixels. Used to compute the
 *                     largest power-of-two inSampleSize that results in a
 *                     decoded image >= targetSizePx on each axis. Defaults to 96 px.
 */
@Composable
fun rememberAlbumArt(
    albumArtUri: Uri?,
    targetSizePx: Int = 96
): Bitmap? {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Use a state object as key so we can update it from the coroutine.
    // Initial value is null (placeholder while loading).
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(albumArtUri) {
        if (albumArtUri == null) {
            bitmapState.value = null
            return@LaunchedEffect
        }
        scope.launch {
            val path = albumArtUri.path
            if (path == null) {
                bitmapState.value = null
                return@launch
            }
            val decoded = withContext(Dispatchers.IO) {
                decodeSampledBitmap(path, targetSizePx)
            }
            bitmapState.value = decoded
        }
    }

    return bitmapState.value
}

/**
 * Decodes a bitmap from [filePath] using power-of-two downsampling so that
 * neither decoded dimension is smaller than [targetSizePx].
 *
 * Passes [BitmapFactory.Options.inJustDecodeBounds = true] first to read the
 * image dimensions without allocating pixel memory, then computes the largest
 * power-of-two [inSampleSize] that satisfies the target constraint.
 *
 * Returns null on any decode error (corrupted file, OOM, missing permissions).
 */
private fun decodeSampledBitmap(filePath: String, targetSizePx: Int): Bitmap? {
    return try {
        // Phase 1: read bounds only
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, boundsOptions)

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return null
        }

        // Phase 2: compute inSampleSize
        val inSampleSize = calculateInSampleSize(
            boundsOptions.outWidth,
            boundsOptions.outHeight,
            targetSizePx,
            targetSizePx
        )

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            // Use RGB_565 for album art — 2 bytes/pixel vs 4 for ARGB_8888,
            // perceptually identical for photographic content at small sizes.
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(filePath, decodeOptions)
    } catch (_: Exception) {
        null
    }
}

/**
 * Returns the largest power-of-two [inSampleSize] such that
 * `rawWidth / inSampleSize >= reqWidth` and `rawHeight / inSampleSize >= reqHeight`.
 *
 * Equivalent to `Integer.highestOneBit(max(1, rawWidth / reqWidth, rawHeight / reqHeight))`.
 */
private fun calculateInSampleSize(
    rawWidth: Int,
    rawHeight: Int,
    reqWidth: Int,
    reqHeight: Int
): Int {
    var inSampleSize = 1
    if (rawHeight > reqHeight || rawWidth > reqWidth) {
        val halfHeight = rawHeight / 2
        val halfWidth = rawWidth / 2
        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
