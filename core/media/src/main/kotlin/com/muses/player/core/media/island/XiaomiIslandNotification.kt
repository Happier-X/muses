package com.muses.player.core.media.island

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Bundle
import com.muses.player.core.data.platform.HyperOsSupport
import org.json.JSONObject

/**
 * 小米超级岛/焦点通知适配层（MeloX 式独立适配：OEM 特性内聚于此，不向共享层渗漏）。
 *
 * 机制（小米官方开发指南 + HyperNotification 逆向字段对齐）：
 * 普通通知 extras 追加 `miui.focus.param`（`param_v2` JSON：焦点模板 + 岛模板），
 * 封面经 `miui.focus.pics` 传 Bitmap；非支持设备/无白名单时 extras 被系统忽略，
 * 原通知行为不变，故默认开也安全。
 *
 * 接入点：PlaybackService 的 Media3 通知 provider 在 createNotification 返回前调
 * [decorate]（标题/艺术家/封面直接取通知自带值，与歌词通知模式的替换结果自动一致；
 * largeIcon 就绪后 Media3 会重建通知，岛参数随之刷新，无需单独监听）。
 *
 * 白名单现实：焦点通知需向小米申请（mipush-permission@xiaomi.com，包名+appid+channel），
 * 未过审设备上岛不显示但通知正常——此为系统侧限制，非本适配可绕过。设置页提供可选开关。
 */
object XiaomiIslandNotification {

    /** 岛参数主 key（JSON 字符串） */
    const val EXTRA_PARAM = "miui.focus.param"

    /** 岛图片包（Bitmap Parcelable 集合） */
    const val EXTRA_PICS = "miui.focus.pics"

    /** 状态栏 ticker 图 */
    const val PIC_TICKER = "miui.focus.pic_ticker"

    /** 岛封面图（大岛图文区 + 小岛图标） */
    const val PIC_COVER = "miui.focus.pic_cover"

    /** 岛用封面边长（Binder 1M 上限，128px≈64KB，大岛小图够用） */
    const val COVER_SIZE_PX = 128

    /**
     * 给 Media3 媒体通知追加岛参数；返回 true = 已附加（设备支持 + 开关开 + 写入成功）。
     * 全程不抛异常——适配失败只影响岛显示，绝不能把播放通知搞崩。
     */
    fun decorate(context: Context, notification: Notification, enabled: Boolean): Boolean {
        if (!enabled || !HyperOsSupport.isHyperOS()) return false
        return runCatching {
            val extras = notification.extras ?: return false
            val title = extras.getString(Notification.EXTRA_TITLE).orEmpty()
            val artist = extras.getString(Notification.EXTRA_TEXT).orEmpty()
            if (title.isEmpty() && artist.isEmpty()) return false
            val cover = iconToBitmap(context, notification.getLargeIcon())
            extras.putString(EXTRA_PARAM, buildParamJson(title, artist, cover != null))
            if (cover != null) {
                extras.putBundle(
                    EXTRA_PICS,
                    Bundle().apply {
                        putParcelable(PIC_TICKER, cover)
                        putParcelable(PIC_COVER, cover)
                    },
                )
            }
            true
        }.getOrDefault(false)
    }

    /**
     * param_v2 信封：V2 焦点模板字段（ticker/baseInfo）OS2 可用，param_island 岛模板
     * OS3 可用——同一份 JSON 双向兼容（版本信息页口径：OS3 支持更丰富的展开态模板，
     * 选用两代通用的图文模板即同样式展示）。
     *
     * - business：运营场景（统计维度），音乐场景填 music；
     * - enableFloat=false：切歌频繁，不自动弹大岛打扰；
     * - updatable=true：切歌/暂停等通知更新时刷新岛内容。
     */
    private fun buildParamJson(title: String, artist: String, hasCover: Boolean): String {
        val param = JSONObject()
            .put("protocol", 1)
            .put("business", "music")
            .put("enableFloat", false)
            .put("updatable", true)
            .put("ticker", title)
            .put(
                "baseInfo",
                JSONObject()
                    .put("type", 1)
                    .put("title", title)
                    .put("content", artist),
            )
        if (hasCover) {
            param.put("tickerPic", PIC_TICKER)
        }
        val island = JSONObject()
            .put("business", "music")
            .put("islandProperty", 1)
        val imageText = JSONObject().put("type", 1)
        if (hasCover) {
            imageText.put(
                "picInfo",
                JSONObject().put("type", 1).put("pic", PIC_COVER),
            )
        }
        imageText.put(
            "textInfo",
            JSONObject().put("title", title).put("content", artist),
        )
        island.put("bigIslandArea", JSONObject().put("imageTextInfoLeft", imageText))
        if (hasCover) {
            island.put(
                "smallIslandArea",
                JSONObject().put(
                    "picInfo",
                    JSONObject().put("type", 1).put("pic", PIC_COVER),
                ),
            )
        }
        param.put("param_island", island)
        return JSONObject().put("param_v2", param).toString()
    }

    /** largeIcon → 缩放 Bitmap（API 安全写法，minSdk 26 无 toBitmap 扩展依赖问题） */
    private fun iconToBitmap(context: Context, icon: Icon?): Bitmap? {
        if (icon == null) return null
        return runCatching {
            val drawable = icon.loadDrawable(context) ?: return null
            val rawW = drawable.intrinsicWidth.takeIf { it > 0 } ?: COVER_SIZE_PX
            val rawH = drawable.intrinsicHeight.takeIf { it > 0 } ?: COVER_SIZE_PX
            val src = Bitmap.createBitmap(rawW, rawH, Bitmap.Config.ARGB_8888)
            Canvas(src).also {
                drawable.setBounds(0, 0, rawW, rawH)
                drawable.draw(it)
            }
            if (rawW <= COVER_SIZE_PX && rawH <= COVER_SIZE_PX) {
                src
            } else {
                val dstH = (COVER_SIZE_PX.toFloat() * rawH / rawW).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(src, COVER_SIZE_PX, dstH, true).also { scaled ->
                    if (scaled !== src) src.recycle()
                }
            }
        }.getOrNull()
    }
}
