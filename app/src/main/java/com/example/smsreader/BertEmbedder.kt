package com.example.smsreader

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TinyBERT embedding inference via TFLite.
 * Input: text string. Output: FloatArray of dim 312.
 */
class BertEmbedder(context: Context) {

    private var interpreter: Interpreter? = null
    private var tokenizer: BertTokenizer? = null
    private val embedDim = 312

    val isReady: Boolean

    init {
        var ready = false
        try {
            tokenizer = BertTokenizer(context, "vocab.txt")
            interpreter = Interpreter(loadModelFile(context, "tinybert.tflite"))
            ready = true
        } catch (_: Exception) {
            // 模型文件缺失，降级为关键词分类
        }
        isReady = ready
    }

    /** Run inference, return embedding vector. 模型不可用时返回零向量。 */
    fun embed(text: String): FloatArray {
        val tok = tokenizer ?: return FloatArray(embedDim)
        val interp = interpreter ?: return FloatArray(embedDim)
        val (ids, mask, segs) = tok.tokenize(text, 128)

        val inputs = arrayOf(
            arrayOf(ids),
            arrayOf(mask),
            arrayOf(segs),
        )
        val outputs = mapOf(
            0 to Array(1) { FloatArray(embedDim) },
        )

        interp.runForMultipleInputsOutputs(inputs, outputs)
        return outputs[0]!![0]
    }

    fun close() {
        interpreter?.close()
    }

    private fun loadModelFile(context: Context, name: String): MappedByteBuffer {
        val fd = context.assets.openFd(name)
        val stream = FileInputStream(fd.fileDescriptor)
        return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }
}
