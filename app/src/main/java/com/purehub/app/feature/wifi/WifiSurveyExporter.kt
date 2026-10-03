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
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.appText
import com.purehub.app.ui.locale
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

object WifiSurveyExporter {
    enum class Format { PNG, PDF }

    fun exportAndShare(context: Context, project: WifiSurveyProject, format: Format, language: AppLanguage) {
        val directory = File(context.cacheDir, "wifi-reports").apply { mkdirs() }
        val safeName = (project.name + "-" + project.floor).replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "survey" }
        val file = File(directory, "purehub-$safeName-${System.currentTimeMillis()}.${format.name.lowercase()}")
        if (format == Format.PNG) {
            val bitmap = render(context, project, WifiSurveyMetric.SIGNAL, language)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        } else writePdf(context, file, project, language)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = if (format == Format.PNG) "image/png" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${t(language, "PureHub Wi-Fi survey", "Khảo sát Wi-Fi PureHub", "PureHub Wi-Fi 覆盖调查", "Estudio Wi-Fi de PureHub")}: ${project.name} / ${project.floor}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, t(language, "Share Wi-Fi survey report", "Chia sẻ báo cáo khảo sát Wi-Fi", "分享 Wi-Fi 覆盖报告", "Compartir informe del estudio Wi-Fi")))
    }

    private fun render(context: Context, project: WifiSurveyProject, metric: WifiSurveyMetric, language: AppLanguage): Bitmap {
        val width = 1400; val height = 1900; val header = 320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(7, 19, 30))
        paint.color = Color.WHITE; paint.textSize = 54f; paint.isFakeBoldText = true
        canvas.drawText("${t(language, "PureHub Wi-Fi survey", "Khảo sát Wi-Fi PureHub", "PureHub Wi-Fi 覆盖调查", "Estudio Wi-Fi de PureHub")} · ${metricLabel(metric, language)}", 70f, 85f, paint)
        paint.textSize = 32f; paint.isFakeBoldText = false; paint.color = Color.rgb(69, 224, 181)
        canvas.drawText("${project.name} · ${project.floor}", 70f, 140f, paint)
        paint.textSize = 24f; paint.color = Color.LTGRAY
        val values = project.points.mapNotNull { WifiPremiumInsights.surveyValue(it, metric) }
        val summary = if (values.isEmpty()) t(language, "No measured values for this layer", "Chưa có giá trị đo cho lớp này", "此图层暂无测量值", "No hay valores medidos para esta capa") else
            when (language) {
                AppLanguage.English -> "${values.size} points · min ${format(values.min(), language)} ${metricUnit(metric, language)} · avg ${format(values.average(), language)} ${metricUnit(metric, language)} · max ${format(values.max(), language)} ${metricUnit(metric, language)}"
                AppLanguage.Vietnamese -> "${values.size} điểm · thấp nhất ${format(values.min(), language)} ${metricUnit(metric, language)} · trung bình ${format(values.average(), language)} ${metricUnit(metric, language)} · cao nhất ${format(values.max(), language)} ${metricUnit(metric, language)}"
                AppLanguage.Chinese -> "${values.size} 个测点 · 最低 ${format(values.min(), language)} ${metricUnit(metric, language)} · 平均 ${format(values.average(), language)} ${metricUnit(metric, language)} · 最高 ${format(values.max(), language)} ${metricUnit(metric, language)}"
                AppLanguage.Spanish -> "${values.size} puntos · mín. ${format(values.min(), language)} ${metricUnit(metric, language)} · media ${format(values.average(), language)} ${metricUnit(metric, language)} · máx. ${format(values.max(), language)} ${metricUnit(metric, language)}"
            }
        drawFittedText(canvas, summary, 70f, 190f, width - 140f, paint)
        val scale = project.calibration?.metersPerNormalizedUnit?.let {
            "${t(language, "Calibrated", "Đã hiệu chỉnh", "已校准", "Calibrado")}: ${format(it.toDouble(), language, 2)} ${t(language, "m per plan unit", "m trên mỗi đơn vị sơ đồ", "米/平面单位", "m por unidad del plano")}"
        } ?: t(language, "Scale not calibrated", "Chưa hiệu chỉnh tỷ lệ", "比例尚未校准", "Escala sin calibrar")
        drawFittedText(canvas, "$scale · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, language.locale()).format(Date())}", 70f, 230f, width - 140f, paint)
        val diagnosticPoints = project.points.filter { it.latencyMs != null || it.downloadMbps != null }
        val diagnostics = if (diagnosticPoints.isEmpty()) t(language, "No attached network-test metrics", "Chưa có chỉ số kiểm tra mạng đính kèm", "没有附加网络测试指标", "Sin métricas de prueba de red adjuntas") else buildString {
            append("${diagnosticPoints.size} ${t(language, "diagnostic points", "điểm chẩn đoán", "个诊断点", "puntos de diagnóstico")}")
            diagnosticPoints.mapNotNull(WifiSurveyPoint::latencyMs).takeIf(List<Int>::isNotEmpty)?.let { append(" · ${t(language, "avg latency", "độ trễ TB", "平均延迟", "latencia media")} ${it.average().toInt()} ms") }
            diagnosticPoints.mapNotNull(WifiSurveyPoint::downloadMbps).takeIf(List<Double>::isNotEmpty)?.let { append(" · ${t(language, "avg down", "tải xuống TB", "平均下载", "descarga media")} ${format(it.average(), language)} Mbps") }
        }
        drawFittedText(canvas, diagnostics, 70f, 275f, width - 140f, paint)

        val mapRect = Rect(55, header, width - 55, height - 110)
        val plan = project.planUri?.let { uri -> runCatching { context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use(BitmapFactory::decodeStream) }.getOrNull() }
        if (plan != null) canvas.drawBitmap(plan, null, mapRect, paint) else drawGrid(canvas, paint, mapRect)
        drawHeatmap(canvas, mapRect, project.points, metric)
        paint.textSize = 21f; paint.color = Color.LTGRAY
        drawFittedText(canvas, t(language, "IDW visualization from measured samples; not a simulated RF propagation model.", "Nội suy IDW từ mẫu đo; không phải mô hình mô phỏng lan truyền RF.", "基于实测样本的 IDW 可视化，并非模拟射频传播模型。", "Visualización IDW basada en muestras medidas; no es un modelo simulado de propagación RF."), 70f, height - 55f, width - 140f, paint)
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

    private fun writePdf(context: Context, file: File, project: WifiSurveyProject, language: AppLanguage) {
        val document = PdfDocument()
        val metrics = WifiSurveyMetric.entries.filter { metric -> project.points.any { WifiPremiumInsights.surveyValue(it, metric) != null } }.ifEmpty { listOf(WifiSurveyMetric.SIGNAL) }
        metrics.forEachIndexed { index, metric ->
            val bitmap = render(context, project, metric, language)
            val page = document.startPage(PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create())
            page.canvas.drawBitmap(bitmap, 0f, 0f, null); document.finishPage(page); bitmap.recycle()
        }
        FileOutputStream(file).use(document::writeTo); document.close()
    }

    private fun drawFittedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint, minimumSize: Float = 16f) {
        val originalSize = paint.textSize
        while (paint.measureText(text) > maxWidth && paint.textSize > minimumSize) paint.textSize -= 1f
        canvas.drawText(text, x, y, paint)
        paint.textSize = originalSize
    }

    private fun metricUnit(metric: WifiSurveyMetric, language: AppLanguage): String = when (metric) {
        WifiSurveyMetric.SIGNAL -> "dBm"
        WifiSurveyMetric.SIR -> t(language, "dB est.", "dB ước tính", "dB 估算", "dB est.")
        WifiSurveyMetric.DOWNLOAD, WifiSurveyMetric.UPLOAD -> "Mbps"
        WifiSurveyMetric.LATENCY -> "ms"
        WifiSurveyMetric.PACKET_LOSS -> "%"
    }

    private fun metricLabel(metric: WifiSurveyMetric, language: AppLanguage): String = when (metric) {
        WifiSurveyMetric.SIGNAL -> t(language, "Signal", "Tín hiệu", "信号", "Señal")
        WifiSurveyMetric.SIR -> t(language, "Estimated SIR", "SIR ước tính", "估算 SIR", "SIR estimado")
        WifiSurveyMetric.DOWNLOAD -> t(language, "Download", "Tải xuống", "下载", "Descarga")
        WifiSurveyMetric.UPLOAD -> t(language, "Upload", "Tải lên", "上传", "Subida")
        WifiSurveyMetric.LATENCY -> t(language, "Latency", "Độ trễ", "延迟", "Latencia")
        WifiSurveyMetric.PACKET_LOSS -> t(language, "Packet loss", "Mất gói", "丢包", "Pérdida de paquetes")
    }

    private fun format(value: Double, language: AppLanguage, decimals: Int = 1): String = String.format(language.locale(), "%.${decimals}f", value)

    private fun t(language: AppLanguage, en: String, vi: String, zh: String, es: String): String = appText(language, en, vi, zh, es)

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
