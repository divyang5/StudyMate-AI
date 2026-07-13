package com.divyang.studymateai.utils

// Blocks small enough that no single Text/TextField is expensive to lay out,
// split on paragraph/line boundaries so the visual flow is preserved. Both the
// chapter detail view and the block editor render chapter content this way —
// one huge string in a single composable is what made 20+ page chapters lag.
private const val MAX_BLOCK_LENGTH = 3000

object TextBlocks {

    fun split(content: String): List<String> =
        content.split("\n\n").flatMap { paragraph ->
            if (paragraph.length <= MAX_BLOCK_LENGTH) {
                listOf(paragraph)
            } else {
                val blocks = mutableListOf<String>()
                val current = StringBuilder()
                paragraph.split("\n").forEach { line ->
                    if (current.isNotEmpty() && current.length + line.length > MAX_BLOCK_LENGTH) {
                        blocks += current.toString()
                        current.clear()
                    }
                    if (line.length > MAX_BLOCK_LENGTH) {
                        blocks += line.chunked(MAX_BLOCK_LENGTH)
                    } else {
                        if (current.isNotEmpty()) current.append('\n')
                        current.append(line)
                    }
                }
                if (current.isNotEmpty()) blocks += current.toString()
                blocks
            }
        }.filter { it.isNotBlank() }

    fun join(blocks: List<String>): String =
        blocks.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n\n")
}
