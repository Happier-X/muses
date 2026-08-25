package com.muses.player.core.lyrics.provider.qrc

import java.util.zip.Inflater

/**
 * QQ 音乐 QRC 解密：非标准 3DES（结构类 DES 的私有分组密码）+ zlib inflate。
 *
 * 规格书 = @applemusic-like-lyrics/lyric@1.0.2 dist amll-lyric.cjs 的
 * formats/eqrc/custom-des.ts + eqrc/index.ts（迁移自 github.com/apoint123/qrc-decoder）。
 * 常量表见 [QrcTables]（机械提取，禁止手改）。
 */
object QrcDecoder {

    // 密钥（custom-des.ts KEY_1/2/3）
    private val KEY_1 = "!@#)(*$%".toByteArray(Charsets.UTF_8)
    private val KEY_2 = "123ZXC!@".toByteArray(Charsets.UTF_8)
    private val KEY_3 = "!@#)(NHL".toByteArray(Charsets.UTF_8)

    private const val BITS_28_MASK: Long = 0xFFFFFFF0L

    /**
     * 从 8 字节密钥按置换表取位（模拟 QQ 音乐特有的小端序字节序处理）。
     * custom-des.ts permuteFromKeyBytes。
     */
    private fun permuteFromKeyBytes(key: ByteArray, table: IntArray): Long {
        var output = 0L
        var bitMask = 1L shl (table.size - 1)
        for (pos in table) {
            val wordIndex = pos shr 5
            val bitInWord = pos and 31
            val byteInWord = bitInWord shr 3
            val bitInByte = bitInWord and 7
            if ((key[wordIndex * 4 + 3 - byteInWord].toInt() shr (7 - bitInByte)) and 1 == 1) {
                output = output or bitMask
            }
            bitMask = bitMask shr 1
        }
        return output
    }

    /** 28 位密钥部分循环左移（custom-des.ts rotateLeft28Bit；掩码取 32 位中的高 28 位） */
    private fun rotateLeft28Bit(value: Long, amount: Int): Long {
        val v = value and BITS_28_MASK
        return ((v shl amount) or (v shr (28 - amount))) and BITS_28_MASK
    }

    /** DES 密钥调度：64 位主密钥 → 16 个 48 位轮密钥（存为两个 24 位 int） */
    private fun keySchedule(key: ByteArray, mode: Int): IntArray {
        val schedule = IntArray(32)
        val c0 = permuteFromKeyBytes(key, QrcTables.KEY_PERM_C)
        val d0 = permuteFromKeyBytes(key, QrcTables.KEY_PERM_D)
        var c = c0 shl 4
        var d = d0 shl 4
        for (i in 0 until 16) {
            val shift = QrcTables.KEY_RND_SHIFT[i]
            c = rotateLeft28Bit(c, shift)
            d = rotateLeft28Bit(d, shift)
            val toGen = if (mode == 1) 15 - i else i
            var subkey48 = 0L
            for (k in QrcTables.KEY_COMPRESSION.indices) {
                val pos = QrcTables.KEY_COMPRESSION[k]
                val bit = if (pos < 28) {
                    (c shr (31 - pos)) and 1L
                } else {
                    (d shr (31 - (pos - 27))) and 1L
                }
                if (bit == 1L) subkey48 = subkey48 or (1L shl (47 - k))
            }
            val b5 = ((subkey48 shr 40) and 255).toInt()
            val b4 = ((subkey48 shr 32) and 255).toInt()
            val b3 = ((subkey48 shr 24) and 255).toInt()
            val high24 = (b5 shl 16) or (b4 shl 8) or b3
            val b2 = ((subkey48 shr 16) and 255).toInt()
            val b1 = ((subkey48 shr 8) and 255).toInt()
            val b0 = (subkey48 and 255).toInt()
            val low24 = (b2 shl 16) or (b1 shl 8) or b0
            schedule[toGen * 2] = high24
            schedule[toGen * 2 + 1] = low24
        }
        return schedule
    }

    /** S-盒查找索引（custom-des.ts calculateSboxIndex） */
    private fun sboxIndex(a: Int): Int = a and 32 or ((a and 31) shr 1) or ((a and 1) shl 4)

    /** 非标准 P 盒置换（custom-des.ts applyQqPboxPermutation） */
    private fun applyQqPbox(input: Int): Int {
        var output = 0
        for (i in 0 until 32) {
            val srcBit = QrcTables.P_BOX[i]
            val destMask = 1 shl (31 - i)
            if ((input and (1 shl (32 - srcBit))) != 0) output = output or destMask
        }
        return output
    }

    /** 64 位块按 64 条规则做 IP 类置换（custom-des.ts applyPermutation） */
    private fun applyPermutation(input: Long, rule: IntArray): Long {
        var output = 0L
        for (i in 0 until 64) {
            val srcBit1Based = rule[i]
            if (((input shr (64 - srcBit1Based)) and 1L) == 1L) {
                output = output or (1L shl (63 - i))
            }
        }
        return output
    }

    /** 按字节位置×字节值预计算的 32 位片段查表（IP_LEFT/RIGHT、INV_IP_LEFT/RIGHT 共用） */
    private fun buildIpFragmentTable(rule: IntArray, high: Boolean): IntArray {
        val table = IntArray(2048)
        for (bytePos in 0 until 8) {
            for (byteVal in 0 until 256) {
                val permuted = applyPermutation(byteVal.toLong() shl (56 - bytePos * 8), rule)
                val idx = (bytePos shl 8) or byteVal
                table[idx] = if (high) {
                    ((permuted shr 32) and 0xFFFFFFFFL).toInt()
                } else {
                    (permuted and 0xFFFFFFFFL).toInt()
                }
            }
        }
        return table
    }

    // ── 合并查找表（懒生成一次，对齐 generatePermutationTables/generateSpTables/generateEBoxTables）──

    private val IP_LEFT_TABLE: IntArray by lazy { buildIpFragmentTable(QrcTables.IP_RULE, high = true) }
    private val IP_RIGHT_TABLE: IntArray by lazy { buildIpFragmentTable(QrcTables.IP_RULE, high = false) }
    private val INV_IP_LEFT_TABLE: IntArray by lazy { buildIpFragmentTable(QrcTables.INV_IP_RULE, high = true) }
    private val INV_IP_RIGHT_TABLE: IntArray by lazy { buildIpFragmentTable(QrcTables.INV_IP_RULE, high = false) }

    private class EboxTables(val high: IntArray, val low: IntArray)

    private val EBOX_TABLES: EboxTables by lazy {
        val highTable = IntArray(1024)
        val lowTable = IntArray(1024)
        for (chunkIdx in 0 until 4) {
            val shiftIn32 = (3 - chunkIdx) * 8
            for (byteVal in 0 until 256) {
                var high24 = 0
                var low24 = 0
                val input = byteVal shl shiftIn32
                for (i in 0 until 24) {
                    if (((input ushr (32 - QrcTables.E_BOX_TABLE[i])) and 1) != 0) high24 = high24 or (1 shl (23 - i))
                }
                for (i in 24 until 48) {
                    if (((input ushr (32 - QrcTables.E_BOX_TABLE[i])) and 1) != 0) low24 = low24 or (1 shl (47 - i))
                }
                val idx = (chunkIdx shl 8) or byteVal
                highTable[idx] = high24
                lowTable[idx] = low24
            }
        }
        EboxTables(highTable, lowTable)
    }

    /** DES F 函数（custom-des.ts fFunction） */
    private fun fFunction(state: Int, keyHigh24: Int, keyLow24: Int): Int {
        val b0 = state ushr 24 and 255
        val b1 = state ushr 16 and 255
        val b2 = state ushr 8 and 255
        val b3 = state and 255
        val eboxHigh24 =
            EBOX_TABLES.high[b0] or EBOX_TABLES.high[256 or b1] or EBOX_TABLES.high[512 or b2] or EBOX_TABLES.high[768 or b3]
        val eboxLow24 =
            EBOX_TABLES.low[b0] or EBOX_TABLES.low[256 or b1] or EBOX_TABLES.low[512 or b2] or EBOX_TABLES.low[768 or b3]
        val xorHigh24 = eboxHigh24 xor keyHigh24
        val xorLow24 = eboxLow24 xor keyLow24
        return SP_TABLE[xorHigh24 ushr 18 and 63] or SP_TABLE[64 or (xorHigh24 ushr 12 and 63)] or
            SP_TABLE[128 or (xorHigh24 ushr 6 and 63)] or SP_TABLE[192 or (xorHigh24 and 63)] or
            SP_TABLE[256 or (xorLow24 ushr 18 and 63)] or SP_TABLE[320 or (xorLow24 ushr 12 and 63)] or
            SP_TABLE[384 or (xorLow24 ushr 6 and 63)] or SP_TABLE[448 or (xorLow24 and 63)]
    }

    private val SP_TABLE: IntArray by lazy {
        val table = IntArray(512)
        for (sBoxIdx in 0 until 8) {
            for (sBoxInput in 0 until 64) {
                val idx = sboxIndex(sBoxInput)
                val prePBoxVal = QrcTables.S_BOXES[sBoxIdx][idx] shl (28 - sBoxIdx * 4)
                table[(sBoxIdx shl 6) or sBoxInput] = applyQqPbox(prePBoxVal)
            }
        }
        table
    }

    /** 加密/解密单个 8 字节块（custom-des.ts desCrypt） */
    private fun desCrypt(input: ByteArray, inputOffset: Int, output: ByteArray, schedule: IntArray) {
        var left = 0
        var right = 0
        for (i in 0 until 8) {
            val idx = (i shl 8) or (input[inputOffset + i].toInt() and 255)
            left = left or IP_LEFT_TABLE[idx]
            right = right or IP_RIGHT_TABLE[idx]
        }
        for (i in 0 until 15) {
            val temp = right
            right = left xor fFunction(right, schedule[i * 2], schedule[i * 2 + 1])
            left = temp
        }
        left = left xor fFunction(right, schedule[30], schedule[31])
        var outLeft = 0
        var outRight = 0
        for (i in 0 until 4) {
            val idxL = (i shl 8) or ((left ushr (24 - i * 8)) and 255)
            outLeft = outLeft or INV_IP_LEFT_TABLE[idxL]
            outRight = outRight or INV_IP_RIGHT_TABLE[idxL]
            val idxR = ((i + 4) shl 8) or ((right ushr (24 - i * 8)) and 255)
            outLeft = outLeft or INV_IP_LEFT_TABLE[idxR]
            outRight = outRight or INV_IP_RIGHT_TABLE[idxR]
        }
        output[0] = (outLeft ushr 24 and 255).toByte()
        output[1] = (outLeft ushr 16 and 255).toByte()
        output[2] = (outLeft ushr 8 and 255).toByte()
        output[3] = (outLeft and 255).toByte()
        output[4] = (outRight ushr 24 and 255).toByte()
        output[5] = (outRight ushr 16 and 255).toByte()
        output[6] = (outRight ushr 8 and 255).toByte()
        output[7] = (outRight and 255).toByte()
    }

    // ── 3DES 编解码器（eqrc/index.ts QqMusicCodec）────────

    init {
    }

    private val DECRYPT_SCHEDULE: List<IntArray> = listOf(
        keySchedule(KEY_3, 1),
        keySchedule(KEY_2, 0),
        keySchedule(KEY_1, 1),
    )

    /** 解密一个 8 字节块：D(K3) ∘ E'(K2) ∘ D(K1) 三段串联 */
    private fun decryptBlock(input: ByteArray, inOffset: Int, output: ByteArray, outOffset: Int) {
        val temp1 = ByteArray(8)
        val temp2 = ByteArray(8)
        desCrypt(input, inOffset, temp1, DECRYPT_SCHEDULE[0])
        desCrypt(temp1, 0, temp2, DECRYPT_SCHEDULE[1])
        desCrypt(temp2, 0, temp1, DECRYPT_SCHEDULE[2])
        System.arraycopy(temp1, 0, output, outOffset, 8)
    }

    /** zlib inflate + 去头部 UTF-8 BOM（eqrc/index.ts decompress；pako.inflate 为 zlib 格式 → Java Inflater 默认即 zlib） */
    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        while (!inflater.finished()) {
            val n = inflater.inflate(chunk)
            if (n == 0 && inflater.needsInput()) break
            buffer.write(chunk, 0, n)
        }
        inflater.end()
        val decompressed = buffer.toByteArray()
        return if (decompressed.size >= 3 && decompressed[0] == 0xEF.toByte() &&
            decompressed[1] == 0xBB.toByte() && decompressed[2] == 0xBF.toByte()
        ) {
            decompressed.copyOfRange(3, decompressed.size)
        } else {
            decompressed
        }
    }

    /**
     * 解密十六进制格式的 QRC 歌词数据（对齐 decryptQrcHex）：
     * 返回前后有 XML 混合的 QRC 明文；格式非法/长度非 8 倍数/解压失败返回 null。
     */
    fun decryptHex(encryptedHexString: String): String? {
        val hex = encryptedHexString.trim()
        if (hex.isEmpty() || hex.length % 2 != 0 || !hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            return null
        }
        val encryptedBytes = try {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (_: Exception) {
            return null
        }
        if (encryptedBytes.isEmpty() || encryptedBytes.size % 8 != 0) {
            return null
        }
        val decrypted = ByteArray(encryptedBytes.size)
        var offset = 0
        while (offset < encryptedBytes.size) {
            decryptBlock(encryptedBytes, offset, decrypted, offset)
            offset += 8
        }
        return try {
            String(inflate(decrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    // ── QRC 正文抽取（qrc.ts extractQrcLyricContent / looksLikeWordLevelBracket）──

    /** 判断是否像明文 QRC/YRC 行时间轴（providers/qrc.ts looksLikeWordLevelBracket） */
    fun looksLikeWordLevelBracket(text: String): Boolean {
        val line = text.split(Regex("\\r?\\n")).firstOrNull { l ->
            l.trim().isNotEmpty() && !l.trim().startsWith("{")
        } ?: return false
        return Regex("^\\[\\d+,\\d+\\]").containsMatchIn(line.trim())
    }

    /** 从 decrypt 结果（XML）抽出 LyricContent 属性内的 QRC 正文（extractQrcLyricContent） */
    fun extractLyricContent(xmlOrPlain: String): String {
        val raw = xmlOrPlain.trim()
        if (raw.isEmpty()) return ""
        if (looksLikeWordLevelBracket(raw) && !raw.contains("<Qrc")) return raw
        val match = Regex("LyricContent=\"([\\s\\S]*?)\"\\s*/>", RegexOption.IGNORE_CASE).find(raw)
            ?: Regex("LyricContent=\"([\\s\\S]*?)\"", RegexOption.IGNORE_CASE).find(raw)
        val content = match?.groupValues?.get(1) ?: return raw
        return content
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&#10;", "\n")
            .replace("&#13;", "\r")
    }

    /** hex 加密串 → 明文 QRC；失败返回 null（decryptQrcToPlain） */
    fun decryptToPlain(encryptedHex: String): String? {
        val plain = decryptHex(encryptedHex)?.let { extractLyricContent(it) }?.trim() ?: return null
        if (plain.isEmpty() || !looksLikeWordLevelBracket(plain)) return null
        return plain
    }
}
