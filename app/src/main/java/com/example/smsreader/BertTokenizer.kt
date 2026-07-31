package com.example.smsreader

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Minimal WordPiece tokenizer for TinyBERT (uncased).
 * Loads vocab.txt from assets.
 */
class BertTokenizer(context: Context, assetName: String = "vocab.txt") {

    private val vocab: MutableMap<String, Int> = mutableMapOf()
    private val ids: MutableMap<Int, String> = mutableMapOf()

    init {
        val reader = BufferedReader(InputStreamReader(context.assets.open(assetName)))
        var i = 0
        reader.forEachLine { line ->
            vocab[line] = i
            ids[i] = line
            i++
        }
        reader.close()
    }

    fun vocabSize(): Int = vocab.size

    /** Tokenize text -> [CLS] + tokens + [SEP], returns inputIds, attentionMask, segmentIds. */
    fun tokenize(text: String, maxLen: Int = 128): Triple<IntArray, IntArray, IntArray> {
        val tokens = mutableListOf<String>()
        tokens.add("[CLS]")

        // Basic tokenize: lowercase, separate CJK, split on punctuation/whitespace
        val basic = basicTokenize(text.lowercase())
        for (token in basic) {
            wordPiece(token, tokens)
        }

        tokens.add("[SEP]")

        // Truncate
        val effective = tokens.take(maxLen)
        val inputIds = IntArray(maxLen)
        val attentionMask = IntArray(maxLen)
        val segmentIds = IntArray(maxLen)

        for ((i, tok) in effective.withIndex()) {
            inputIds[i] = vocab[tok] ?: vocab["[UNK]"] ?: 100
            attentionMask[i] = 1
            segmentIds[i] = 0
        }

        return Triple(inputIds, attentionMask, segmentIds)
    }

    /** Split text into basic tokens: whitespace, separate CJK, split punctuation. */
    private fun basicTokenize(text: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()

        for (ch in text) {
            when {
                ch.isWhitespace() -> { flushBuf(sb, result) }
                ch in '一'..'鿿' || ch in '㐀'..'䶿' -> {
                    // CJK character -> separate token
                    flushBuf(sb, result)
                    result.add(ch.toString())
                }
                ch in setOf('，', '。', '！', '？', '、', '：', '；', '（', '）',
                    '【', '】', '《', '》', '“', '”', '‘', '’', '…', '—', '～') ||
                ch in setOf(',', '.', '!', '?', ':', ';', '(', ')', '[', ']',
                    '{', '}', '<', '>', '"', '\'', '-', '_', '/', '\\', '@', '#',
                    '$', '%', '^', '&', '*', '+', '=', '|', '~', '`') -> {
                    flushBuf(sb, result)
                    result.add(ch.toString())
                }
                else -> sb.append(ch)
            }
        }
        flushBuf(sb, result)
        return result.filter { it.isNotBlank() }
    }

    private fun flushBuf(sb: StringBuilder, out: MutableList<String>) {
        if (sb.isNotEmpty()) {
            out.add(sb.toString())
            sb.clear()
        }
    }

    /** Greedy longest-match WordPiece decomposition. */
    private fun wordPiece(token: String, out: MutableList<String>) {
        if (vocab.containsKey(token)) {
            out.add(token)
            return
        }

        val chars = token.toMutableList()
        var start = 0
        while (start < chars.size) {
            var end = chars.size
            var found = false
            while (end > start) {
                val sub = String(chars.toCharArray(), start, end - start)
                val key = if (start > 0) "##$sub" else sub
                if (vocab.containsKey(key)) {
                    out.add(key)
                    found = true
                    break
                }
                end--
            }
            if (found) {
                start = end
            } else {
                out.add("[UNK]")
                break
            }
        }
    }
}
