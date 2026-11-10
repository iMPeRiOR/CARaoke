package com.example.androidautolyrics.lyrics

data class LyricLine(
    val timestampMs: Long,
    val text: String,
)

object LrcParser {
    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offset = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

    fun parse(lrc: String): List<LyricLine> {
        val offsetMs = offset.find(lrc)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

        return lrc.lineSequence()
            .flatMap { rawLine ->
                val matches = timestamp.findAll(rawLine).toList()
                if (matches.isEmpty()) {
                    return@flatMap emptySequence()
                }

                val text = rawLine.substring(matches.last().range.last + 1).trim()
                if (text.isEmpty()) {
                    return@flatMap emptySequence()
                }

                matches.asSequence().map { match ->
                    val minutes = match.groupValues[1].toLong()
                    val seconds = match.groupValues[2].toLong()
                    val fraction = match.groupValues[3]
                    val fractionMs = when (fraction.length) {
                        1 -> fraction.toLong() * 100
                        2 -> fraction.toLong() * 10
                        3 -> fraction.toLong()
                        else -> 0L
                    }
                    LyricLine(
                        timestampMs = (minutes * 60_000 + seconds * 1_000 + fractionMs + offsetMs)
                            .coerceAtLeast(0),
                        text = text,
                    )
                }
            }
            .sortedBy(LyricLine::timestampMs)
            .toList()
    }

    fun lineAt(lines: List<LyricLine>, positionMs: Long): LyricLine? {
        if (lines.isEmpty() || positionMs < lines.first().timestampMs) {
            return null
        }

        var low = 0
        var high = lines.lastIndex
        while (low <= high) {
            val middle = (low + high).ushr(1)
            if (lines[middle].timestampMs <= positionMs) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return lines[high]
    }
}
