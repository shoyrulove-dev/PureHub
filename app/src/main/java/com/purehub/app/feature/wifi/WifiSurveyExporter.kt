package com.purehub.app.feature.wifi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

object WifiSurveyExporter {
    enum class Format { PNG, PDF }

    fun exportAndShare(context: Context, project: WifiSurveyProject, format: Format) {
        val directory = File(context.cacheDir, "wifi-reports").apply { mkdirs() }
        val safeName = (project.name + "-" + project.floor).replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "survey" }
        val file = File(directory, "purehub-$safeName-${System.currentTimeMillis()}.${format.name.lowercase()}")
        if (format == Format.PNG) {
            val bitmap = render(context, project, WifiSurveyMetric.SIGNAL)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        } else writePdf(context, file, project)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = if (format == Format.PNG) "image/png" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PureHub Wi-Fi survey: ${project.name} / ${project.floor}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share Wi-Fi survey report"))
    }

    private fun render(context: Context, project: WifiSurveyProject, metric: WifiSurveyMetric): Bitmap {
        val width = 1400; val height = 1900; val header = 320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(7, 19, 30))
        paint.color = Color.WHITE; paint.textSize = 54f; paint.isFakeBoldText = true
        canvas.drawText("PureHub Wi-Fi survey · ${metric.label}", 70f, 85f, paint)
        paint.textSize = 32f; paint.isFakeBoldText = false; paint.color = Color.rgb(69, 224, 181)
        canvas.drawText("${project.name} · ${project.floor}", 70f, 140f, paint)
        paint.textSize = 24f; paint.color = Color.LTGRAY
        val values = project.points.mapNotNull { WifiPremiumInsights.surveyValue(it, metric) }
        val summary = if (values.isEmpty()) "No measured values for this layer" else
            "${values.size} points · min ${"%.1f".format(values.min())} ${metricUnit(metric)} · avg ${"%.1f".format(values.average())} ${metricUnit(metric)} · max ${"%.1f".format(values.max())} ${metricUnit(metric)}"
        canvas.drawText(summary, 70f, 190f, paint)
        val scale = project.calibration?.metersPerNormalizedUnit?.let { "Calibrated: %.2f m per plan unit".format(it) } ?: "Scale not calibrated"
        canvas.drawText("$scale · ${DateFormat.getDateTimeInstance().format(Date())}", 70f, 230f, paint)
        val diagnosticPoints = project.points.filter { it.latencyMs != null || it.downloadMbps != null }
        val diagnostics = if (diagnosticPoints.isEmpty()) "No attached network-test metrics" else buildString {
            append("${diagnosticPoints.size} diagnostic points")
            diagnosticPoints.mapNotNull(WifiSurveyPoint::latencyMs).takeIf(List<Int>::isNotEmpty)?.let { append(" · avg latency ${it.average().toInt()} ms") }
            diagnosticPoints.mapNotNull(WifiSurveyPoint::downloadMbps).takeIf(List<Double>::isNotEmpty)?.let { append(" · avg down ${"%.1f".format(it.average())} Mbps") }
        }
        canvas.drawText(diagnostics, 70f, 275f, paint)

        val mapRect = Rect(55, header, width - 55, height - 110)
        val plan = project.planUri?.let { uri -> runCatching { context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use(BitmapFactory::decodeStream) }.getOrNull() }
        if (plan != null) canvas.drawBitmap(plan, null, mapRect, paint) else drawGrid(canvas, paint, mapRect)
        drawHeatmap(canvas, mapRect, project.points, metric)
        paint.textSize = 21f; paint.color = Color.LTGRAY
        canvas.drawText("IDW visualization from measured samples; not a simulated RF propagation model.", 70f, height - 55f, paint)
        plan?.recycle()
        return bitmap
    }

    private fun drawGrid(canvas: Canvas, paint: Paint, rect: Rect) {
        paint.color = Color.rgb(13, 34, 48); canvas.drawRect(rect, paint)
        paint.color = Color.argb(35, 255, 255, 255); paint.strokeWidth = 2f
        repeat(9) { i -> val y = rect.top + rect.height() * i / 8f; canvas.drawLine(rect.left.toFloat(), y, rect.right.toFloat(), y, paint) }
        repeat(7) { i -> val x = rect.left + rect.width() * i / 6f; canvas.drawLine(x, rect.top.toFloat(), x, rect.bottom.toFloat(), paint) }
    }

    private fun drawHeatmap(canvas: Canvas, rect: Rect, points: List<WifiSurveyPoint>, metric: WifiSurveyMetric) {
        val measured = points.mapNotNull { point -> WifiPremiumInsights.surveyValue(point, metric)?.let { point to it } }
        if (measured.size >= 3) {
            val paint = Paint(); val cols = 35; val rows = 45
            repeat(rows) { row -> repeat(cols) { col ->
                val x = (col + .5f) / cols; val y = (row + .5f) / rows
                var weighted = 0.0; var weights = 0.0
                measured.forEach { (point, value) ->
                    val distance = ((x - point.x) * (x - point.x) + (y - point.y) * (y - point.y)).toDouble().coerceAtLeast(.0008)
                    val weight = 1.0 / distance; weighted += value * weight; weights += weight
                }
                paint.color = metricColor(weighted / weights, metric, 92)
                val left = rect.left + rect.width() * col / cols; val top = rect.top + rect.height() * row / rows
                canvas.drawRect(left.toFloat(), top.toFloat(), (rect.left + rect.width() * (col + 1) / cols + 1).toFloat(), (rect.top + rect.height() * (row + 1) / rows + 1).toFloat(), paint)
            } }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        measured.forEachIndexed { index, (point, value) ->
            val x = rect.left + point.x * rect.width(); val y = rect.top + point.y * rect.height()
            paint.color = metricColor(value, metric, 255); canvas.drawCircle(x, y, 13f, paint)
            paint.color = Color.WHITE; paint.textSize = 21f; canvas.drawText("${index + 1}", x + 16f, y - 12f, paint)
        }
    }

    private fun writePdf(context: Context, file: File, project: WifiSurveyProject) {
        val document = PdfDocument()
        val metrics = WifiSurveyMetric.entries.filter { metric -> project.points.any { WifiPremiumInsights.surveyValue(it, metric) != null } }.ifEmpty { listOf(WifiSurveyMetric.SIGNAL) }
        metrics.forEachIndexed { index, metric ->
            val bitmap = render(context, project, metric)
            val page = document.startPage(PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create())
            page.canvas.drawBitmap(bitmap, 0f, 0f, null); document.finishPage(page); bitmap.recycle()
        }
        FileOutputStream(file).use(document::writeTo); document.close()
    }

    private fun metricUnit(metric: WifiSurveyMetric): String = when (metric) {
        WifiSurveyMetric.SIGNAL -> "dBm"
        WifiSurveyMetric.SIR -> "dB est."
        WifiSurveyMetric.DOWNLOAD, WifiSurveyMetric.UPLOAD -> "Mbps"
        WifiSurveyMetric.LATENCY -> "ms"
        WifiSurveyMetric.PACKET_LOSS -> "%"
    }

    private fun metricColor(value: Double, metric: WifiSurveyMetric, alpha: Int): Int {
        val healthy = when (metric) {
            WifiSurveyMetric.SIGNAL -> value >= -60; WifiSurveyMetric.SIR -> value >= 20
            WifiSurveyMetric.DOWNLOAD -> value >= 50; WifiSurveyMetric.UPLOAD -> value >= 15
            WifiSurveyMetric.LATENCY -> value <= 45; WifiSurveyMetric.PACKET_LOSS -> value <= 1
        }
        val warning = when (metric) {
            WifiSurveyMetric.SIGNAL -> value >= -72; WifiSurveyMetric.SIR -> value >= 10
            WifiSurveyMetric.DOWNLOAD -> value >= 15; WifiSurveyMetric.UPLOAD -> value >= 5
            WifiSurveyMetric.LATENCY -> value <= 100; WifiSurveyMetric.PACKET_LOSS -> value <= 5
        }
        val color = when { healthy -> Color.rgb(69, 224, 181); warning -> Color.rgb(113, 184, 255); else -> Color.rgb(255, 184, 107) }
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
