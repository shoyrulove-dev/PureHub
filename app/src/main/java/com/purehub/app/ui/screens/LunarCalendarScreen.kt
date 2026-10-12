package com.purehub.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.lunar.LunarCalendarConverter
import com.purehub.app.feature.lunar.LunarDayDetails
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.appText
import com.purehub.app.ui.locale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val LunarViolet = Color(0xFF7C3AED)
private val LunarGold = Color(0xFFF5B942)
private val LunarNight = Color(0xFF19122E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarCalendarScreen(innerPadding: PaddingValues, embedded: Boolean = false) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val language = LocalAppLanguage.current
    val today = LocalDate.now()
    val days = remember(month) { monthCells(month) }
    val formatter = remember(language) { DateTimeFormatter.ofPattern("MMMM yyyy", language.locale()) }
    Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TodayCard(LunarCalendarConverter.details(today), language) { month = YearMonth.now(); selected = today }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null) }
                    Text(month.format(formatter), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { weekLabels(language).forEach { Text(it, Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                LazyVerticalGrid(columns = GridCells.Fixed(7), userScrollEnabled = false, modifier = Modifier.heightIn(min = 322.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(days) { day -> if (day == null) Box(Modifier.heightIn(min = 52.dp)) else DayCell(day, today) { selected = day } }
                }
            }
        }
        Text(appText(language, "Vietnamese lunar calendar · UTC+7", "Âm lịch Việt Nam · UTC+7", "越南农历 · UTC+7", "Calendario lunar vietnamita · UTC+7"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    selected?.let { ModalBottomSheet(onDismissRequest = { selected = null }) { DetailsSheet(LunarCalendarConverter.details(it), language) } }
}

@Composable private fun TodayCard(details: LunarDayDetails, language: AppLanguage, open: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LunarNight), modifier = Modifier.fillMaxWidth().clickable(onClick = open)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.CalendarMonth, null, tint = LunarGold); Spacer(Modifier.width(8.dp)); Text(appText(language, "TODAY", "HÔM NAY", "今天", "HOY"), color = LunarGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black) }
            Text(details.solarDate.dayOfMonth.toString(), color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            Text("${details.lunarDate.day}/${details.lunarDate.month}/${details.lunarDate.year}${if (details.lunarDate.isLeapMonth) " · leap" else ""}", color = Color(0xFFE9DDFF), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${details.canChiDay} · ${details.traditionalMarker ?: details.solarTerm ?: appText(language, "Vietnamese lunar day", "Ngày âm Việt", "越南农历日", "Día lunar vietnamita")}", color = Color(0xFFCFC5E8), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun DayCell(date: LocalDate, today: LocalDate, open: () -> Unit) {
    val detail = LunarCalendarConverter.details(date)
    Surface(shape = RoundedCornerShape(14.dp), color = if (date == today) LunarViolet.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().clickable(onClick = open)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("${detail.lunarDate.day}/${detail.lunarDate.month}${if (detail.lunarDate.isLeapMonth) "*" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail.traditionalMarker != null || detail.solarTerm != null) Text("•", color = LunarGold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun DetailsSheet(details: LunarDayDetails, language: AppLanguage) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(appText(language, "Day details", "Chi tiết ngày", "日期详情", "Detalles del día"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("${details.solarDate} · ${details.lunarDate.displayText}", style = MaterialTheme.typography.titleMedium, color = LunarViolet)
        DetailRow(appText(language, "Can Chi", "Can Chi", "干支", "Can Chi"), "${details.canChiDay} · ${details.canChiMonth} · ${details.canChiYear}")
        DetailRow(appText(language, "Auspicious hours", "Giờ hoàng đạo", "吉时", "Horas favorables"), details.auspiciousHours.joinToString(" · "))
        details.solarTerm?.let { DetailRow(appText(language, "Solar term", "Tiết khí", "节气", "Término solar"), it) }
        details.traditionalMarker?.let { DetailRow(appText(language, "Traditional marker", "Mốc truyền thống", "传统节日", "Marcador tradicional"), it) }
        DetailRow(appText(language, "Moon illumination", "Độ sáng mặt trăng", "月亮照明度", "Iluminación lunar"), "${details.moonIlluminationPercent}%")
        Text(appText(language, "Calculated locally using Vietnamese UTC+7 rules.", "Tính cục bộ theo quy tắc âm lịch Việt UTC+7.", "按越南 UTC+7 规则在本地计算。", "Calculado localmente con reglas vietnamitas UTC+7."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun DetailRow(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) } }
private fun weekLabels(language: AppLanguage) = when (language) { AppLanguage.Vietnamese -> listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"); AppLanguage.Chinese -> listOf("一", "二", "三", "四", "五", "六", "日"); AppLanguage.Spanish -> listOf("L", "M", "X", "J", "V", "S", "D"); else -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
private fun monthCells(month: YearMonth): List<LocalDate?> { val first = month.atDay(1); val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7; return buildList { repeat(leading) { add(null) }; repeat(month.lengthOfMonth()) { add(month.atDay(it + 1)) } } }
