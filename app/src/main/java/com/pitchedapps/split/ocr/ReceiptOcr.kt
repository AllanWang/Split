package com.pitchedapps.split.ocr

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object ReceiptOcr {

    suspend fun read(context: Context, uri: Uri): Result? =
        withContext(Dispatchers.IO) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext null
            }
            val text = recognizer.use { it.readReceipt(image) }
            val receiptText = text.toReceiptText()
            val data = receiptText.toEntries().toData()
            Result(text = receiptText, data = data)
        }

    private suspend fun TextRecognizer.readReceipt(image: InputImage): Text =
        suspendCoroutine { cont ->
            process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    internal fun Text.toReceiptText(): ReceiptText {
        val data = textBlocks.flatMap { it.lines }.mapNotNull {
            val bounds = it.boundingBox ?: return@mapNotNull null
            ReceiptText.Entry(text = it.text, bounds = bounds)
        }.sortedBy { it.bounds.top }

        val leftMin = data.minOf { it.bounds.left }
        val leftMax = data.maxOf { it.bounds.left }
        val leftCutoff = progress(leftMin, leftMax, 0.6f)

        val (items, values) = data.partition { !(it.text.isCurrency && it.bounds.left >= leftCutoff) }
        return ReceiptText(items = items, values = values)
    }

    /**
     * Convert [ReceiptText] to [List] of [ReceiptData.Entry].
     *
     * An data entry is defined as a text + price receipt entry, where both are on the same line.
     * Lines without one of the two values are ignored.
     */
    internal fun ReceiptText.toEntries(): List<ReceiptData.Entry> {
        var itemI = 0

        val data = mutableListOf<ReceiptData.Entry>()
        for (value in values) {
            while (itemI < items.size) {
                val item = items[itemI++]
                if (value.sameLine(item)) {
                    data.add(ReceiptData.Entry(text = item.text, value = value.text.currency))
                    break
                }
            }
        }
        return data
    }

    internal fun List<ReceiptData.Entry>.toData(): ReceiptData {
        if (size <= 3) return ReceiptData(items = this)
        val subtotalIndex = indexOfFirst { it.text.contains("subtotal", ignoreCase = true) }
        if (subtotalIndex == -1) {
            return ReceiptData(items = dropLast(1), total = last())
        }
        return ReceiptData(
            items = subList(0, subtotalIndex),
            subtotal = get(subtotalIndex),
            extras = subList(subtotalIndex + 1, size - 1),
            total = last(),
        )
    }

    internal fun progress(min: Int, max: Int, progress: Float): Int {
        return min + ((max - min) * progress).toInt()
    }

    internal val String.isCurrency: Boolean
        get() {
            if (contains("$")) return true
            if (count { it.isDigit() } > length - 4) return true
            return false
        }

    internal val String.currency: BigDecimal
        get() {
            return filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
                ?.let { BigDecimal.valueOf(it).setScale(2, RoundingMode.HALF_UP) }
                ?: BigDecimal.ZERO
        }

    internal fun Rect.containsY(y: Int) = y in top..bottom

    internal fun Rect.containsY(rect: Rect) = containsY(rect.top) || containsY(rect.bottom)

    /**
     * Representation of entries read from receipt.
     *
     * Items are only skipped if they have no bounding box.
     * Both [items] and [values] are sorted based on top of bounding box.
     */
    data class ReceiptText(val items: List<Entry>, val values: List<Entry>) {
        data class Entry(val text: String, val bounds: Rect) {
            internal fun sameLine(other: Entry): Boolean {
                return bounds.containsY(other.bounds) || other.bounds.containsY(bounds)
            }
        }
    }

    /**
     * Representation of parsed receipt.
     *
     * Only pairs of text + prices are added. Pairings will be moved to the appropriate category where possible.
     */
    data class ReceiptData(
        val items: List<Entry>,
        val subtotal: Entry? = null,
        val extras: List<Entry> = emptyList(),
        val total: Entry? = null,
    ) {
        data class Entry(val text: String, val value: BigDecimal)
    }

    data class Result(val text: ReceiptText, val data: ReceiptData)
}