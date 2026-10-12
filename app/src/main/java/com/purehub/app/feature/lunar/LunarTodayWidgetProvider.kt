package com.purehub.app.feature.lunar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.purehub.app.MainActivity
import com.purehub.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class LunarTodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val today = LocalDate.now()
            val details = LunarCalendarConverter.details(today, LunarPreferences.region(context))
            val language = Locale.getDefault().language
            val title = when (language) { "vi" -> "HÔM NAY"; "zh" -> "今天"; "es" -> "HOY"; else -> "TODAY" }
            val lunarLabel = when (language) { "vi" -> "Âm lịch"; "zh" -> "农历"; "es" -> "Calendario lunar"; else -> "Lunar" }
            val views = RemoteViews(context.packageName, R.layout.widget_lunar_today).apply {
                setTextViewText(R.id.widget_eyebrow, title)
                setTextViewText(R.id.widget_solar_day, today.dayOfMonth.toString())
                setTextViewText(R.id.widget_solar_date, today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())))
                setTextViewText(R.id.widget_lunar_date, "$lunarLabel ${details.lunarDate.day}/${details.lunarDate.month} · ${details.canChiDay}")
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
