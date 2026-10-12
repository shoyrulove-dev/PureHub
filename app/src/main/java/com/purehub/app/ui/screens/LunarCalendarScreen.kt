package com.purehub.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.lunar.LunarCalendarConverter
import com.purehub.app.feature.lunar.LunarDayDetails
import com.purehub.app.feature.lunar.LunarAstronomy
import com.purehub.app.feature.lunar.LunarEvent
import com.purehub.app.feature.lunar.LunarEventRepository
import com.purehub.app.feature.lunar.LunarExporter
import com.purehub.app.feature.lunar.LunarLeapRule
import com.purehub.app.feature.lunar.LunarRegion
import com.purehub.app.feature.lunar.LunarPreferences
import com.purehub.app.feature.lunar.LunarEventCategory
import com.purehub.app.feature.lunar.LunarEventRecurrence
import com.purehub.app.ui.AppLanguage
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.appText
import com.purehub.app.ui.locale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

private val LunarViolet = Color(0xFF7C3AED)
private val LunarGold = Color(0xFFF5B942)
private val LunarNight = Color(0xFF19122E)
private enum class LunarSection { Calendar, Convert, Upcoming, Events, Export, Moon, Method }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunarCalendarScreen(innerPadding: PaddingValues, embedded: Boolean = false) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var section by remember { mutableStateOf(LunarSection.Calendar) }
    val context = LocalContext.current
    val eventRepository = remember { LunarEventRepository(context) }
    var events by remember { mutableStateOf(eventRepository.load()) }
    var quickEventDate by remember { mutableStateOf<LocalDate?>(null) }
    var region by remember { mutableStateOf(LunarPreferences.region(context)) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    val today = LocalDate.now()
    val days = remember(month) { monthCells(month) }
    val formatter = remember(language) { DateTimeFormatter.ofPattern("MMMM yyyy", language.locale()) }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            LunarDrawer(language, section, region, innerPadding.calculateTopPadding(), { newRegion ->
                region = newRegion
                LunarPreferences.setRegion(context, newRegion)
            }) {
                section = it
                scope.launch { drawerState.close() }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TodayCard(LunarCalendarConverter.details(today, region), language, { month = YearMonth.now(); selected = today }) { scope.launch { drawerState.open() } }
            SectionHeading(section, language) { scope.launch { drawerState.open() } }
            when (section) {
                LunarSection.Calendar -> {
                    CalendarPanel(month, formatter, language, days, today, region, events, { month = month.minusMonths(1) }, { month = month.plusMonths(1) }) { selected = it }
                    UpcomingPreview(language, today, region) { section = LunarSection.Upcoming }
                }
                LunarSection.Convert -> ConversionPanel(language, region)
                LunarSection.Upcoming -> UpcomingPanel(language, today, region) { selected = it }
                LunarSection.Events -> EventsPanel(language, region, events, quickEventDate, { quickEventDate = null }) { updated -> events = updated; eventRepository.save(updated) }
                LunarSection.Export -> ExportPanel(language, month, today)
                LunarSection.Moon -> MoonPanel(language, today)
                LunarSection.Method -> MethodPanel(language, region)
            }
            Text(regionLabel(region, language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    selected?.let { date -> ModalBottomSheet(onDismissRequest = { selected = null }) { DetailsSheet(LunarCalendarConverter.details(date, region), language, region) { quickEventDate = date; selected = null; section = LunarSection.Events } } }
}

@Composable
private fun LunarDrawer(language: AppLanguage, current: LunarSection, region: LunarRegion, topPadding: Dp, setRegion: (LunarRegion) -> Unit, select: (LunarSection) -> Unit) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.52f).padding(top = topPadding),
        drawerContainerColor = LunarNight,
        drawerContentColor = Color.White,
    ) {
        Column(
            Modifier
                .padding(horizontal = 12.dp, vertical = 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = LunarViolet.copy(alpha = .28f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Icon(Icons.Rounded.DarkMode, null, tint = LunarGold)
                    Text("Lunar", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(region == LunarRegion.Vietnam, { setRegion(LunarRegion.Vietnam) }, { Text("VN +7", maxLines = 1) }, modifier = Modifier.weight(1f))
                FilterChip(region == LunarRegion.China, { setRegion(LunarRegion.China) }, { Text("CN +8", maxLines = 1) }, modifier = Modifier.weight(1f))
            }
            listOf(
                Triple(LunarSection.Calendar, Icons.Rounded.CalendarMonth, appText(language, "Calendar", "Lịch tháng", "月历", "Calendario")),
                Triple(LunarSection.Convert, Icons.AutoMirrored.Rounded.CompareArrows, appText(language, "Date converter", "Đổi ngày", "日期转换", "Conversor")),
                Triple(LunarSection.Upcoming, Icons.Rounded.EventRepeat, appText(language, "Upcoming", "Sắp tới", "近期", "Próximos")),
                Triple(LunarSection.Events, Icons.Rounded.FamilyRestroom, appText(language, "Reminders", "Nhắc việc", "提醒", "Recordatorios")),
                Triple(LunarSection.Export, Icons.Rounded.IosShare, appText(language, "Export & backup", "Xuất & sao lưu", "导出与备份", "Exportar y copia")),
                Triple(LunarSection.Moon, Icons.Rounded.LocationOn, appText(language, "Moon", "Mặt trăng", "月亮", "Luna")),
                Triple(LunarSection.Method, Icons.Rounded.Info, appText(language, "Method", "Phương pháp", "算法", "Método")),
            ).forEach { (item, icon, label) ->
                NavigationDrawerItem(
                    label = { Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge) },
                    selected = current == item,
                    icon = { Icon(icon, null) },
                    onClick = { select(item) },
                    modifier = Modifier.height(48.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = LunarViolet,
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = Color(0xFFD8CCF2),
                        unselectedTextColor = Color.White,
                    ),
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = .16f))
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Widgets, null, tint = LunarGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Text(appText(language, "Add the Today widget from your Home screen.", "Thêm widget Hôm nay từ màn hình chính.", "从主屏幕添加今日小组件。", "Añade el widget Hoy desde Inicio."), color = Color(0xFFD8CCF2), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SectionHeading(section: LunarSection, language: AppLanguage, openMenu: () -> Unit) {
    val title = when (section) {
        LunarSection.Calendar -> appText(language, "Month calendar", "Lịch tháng", "月历", "Calendario mensual")
        LunarSection.Convert -> appText(language, "Date converter", "Đổi ngày", "日期转换", "Conversor de fechas")
        LunarSection.Upcoming -> appText(language, "Upcoming days", "Ngày sắp tới", "近期日期", "Próximos días")
        LunarSection.Events -> appText(language, "Family reminders", "Nhắc việc gia đình", "家庭提醒", "Recordatorios familiares")
        LunarSection.Export -> appText(language, "Export & backup", "Xuất & sao lưu", "导出与备份", "Exportar y copia")
        LunarSection.Moon -> appText(language, "Moon by location", "Trăng theo vị trí", "按位置看月亮", "Luna por ubicación")
        LunarSection.Method -> appText(language, "Calculation method", "Phương pháp tính", "计算方法", "Método de cálculo")
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = openMenu, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = LunarViolet.copy(alpha = .14f))) { Icon(Icons.Rounded.Menu, null, tint = LunarViolet) }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun CalendarPanel(month: YearMonth, formatter: DateTimeFormatter, language: AppLanguage, days: List<LocalDate?>, today: LocalDate, region: LunarRegion, events: List<LunarEvent>, previous: () -> Unit, next: () -> Unit, select: (LocalDate) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = previous) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null) }
                Text(month.format(formatter), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                IconButton(onClick = next) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { weekLabels(language).forEach { Text(it, Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            LazyVerticalGrid(columns = GridCells.Fixed(7), userScrollEnabled = false, modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(days) { day -> if (day == null) Box(Modifier.heightIn(min = 52.dp)) else DayCell(day, today, region, events) { select(day) } }
            }
        }
    }
}

@Composable
private fun ConversionPanel(language: AppLanguage, region: LunarRegion) {
    var solarDate by remember { mutableStateOf(LocalDate.now()) }
    var lunarDay by remember { mutableStateOf("1") }
    var lunarMonth by remember { mutableStateOf("1") }
    var lunarYear by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var leap by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val converted = remember(lunarDay, lunarMonth, lunarYear, leap, region) {
        val day = lunarDay.toIntOrNull(); val month = lunarMonth.toIntOrNull(); val year = lunarYear.toIntOrNull()
        if (day in 1..30 && month in 1..12 && year != null && year in 1900..2100) LunarCalendarConverter.convertLunarToSolar(com.purehub.app.feature.lunar.LunarDate(day!!, month!!, year, leap), region) else null
    }
    val solarLunar = remember(solarDate, region) { LunarCalendarConverter.convertSolarToLunar(solarDate, region) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(appText(language, "Solar → lunar", "Dương → âm", "公历 → 农历", "Solar → lunar"), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) { Text("$solarDate  →  ${solarLunar.day}/${solarLunar.month}/${solarLunar.year}${leapLabel(solarLunar.isLeapMonth, language)}") }
            HorizontalDivider()
            Text(appText(language, "Lunar → solar", "Âm → dương", "农历 → 公历", "Lunar → solar"), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(lunarDay, { lunarDay = it }, appText(language, "Day", "Ngày", "日", "Día"), Modifier.weight(1f))
                NumberField(lunarMonth, { lunarMonth = it }, appText(language, "Month", "Tháng", "月", "Mes"), Modifier.weight(1f))
                NumberField(lunarYear, { lunarYear = it }, appText(language, "Year", "Năm", "年", "Año"), Modifier.weight(1.35f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(leap, { leap = it }); Spacer(Modifier.width(8.dp)); Text(appText(language, "Leap month", "Tháng nhuận", "闰月", "Mes intercalar")) }
            Surface(color = LunarViolet.copy(alpha = .12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(converted?.toString() ?: appText(language, "No matching date", "Không có ngày phù hợp", "没有匹配日期", "No hay fecha válida"), Modifier.padding(14.dp), fontWeight = FontWeight.Bold, color = LunarViolet)
            }
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = solarDate.toEpochDay() * 86_400_000L)
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = { TextButton(onClick = { pickerState.selectedDateMillis?.let { solarDate = LocalDate.ofEpochDay(it / 86_400_000L) }; showPicker = false }) { Text(appText(language, "Select", "Chọn", "选择", "Elegir")) } }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text(appText(language, "Cancel", "Hủy", "取消", "Cancelar")) } }) { DatePicker(pickerState) }
    }
}

@Composable
private fun NumberField(value: String, change: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(value, { change(it.filter(Char::isDigit).take(4)) }, modifier = modifier, label = { Text(label) }, singleLine = true)
}

@Composable
private fun UpcomingPanel(language: AppLanguage, today: LocalDate, region: LunarRegion, select: (LocalDate) -> Unit) {
    val markers = remember(today, region) { LunarCalendarConverter.upcomingMarkers(today, 60, region).take(12) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(appText(language, "Next 60 days", "60 ngày tới", "未来 60 天", "Próximos 60 días"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            markers.forEach { detail ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().clickable { select(detail.solarDate) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        MarkerBadge(detail)
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(localizedMarker(detail.traditionalMarker ?: detail.solarTerm.orEmpty(), language), fontWeight = FontWeight.Bold)
                            Text("${detail.solarDate} · ${detail.lunarDate.day}/${detail.lunarDate.month}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${detail.moonIlluminationPercent}%", color = LunarViolet, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsPanel(language: AppLanguage, region: LunarRegion, events: List<LunarEvent>, quickDate: LocalDate?, consumeQuickDate: () -> Unit, updateEvents: (List<LunarEvent>) -> Unit) {
    val context = LocalContext.current
    val quickLunar = remember(quickDate, region) { quickDate?.let { LunarCalendarConverter.convertSolarToLunar(it, region) } }
    var title by remember { mutableStateOf("") }; var day by remember { mutableStateOf(quickLunar?.day?.toString() ?: "1") }; var month by remember { mutableStateOf(quickLunar?.month?.toString() ?: "1") }
    var hour by remember { mutableStateOf("8") }; var minute by remember { mutableStateOf("00") }
    var offsets by remember { mutableStateOf(setOf(0)) }; var leapRule by remember { mutableStateOf(LunarLeapRule.MainMonth) }; var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(LunarEventCategory.Family) }; var recurrence by remember { mutableStateOf(LunarEventRecurrence.Yearly) }
    var showTrash by remember { mutableStateOf(false) }; var categoryFilter by remember { mutableStateOf<LunarEventCategory?>(null) }
    LaunchedEffect(quickLunar) { quickLunar?.let { day = it.day.toString(); month = it.month.toString(); leapRule = if (it.isLeapMonth) LunarLeapRule.LeapMonth else LunarLeapRule.MainMonth; consumeQuickDate() } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(appText(language, "Private lunar reminders", "Nhắc ngày âm riêng tư", "私密农历提醒", "Recordatorios lunares privados"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), label = { Text(appText(language, "Name", "Tên sự kiện", "名称", "Nombre")) }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(day, { day = it }, appText(language, "Day", "Ngày", "日", "Día"), Modifier.weight(1f))
                NumberField(month, { month = it }, appText(language, "Month", "Tháng", "月", "Mes"), Modifier.weight(1f))
                NumberField(hour, { hour = it }, appText(language, "Hour", "Giờ", "时", "Hora"), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(minute, { minute = it }, appText(language, "Minute", "Phút", "分", "Min"), Modifier.weight(.8f))
                Column(Modifier.weight(1.7f)) {
                    Text(appText(language, "Notify before (days)", "Báo trước (ngày)", "提前提醒（天）", "Avisar antes (días)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf(0, 1, 3, 7).forEach { value -> FilterChip(value in offsets, { offsets = if (value in offsets && offsets.size > 1) offsets - value else offsets + value }, { Text(value.toString(), maxLines = 1) }, modifier = Modifier.weight(1f)) } }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(
                    LunarLeapRule.MainMonth to appText(language, "Main", "Tháng chính", "正常月", "Normal"),
                    LunarLeapRule.LeapMonth to appText(language, "Leap", "Tháng nhuận", "闰月", "Intercalar"),
                    LunarLeapRule.Both to appText(language, "Both", "Cả hai", "两者", "Ambos"),
                ).forEach { (rule, label) -> FilterChip(leapRule == rule, { leapRule = rule }, { Text(label, maxLines = 1) }, modifier = Modifier.weight(1f)) }
            }
            LunarEventCategory.entries.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { item -> FilterChip(category == item, { category = item }, { Text(categoryLabel(item, language), maxLines = 1) }, modifier = Modifier.weight(1f)) }
            } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(appText(language, "Repeat", "Lặp", "重复", "Repetir"), Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    LunarEventRecurrence.entries.forEachIndexed { index, item -> SegmentedButton(recurrence == item, { recurrence = item }, SegmentedButtonDefaults.itemShape(index, 2)) { Text(if (item == LunarEventRecurrence.Yearly) appText(language, "Year", "Năm", "每年", "Año") else appText(language, "Month", "Tháng", "每月", "Mes")) } }
                }
            }
            Button(onClick = {
                val parsedDay = day.toIntOrNull(); val parsedMonth = month.toIntOrNull(); val parsedHour = hour.toIntOrNull(); val parsedMinute = minute.toIntOrNull()
                if (title.isNotBlank() && parsedDay in 1..30 && parsedMonth in 1..12 && parsedHour in 0..23 && parsedMinute in 0..59) {
                    updateEvents(events + LunarEvent(title = title.trim(), day = parsedDay!!, month = parsedMonth!!, leapRule = leapRule, reminderOffsets = offsets.sortedDescending(), reminderHour = parsedHour!!, reminderMinute = parsedMinute!!, category = category, recurrence = recurrence))
                    title = ""
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(appText(language, "Save reminder", "Lưu nhắc việc", "保存提醒", "Guardar recordatorio")) }
        }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(appText(language, "Search saved dates", "Tìm ngày đã lưu", "搜索保存的日期", "Buscar fechas")) }, singleLine = true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        FilterChip(categoryFilter == null, { categoryFilter = null }, { Text(appText(language, "All", "Tất cả", "全部", "Todo")) })
        LunarEventCategory.entries.forEach { item -> FilterChip(categoryFilter == item, { categoryFilter = item }, { Text(categoryLabel(item, language), maxLines = 1) }) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) { Switch(showTrash, { showTrash = it }); Spacer(Modifier.width(8.dp)); Text(appText(language, "Trash", "Thùng rác", "回收站", "Papelera")) }
    val filtered = events.filter { (if (showTrash) it.deletedAtEpochMs != null else it.deletedAtEpochMs == null) && (categoryFilter == null || it.category == categoryFilter) && (query.isBlank() || it.title.contains(query, ignoreCase = true)) }
    filtered.forEach { event ->
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EventRepeat, null, tint = LunarViolet)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(event.title, fontWeight = FontWeight.Bold); Text("${event.day}/${event.month} · ${categoryLabel(event.category, language)} · %02d:%02d · ${event.reminderOffsets.joinToString("/") { "${it}d" }}".format(event.reminderHour, event.reminderMinute), style = MaterialTheme.typography.bodySmall); Text(nextEventLabel(event, region, language), color = LunarViolet, style = MaterialTheme.typography.labelSmall) }
                if (!showTrash) Switch(event.enabled, { enabled -> updateEvents(events.map { if (it.id == event.id) it.copy(enabled = enabled) else it }) })
                TextButton(onClick = { updateEvents(if (showTrash) events.map { if (it.id == event.id) it.copy(deletedAtEpochMs = null) else it } else events.map { if (it.id == event.id) it.copy(deletedAtEpochMs = System.currentTimeMillis()) else it }) }) { Text(if (showTrash) appText(language, "Restore", "Khôi phục", "恢复", "Restaurar") else appText(language, "Delete", "Xóa", "删除", "Borrar")) }
            }
        }
    }
}

@Composable
private fun ExportPanel(language: AppLanguage, month: YearMonth, today: LocalDate) {
    val context = LocalContext.current
    val repository = remember { LunarEventRepository(context) }
    var passphrase by remember { mutableStateOf("") }; var status by remember { mutableStateOf("") }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Empty file")
            repository.importJson(LunarExporter.decryptBackup(bytes, passphrase))
        }.onSuccess { status = appText(language, "Backup restored", "Đã khôi phục", "备份已恢复", "Copia restaurada") }
            .onFailure { status = appText(language, "Check file or passphrase", "Kiểm tra file hoặc mật khẩu", "请检查文件或密码", "Comprueba archivo o clave") }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(appText(language, "Share calendar", "Chia sẻ lịch", "分享日历", "Compartir calendario"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Button({ LunarExporter.shareDayIcs(context, today) }, Modifier.fillMaxWidth()) { Text(appText(language, "Export today as ICS", "Xuất hôm nay dạng ICS", "导出今日 ICS", "Exportar hoy como ICS")) }
            OutlinedButton({ LunarExporter.shareMonthPng(context, month) }, Modifier.fillMaxWidth()) { Text(appText(language, "Share month image", "Chia sẻ ảnh tháng", "分享月份图片", "Compartir imagen mensual")) }
            OutlinedButton({ LunarExporter.shareMonthPdf(context, month) }, Modifier.fillMaxWidth()) { Text(appText(language, "Share month PDF", "Chia sẻ PDF tháng", "分享月份 PDF", "Compartir PDF mensual")) }
            HorizontalDivider()
            Text(appText(language, "Encrypted local backup", "Sao lưu mã hóa cục bộ", "本地加密备份", "Copia local cifrada"), fontWeight = FontWeight.Black)
            OutlinedTextField(passphrase, { passphrase = it }, Modifier.fillMaxWidth(), label = { Text(appText(language, "Passphrase (8+ characters)", "Mật khẩu (từ 8 ký tự)", "密码（至少 8 个字符）", "Clave (8+ caracteres)")) }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { runCatching { LunarExporter.shareEncryptedBackup(context, repository.exportJson(), passphrase) }.onFailure { status = it.message.orEmpty() } }, enabled = passphrase.length >= 8, modifier = Modifier.weight(1f)) { Text(appText(language, "Backup", "Sao lưu", "备份", "Copia")) }
                OutlinedButton(onClick = { importBackup.launch("application/octet-stream") }, enabled = passphrase.length >= 8, modifier = Modifier.weight(1f)) { Text(appText(language, "Restore", "Khôi phục", "恢复", "Restaurar")) }
            }
            if (status.isNotBlank()) Text(status, color = LunarViolet, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MoonPanel(language: AppLanguage, today: LocalDate) {
    val context = LocalContext.current
    var coordinates by remember { mutableStateOf(readLastLocation(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) coordinates = readLastLocation(context) }
    val sky = remember(coordinates, today) { coordinates?.let { LunarAstronomy.calculate(today, it.first, it.second) } }
    Card(colors = CardDefaults.cardColors(containerColor = LunarNight), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.DarkMode, null, tint = LunarGold); Spacer(Modifier.width(9.dp)); Text(appText(language, "Local moon", "Trăng tại vị trí", "本地月亮", "Luna local"), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            if (sky == null) {
                Text(appText(language, "Location is used only after you enable it and never leaves this device.", "Vị trí chỉ được dùng khi bạn bật và không rời khỏi thiết bị.", "仅在您启用后使用位置，数据不会离开设备。", "La ubicación solo se usa al activarla y no sale del dispositivo."), color = Color(0xFFD8CCF2))
                Button(onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Text(appText(language, "Use my location", "Dùng vị trí", "使用我的位置", "Usar mi ubicación")) }
            } else {
                Text("${sky.illuminationPercent}%", color = LunarGold, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                DetailRowLight(appText(language, "Moon age", "Tuổi trăng", "月龄", "Edad lunar"), "%.1f %s".format(sky.moonAgeDays, appText(language, "days", "ngày", "天", "días")))
                DetailRowLight(appText(language, "Moonrise", "Trăng mọc", "月出", "Salida"), sky.moonrise?.toString() ?: "—")
                DetailRowLight(appText(language, "Moonset", "Trăng lặn", "月落", "Puesta"), sky.moonset?.toString() ?: "—")
                DetailRowLight(appText(language, "Altitude", "Độ cao", "高度", "Altitud"), "%.1f°".format(sky.altitudeDegrees))
                DetailRowLight(appText(language, "Azimuth", "Phương vị", "方位角", "Acimut"), "%.1f°".format(sky.azimuthDegrees))
                DetailRowLight(appText(language, "Next new moon", "Trăng non tới", "下次新月", "Próxima luna nueva"), sky.nextNewMoon.toLocalDate().toString())
                DetailRowLight(appText(language, "Next full moon", "Trăng tròn tới", "下次满月", "Próxima luna llena"), sky.nextFullMoon.toLocalDate().toString())
                Text(appText(language, "Offline astronomical estimate for the selected location.", "Ước tính thiên văn ngoại tuyến theo vị trí đã chọn.", "根据所选位置进行离线天文估算。", "Estimación astronómica sin conexión para la ubicación."), color = Color(0xFFD8CCF2), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MethodPanel(language: AppLanguage, region: LunarRegion) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(appText(language, "Calculation transparency", "Minh bạch phép tính", "计算透明度", "Transparencia del cálculo"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            DetailRow(appText(language, "Region", "Khu vực", "地区", "Región"), regionLabel(region, language))
            DetailRow(appText(language, "Supported range", "Khoảng hỗ trợ", "支持范围", "Rango compatible"), "1900–2100")
            DetailRow(appText(language, "Calendar engine", "Thuật toán lịch", "历法算法", "Motor de calendario"), "Astronomical Algorithms (Meeus) · local new moon and solar longitude")
            DetailRow(appText(language, "Validation", "Kiểm thử", "验证", "Validación"), appText(language, "Round-trip, leap-month, Tết and solar-term tests", "Test hai chiều, tháng nhuận, Tết và tiết khí", "双向、闰月、春节和节气测试", "Pruebas bidireccionales, mes intercalar, Tết y términos solares"))
            Text(appText(language, "Astronomy values are offline estimates for planning, not navigation.", "Số liệu thiên văn là ước tính ngoại tuyến để tham khảo, không dùng để định vị.", "天文数据为离线规划估算，不用于导航。", "Los datos astronómicos son estimaciones sin conexión, no aptas para navegación."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun DetailRowLight(label: String, value: String) { Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = Color(0xFFD8CCF2)); Text(value, color = Color.White, fontWeight = FontWeight.Bold) } }

private fun readLastLocation(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    return runCatching {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }?.let { it.latitude to it.longitude }
    }.getOrNull()
}

@Composable
private fun UpcomingPreview(language: AppLanguage, today: LocalDate, region: LunarRegion, openAll: () -> Unit) {
    val markers = remember(today, region) { LunarCalendarConverter.upcomingMarkers(today, 35, region).take(3) }
    Card(colors = CardDefaults.cardColors(containerColor = LunarNight.copy(alpha = .97f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(appText(language, "Coming up", "Sắp tới", "即将到来", "Próximamente"), Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Black)
                TextButton(onClick = openAll) { Text(appText(language, "View all", "Xem hết", "查看全部", "Ver todo"), color = LunarGold) }
            }
            markers.forEach { detail ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MarkerBadge(detail)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(localizedMarker(detail.traditionalMarker ?: detail.solarTerm.orEmpty(), language), color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${detail.solarDate} · ${detail.lunarDate.day}/${detail.lunarDate.month}", color = Color(0xFFCFC5E8), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerBadge(details: LunarDayDetails) {
    val festival = details.traditionalMarker != null && details.traditionalMarker !in setOf("Mùng một", "Ngày rằm")
    val icon = when { festival -> Icons.Rounded.Celebration; details.solarTerm != null -> Icons.Rounded.WbSunny; else -> Icons.Rounded.DarkMode }
    val tint = when { festival -> Color(0xFFFFB74D); details.solarTerm != null -> Color(0xFFF59E0B); else -> LunarViolet }
    Surface(shape = RoundedCornerShape(12.dp), color = tint.copy(alpha = .16f), modifier = Modifier.size(42.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
    }
}

@Composable private fun TodayCard(details: LunarDayDetails, language: AppLanguage, open: () -> Unit, openMenu: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = LunarNight), modifier = Modifier.fillMaxWidth().clickable(onClick = open)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = openMenu) { Icon(Icons.Rounded.Menu, appText(language, "Open menu", "Mở menu", "打开菜单", "Abrir menú"), tint = Color.White) }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.DarkMode, null, tint = LunarGold)
                Spacer(Modifier.width(8.dp))
                Text(appText(language, "TODAY", "HÔM NAY", "今天", "HOY"), Modifier.weight(1f), color = LunarGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(details.solarDate.dayOfMonth.toString(), color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(details.solarDate.format(DateTimeFormatter.ofPattern("EEEE", language.locale())), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(details.solarDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", language.locale())), color = Color(0xFFCFC5E8), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = LunarViolet.copy(alpha = .28f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DarkMode, null, tint = LunarGold)
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("${details.lunarDate.day}/${details.lunarDate.month}/${details.lunarDate.year}${leapLabel(details.lunarDate.isLeapMonth, language)}", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${details.canChiDay} · ${localizedMarker(details.traditionalMarker ?: details.solarTerm ?: appText(language, "Lunar day", "Ngày âm", "农历日", "Día lunar"), language)}", color = Color(0xFFE9DDFF), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable private fun DayCell(date: LocalDate, today: LocalDate, region: LunarRegion, events: List<LunarEvent>, open: () -> Unit) {
    val detail = LunarCalendarConverter.details(date, region)
    val hasEvent = events.any { event -> event.enabled && event.deletedAtEpochMs == null && event.day == detail.lunarDate.day && (event.recurrence == LunarEventRecurrence.Monthly || event.month == detail.lunarDate.month) }
    Surface(shape = RoundedCornerShape(14.dp), color = if (date == today) LunarViolet.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().clickable(onClick = open)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("${detail.lunarDate.day}/${detail.lunarDate.month}${if (detail.lunarDate.isLeapMonth) "*" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail.traditionalMarker != null || detail.solarTerm != null || hasEvent) Text(if (hasEvent) "◆" else if (detail.solarTerm != null) "☀" else "●", color = if (hasEvent) LunarViolet else LunarGold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun DetailsSheet(details: LunarDayDetails, language: AppLanguage, region: LunarRegion, addReminder: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(appText(language, "Day details", "Chi tiết ngày", "日期详情", "Detalles del día"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("${details.solarDate} · ${details.lunarDate.displayText}", style = MaterialTheme.typography.titleMedium, color = LunarViolet)
        DetailRow(appText(language, "Can Chi", "Can Chi", "干支", "Can Chi"), "${details.canChiDay} · ${details.canChiMonth} · ${details.canChiYear}")
        DetailRow(appText(language, "Auspicious hours", "Giờ hoàng đạo", "吉时", "Horas favorables"), details.auspiciousHours.joinToString(" · "))
        details.solarTerm?.let { DetailRow(appText(language, "Solar term", "Tiết khí", "节气", "Término solar"), localizedMarker(it, language)) }
        details.traditionalMarker?.let { DetailRow(appText(language, "Traditional marker", "Mốc truyền thống", "传统节日", "Marcador tradicional"), localizedMarker(it, language)) }
        DetailRow(appText(language, "Estimated moon illumination", "Độ sáng trăng ước tính", "月光照明估算", "Iluminación lunar estimada"), "${details.moonIlluminationPercent}%")
        Text(regionLabel(region, language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(addReminder, Modifier.fillMaxWidth()) { Text(appText(language, "Add reminder", "Thêm nhắc việc", "添加提醒", "Añadir recordatorio")) }
    }
}

@Composable private fun DetailRow(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) } }
private fun weekLabels(language: AppLanguage) = when (language) { AppLanguage.Vietnamese -> listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"); AppLanguage.Chinese -> listOf("一", "二", "三", "四", "五", "六", "日"); AppLanguage.Spanish -> listOf("L", "M", "X", "J", "V", "S", "D"); else -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }
private fun monthCells(month: YearMonth): List<LocalDate?> { val first = month.atDay(1); val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7; return buildList { repeat(leading) { add(null) }; repeat(month.lengthOfMonth()) { add(month.atDay(it + 1)) } } }

private fun leapLabel(leap: Boolean, language: AppLanguage): String = if (!leap) "" else appText(language, " · leap", " · nhuận", " · 闰月", " · intercalar")

private fun regionLabel(region: LunarRegion, language: AppLanguage): String = when (region) {
    LunarRegion.Vietnam -> appText(language, "Vietnam lunar · UTC+7", "Âm lịch Việt Nam · UTC+7", "越南阴历 · UTC+7", "Calendario lunar vietnamita · UTC+7")
    LunarRegion.China -> appText(language, "Chinese lunar · UTC+8", "Nông lịch Trung Quốc · UTC+8", "中国农历 · UTC+8", "Calendario lunar chino · UTC+8")
}

private fun categoryLabel(category: LunarEventCategory, language: AppLanguage): String = when (category) {
    LunarEventCategory.Family -> appText(language, "Family", "Gia đình", "家庭", "Familia")
    LunarEventCategory.Worship -> appText(language, "Ritual", "Cúng lễ", "祭祀", "Ritual")
    LunarEventCategory.Birthday -> appText(language, "Birthday", "Sinh nhật", "生日", "Cumpleaños")
    LunarEventCategory.Personal -> appText(language, "Personal", "Cá nhân", "个人", "Personal")
}

private fun nextEventLabel(event: LunarEvent, region: LunarRegion, language: AppLanguage): String {
    val today = LocalDate.now()
    val next = (0L..430L).firstOrNull { offset ->
        val lunar = LunarCalendarConverter.convertSolarToLunar(today.plusDays(offset), region)
        val leapMatches = when (event.leapRule) { LunarLeapRule.MainMonth -> !lunar.isLeapMonth; LunarLeapRule.LeapMonth -> lunar.isLeapMonth; LunarLeapRule.Both -> true }
        lunar.day == event.day && (event.recurrence == LunarEventRecurrence.Monthly || lunar.month == event.month) && leapMatches
    }
    return when {
        next == null -> appText(language, "No date in range", "Không có ngày trong phạm vi", "范围内无日期", "Sin fecha en el rango")
        next == 0L -> appText(language, "Today", "Hôm nay", "今天", "Hoy")
        else -> appText(language, "In $next days", "Còn $next ngày", "$next 天后", "En $next días")
    }
}

private fun localizedMarker(value: String, language: AppLanguage): String {
    if (language == AppLanguage.Vietnamese) return value
    val translations = mapOf(
        "Tết Nguyên Đán" to listOf("Vietnamese Lunar New Year", "越南春节", "Año Nuevo lunar vietnamita"),
        "Rằm tháng Giêng" to listOf("First full moon", "元宵节", "Primera luna llena"),
        "Tết Hàn Thực" to listOf("Cold Food Festival", "寒食节", "Festival de comida fría"),
        "Giỗ Tổ Hùng Vương" to listOf("Hùng Kings Festival", "雄王祭祖日", "Fiesta de los Reyes Hùng"),
        "Phật Đản" to listOf("Buddha's Birthday", "佛诞", "Nacimiento de Buda"),
        "Tết Đoan Ngọ" to listOf("Đoan Ngọ Festival", "端午节", "Festival Đoan Ngọ"),
        "Vu Lan" to listOf("Vu Lan Festival", "盂兰盆节", "Festival Vu Lan"),
        "Tết Trung Thu" to listOf("Mid-Autumn Festival", "中秋节", "Festival del Medio Otoño"),
        "Ông Công Ông Táo" to listOf("Kitchen Gods Day", "灶王节", "Día de los dioses de la cocina"),
        "Mùng một" to listOf("New moon day", "初一", "Día de luna nueva"),
        "Ngày rằm" to listOf("Full moon day", "十五", "Día de luna llena"),
        "Xuân phân" to listOf("Spring equinox", "春分", "Equinoccio de primavera"), "Thanh minh" to listOf("Pure Brightness", "清明", "Claridad pura"),
        "Cốc vũ" to listOf("Grain Rain", "谷雨", "Lluvia de grano"), "Lập hạ" to listOf("Start of summer", "立夏", "Inicio del verano"),
        "Tiểu mãn" to listOf("Grain Buds", "小满", "Grano lleno"), "Mang chủng" to listOf("Grain in Ear", "芒种", "Grano en espiga"),
        "Hạ chí" to listOf("Summer solstice", "夏至", "Solsticio de verano"), "Tiểu thử" to listOf("Minor Heat", "小暑", "Calor menor"),
        "Đại thử" to listOf("Major Heat", "大暑", "Calor mayor"), "Lập thu" to listOf("Start of autumn", "立秋", "Inicio del otoño"),
        "Xử thử" to listOf("End of Heat", "处暑", "Fin del calor"), "Bạch lộ" to listOf("White Dew", "白露", "Rocío blanco"),
        "Thu phân" to listOf("Autumn equinox", "秋分", "Equinoccio de otoño"), "Hàn lộ" to listOf("Cold Dew", "寒露", "Rocío frío"),
        "Sương giáng" to listOf("Frost Descent", "霜降", "Caída de escarcha"), "Lập đông" to listOf("Start of winter", "立冬", "Inicio del invierno"),
        "Tiểu tuyết" to listOf("Minor Snow", "小雪", "Nieve menor"), "Đại tuyết" to listOf("Major Snow", "大雪", "Nieve mayor"),
        "Đông chí" to listOf("Winter solstice", "冬至", "Solsticio de invierno"), "Tiểu hàn" to listOf("Minor Cold", "小寒", "Frío menor"),
        "Đại hàn" to listOf("Major Cold", "大寒", "Frío mayor"), "Lập xuân" to listOf("Start of spring", "立春", "Inicio de la primavera"),
        "Vũ thủy" to listOf("Rain Water", "雨水", "Agua de lluvia"), "Kinh trập" to listOf("Awakening of Insects", "惊蛰", "Despertar de los insectos"),
    )
    val translated = translations[value] ?: return value
    return when (language) { AppLanguage.Chinese -> translated[1]; AppLanguage.Spanish -> translated[2]; else -> translated[0] }
}
