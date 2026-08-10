package com.purehub.app.feature.receipt

data class ReceiptResult(
    val merchant: String,
    val total: Double?,
    val tax: Double?,
    val tip: Double?,
    val date: String?,
    val lines: List<ReceiptLine>,
    val rawText: String,
)

data class ReceiptLine(val name: String, val amount: Double)

object ReceiptParser {
    private val amount = Regex("(?:[$€£₫]|VND|USD)?\\s*([0-9]{1,3}(?:[., ][0-9]{3})*(?:[.,][0-9]{2})?)\\s*(?:VND|USD)?$", RegexOption.IGNORE_CASE)
    private val totalWords = Regex("\\b(total|grand total|amount due|balance due|tổng cộng|thanh toán)\\b", RegexOption.IGNORE_CASE)
    private val taxWords = Regex("\\b(tax|vat|thuế)\\b", RegexOption.IGNORE_CASE)
    private val tipWords = Regex("\\b(tip|gratuity|service charge)\\b", RegexOption.IGNORE_CASE)
    private val datePattern = Regex("\\b(?:\\d{1,2}[-/.]){2}\\d{2,4}\\b")
    private val referenceWords = Regex("\\b(receipt|invoice|order|reference|ref)\\b", RegexOption.IGNORE_CASE)

    fun parse(text: String): ReceiptResult {
        val cleanLines = text.lines().map(String::trim).filter(String::isNotBlank)
        val inlinePriced = cleanLines.mapNotNull { line ->
            val value = parseAmount(line) ?: return@mapNotNull null
            val label = line.replace(amount, "").trim(' ', '-', ':')
            if (
                label.isBlank() ||
                datePattern.containsMatchIn(line) ||
                '#' in line ||
                referenceWords.containsMatchIn(label)
            ) {
                null
            } else {
                line to value
            }
        }
        val columnPriced = pairSplitColumns(cleanLines)
        val priced = if (columnPriced.any { totalWords.containsMatchIn(it.first) }) columnPriced else inlinePriced
        val total = priced.lastOrNull { totalWords.containsMatchIn(it.first) }?.second
            ?: priced.maxOfOrNull { it.second }
        val tax = priced.lastOrNull { taxWords.containsMatchIn(it.first) }?.second
        val tip = priced.lastOrNull { tipWords.containsMatchIn(it.first) }?.second
        val excluded = totalWords.pattern + "|" + taxWords.pattern + "|" + tipWords.pattern
        val lines = priced.filterNot {
            Regex(excluded, RegexOption.IGNORE_CASE).containsMatchIn(it.first) ||
                datePattern.containsMatchIn(it.first)
        }
            .map { (line, value) ->
                ReceiptLine(line.replace(amount, "").trim(' ', '-', ':').ifBlank { "Receipt item" }, value)
            }.take(30)
        return ReceiptResult(
            merchant = cleanLines.firstOrNull { it.any(Char::isLetter) }?.take(80) ?: "Receipt",
            total = total,
            tax = tax,
            tip = tip,
            date = datePattern.find(text)?.value,
            lines = lines,
            rawText = text,
        )
    }

    /**
     * ML Kit can return a receipt's left text column first and its right price column second.
     * Rebuild those rows when the document ends with a contiguous block of standalone prices.
     */
    private fun pairSplitColumns(lines: List<String>): List<Pair<String, Double>> {
        val trailingAmounts = lines.asReversed()
            .takeWhile { line ->
                val value = parseAmount(line)
                value != null && line.replace(amount, "").isBlank()
            }
            .asReversed()
            .mapNotNull(::parseAmount)
        if (trailingAmounts.size < 2) return emptyList()

        val labelsEnd = lines.size - trailingAmounts.size
        if (labelsEnd < trailingAmounts.size) return emptyList()
        val labels = lines.subList(labelsEnd - trailingAmounts.size, labelsEnd)
        if (labels.any { it.isBlank() || datePattern.containsMatchIn(it) || parseAmount(it) != null }) return emptyList()
        return labels.zip(trailingAmounts)
    }

    private fun parseAmount(line: String): Double? {
        val raw = amount.find(line)?.groupValues?.getOrNull(1)?.replace(" ", "") ?: return null
        val normalized = when {
            raw.count { it == ',' } == 1 && raw.substringAfter(',').length == 2 -> raw.replace(".", "").replace(',', '.')
            raw.count { it == '.' } == 1 && raw.substringAfter('.').length == 2 -> raw.replace(",", "")
            else -> raw.replace(",", "").replace(".", "")
        }
        return normalized.toDoubleOrNull()?.takeIf { it > 0.0 }
    }
}
