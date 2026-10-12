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
import java.time.YearMonth

class LunarMonthWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val month = YearMonth.now(); val today = LocalDate.now()
        val leading = month.atDay(1).dayOfWeek.value - 1
        val calendar = buildString {
            append("M   T   W   T   F   S   S\n")
            repeat(leading) { append("    ") }
            repeat(month.lengthOfMonth()) { index ->
                val day = index + 1
                append(day.toString().padStart(2, ' ')).append("  ")
                if ((leading + index + 1) % 7 == 0) append('\n')
            }
        }.trimEnd()
        val upcoming = LunarCalendarConverter.upcomingMarkers(today, 35, LunarPreferences.region(context)).take(3).joinToString("\n") {
            "${it.solarDate.dayOfMonth}/${it.solarDate.monthValue}  ${it.traditionalMarker ?: it.solarTerm.orEmpty()}"
        }
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_lunar_month).apply {
                setTextViewText(R.id.widget_month_title, month.toString())
                setTextViewText(R.id.widget_month_grid, calendar)
                setTextViewText(R.id.widget_upcoming, upcoming)
                setOnClickPendingIntent(R.id.widget_month_root, PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            manager.updateAppWidget(id, views)
        }
    }
}
