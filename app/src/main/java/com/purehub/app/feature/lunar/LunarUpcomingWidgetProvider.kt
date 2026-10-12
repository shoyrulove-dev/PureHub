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

class LunarUpcomingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val items = LunarCalendarConverter.upcomingMarkers(LocalDate.now(), 60, LunarPreferences.region(context)).take(3)
        val text = items.joinToString("\n") { details ->
            val marker = details.traditionalMarker ?: details.solarTerm.orEmpty()
            "${details.solarDate.dayOfMonth}/${details.solarDate.monthValue}  $marker  ·  ${details.lunarDate.day}/${details.lunarDate.month}"
        }.ifBlank { "No upcoming lunar markers" }
        val openApp = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_lunar_upcoming).apply {
                setTextViewText(R.id.widget_upcoming_events, text)
                setOnClickPendingIntent(R.id.widget_upcoming_root, openApp)
            }
            manager.updateAppWidget(id, views)
        }
    }
}
