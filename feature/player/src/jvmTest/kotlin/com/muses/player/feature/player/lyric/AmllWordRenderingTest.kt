package com.muses.player.feature.player.lyric

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricSyllable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class AmllWordRenderingTest {
    private fun render(
        text: String,
        start: Long,
        end: Long,
        times: List<Long>,
        quality: LyricsRenderingQuality = LyricsRenderingQuality.High,
        reduceMotion: Boolean = true,
        ruby: List<LyricSyllable> = emptyList(),
    ): List<BufferedImage> = runBlocking {
        val time = mutableLongStateOf(times.first())
        val line = LyricLine(start, text = text, syllables = listOf(LyricSyllable(text, start, end, ruby)))
        val scene = ImageComposeScene(width = 640, height = 180, coroutineContext = coroutineContext) {
            Box(Modifier.fillMaxSize().background(Color.Black).padding(32.dp)) {
                AmllWordLyricText(line, { time.longValue }, true, 2f, reduceMotion, 1f, .4f, quality, false)
            }
        }
        try {
            times.mapIndexed { index, value ->
                time.longValue = value
                scene.render(index * 16_000_000L).use { image ->
                    image.encodeToData()!!.use { data -> ImageIO.read(ByteArrayInputStream(data.bytes)) }
                }
            }
        } finally { scene.close() }
    }

    private fun brightness(image: BufferedImage): Long {
        var result = 0L
        for (y in 0 until image.height) for (x in 0 until image.width) result += image.getRGB(x, y) and 255
        return result
    }

    @Test
    fun 注音实际显示且片段停顿期间遮罩冻结() {
        val frames = render("AB", 1000, 3000, listOf(1000, 1600, 2400, 3000, 1000), ruby = listOf(
            LyricSyllable("abc", 1000, 1500), LyricSyllable("def", 2500, 3000),
        ))
        assertEquals(pixels(frames[1]), pixels(frames[2]), "注音片段停顿必须冻结")
        assertEquals(pixels(frames[0]), pixels(frames[4]), "注音倒退不应残留高亮")
        assertTrue(brightness(frames[3]) > brightness(frames[1]))
        val plain = render("AB", 1000, 3000, listOf(3000)).single()
        assertTrue(brightness(frames[3]) > brightness(plain), "主字之外应实际画出注音")
        frames.take(4).forEachIndexed { index, frame -> save("注音-$index", frame) }
    }

    private fun pixels(image: BufferedImage) = image.getRGB(0, 0, image.width, image.height, null, 0, image.width).toList()

    private fun save(name: String, image: BufferedImage) {
        val directory = File("build/reports/amll-words").apply { mkdirs() }
        ImageIO.write(image, "png", File(directory, "$name.png"))
    }

    @Test
    fun 真实绘制的渐变持续推进并可无残影倒退() {
        val frames = render("MMMMMMMM", 1000, 1600, listOf(1000, 1150, 1300, 1450, 1600, 1000))
        val values = frames.map(::brightness)
        assertTrue(values[0] > 0, "未唱部分仍应可见")
        for (index in 0..3) assertTrue(values[index + 1] > values[index], "渐变应持续推进：$values")
        assertTrue(values[4] > values[0] * 2)
        assertEquals(pixels(frames[0]), pixels(frames[5]), "倒退播放不能残留高亮")
        frames.take(5).forEachIndexed { index, frame -> save("遮罩-$index", frame) }
    }

    @Test
    fun 辉光实际绘制且透明度不会被忽略() {
        val high = render("love", 1000, 3000, listOf(2200), LyricsRenderingQuality.High, false).single()
        val low = render("love", 1000, 3000, listOf(2200), LyricsRenderingQuality.Low, false).single()
        assertTrue(brightness(high) > brightness(low), "高画质的强调词应确实画出辉光")
        var haloPixels = 0
        for (y in 0 until high.height) for (x in 0 until high.width) {
            if ((low.getRGB(x, y) and 255) == 0 && (high.getRGB(x, y) and 255) > 0) haloPixels++
        }
        assertTrue(haloPixels > 20, "辉光不能被字形硬裁剪：$haloPixels")
        save("长音辉光", high)
        save("长音无辉光", low)
    }

    @Test
    fun 无动态效果仍保留逐词时间同步() {
        val frames = render("office", 1000, 3000, listOf(1000, 2000, 3000), reduceMotion = true)
        assertTrue(brightness(frames[2]) > brightness(frames[1]))
        assertTrue(brightness(frames[1]) > brightness(frames[0]))
        frames.forEachIndexed { index, frame -> save("减少动态-$index", frame) }
    }
}
