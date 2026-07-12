package com.purehub.app.feature.lunar

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import java.time.LocalDate

data class LunarDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeapMonth: Boolean,
) {
    val displayText: String
        get() = "$day/$month${if (isLeapMonth) " (Leap)" else ""}/$year"
}

data class LunarDateDescription(
    val lunarDate: LunarDate,
    val canchiLabel: String,
    val holidayLabel: String?,
)

object LunarCalendarConverter {
    private const val TIME_ZONE = 7.0 // Vietnam timezone for traditional lunar conversion
    private val heavenlyStems = listOf("Giap", "At", "Binh", "Dinh", "Mau", "Ky", "Canh", "Tan", "Nham", "Quy")
    private val earthlyBranches = listOf("Ty", "Suu", "Dan", "Mao", "Thin", "Ty.", "Ngo", "Mui", "Than", "Dau", "Tuat", "Hoi")
    private val holidayMap = mapOf(
        "1-1" to "Tet Nguyen Dan",
        "15-1" to "Ram thang Gieng",
        "3-3" to "Tet Han Thuc",
        "10-3" to "Gio To Hung Vuong",
        "15-4" to "Phat Dan",
        "5-5" to "Tet Doan Ngo",
        "15-7" to "Vu Lan",
        "15-8" to "Tet Trung Thu",
        "23-12" to "Ong Cong Ong Tao",
    )

    fun convertSolarToLunar(date: LocalDate): LunarDate {
        return convertSolarToLunar(
            day = date.dayOfMonth,
            month = date.monthValue,
            year = date.year,
        )
    }

    fun describeDate(date: LocalDate): LunarDateDescription {
        val lunar = convertSolarToLunar(date)
        val canChiYear = "${stemFor(lunar.year + 6)} ${branchFor(lunar.year + 8)}"
        val canChiMonth = "${stemFor(lunar.year * 12 + lunar.month + 3)} ${branchFor(lunar.month + 1)}"
        val julianDay = jdFromDate(date.dayOfMonth, date.monthValue, date.year)
        val canChiDay = "${stemFor(julianDay + 9)} ${branchFor(julianDay + 1)}"
        return LunarDateDescription(
            lunarDate = lunar,
            canchiLabel = "Year $canChiYear • Month $canChiMonth • Day $canChiDay",
            holidayLabel = holidayMap["${lunar.day}-${lunar.month}"],
        )
    }

    fun convertSolarToLunar(
        day: Int,
        month: Int,
        year: Int,
    ): LunarDate {
        val dayNumber = jdFromDate(day, month, year)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853)
        var monthStart = getNewMoonDay(k + 1, TIME_ZONE)
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, TIME_ZONE)
        }

        var a11 = getLunarMonth11(year, TIME_ZONE)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = year
            a11 = getLunarMonth11(year - 1, TIME_ZONE)
        } else {
            lunarYear = year + 1
            b11 = getLunarMonth11(year + 1, TIME_ZONE)
        }

        val lunarDay = dayNumber - monthStart + 1
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var lunarLeap = false
        var lunarMonth = diff + 11

        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, TIME_ZONE)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    lunarLeap = true
                }
            }
        }

        if (lunarMonth > 12) {
            lunarMonth -= 12
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1
        }

        return LunarDate(
            day = lunarDay,
            month = lunarMonth,
            year = lunarYear,
            isLeapMonth = lunarLeap,
        )
    }

    private fun jdFromDate(day: Int, month: Int, year: Int): Int {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        var jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) {
            jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        }
        return jd
    }

    private fun getNewMoonDay(k: Double, timeZone: Double): Int {
        val jd = newMoonAA98(k)
        return floor(jd + 0.5 + timeZone / 24.0).toInt()
    }

    private fun getLunarMonth11(year: Int, timeZone: Double): Int {
        val off = jdFromDate(31, 12, year) - 2415021
        val k = floor(off / 29.530588853)
        var newMoon = getNewMoonDay(k, timeZone)
        val sunLongitude = getSunLongitude(newMoon, timeZone)
        if (sunLongitude >= 9) {
            newMoon = getNewMoonDay(k - 1, timeZone)
        }
        return newMoon
    }

    private fun getLeapMonthOffset(a11: Int, timeZone: Double): Int {
        val k = floor(0.5 + (a11 - 2415021.076998695) / 29.530588853)
        var last = 0
        var index = 1
        var arc = getSunLongitude(getNewMoonDay(k + index, timeZone), timeZone)
        do {
            last = arc
            index += 1
            arc = getSunLongitude(getNewMoonDay(k + index, timeZone), timeZone)
        } while (arc != last && index < 15)
        return index - 1
    }

    private fun getSunLongitude(dayNumber: Int, timeZone: Double): Int {
        return floor(sunLongitudeAA98(dayNumber - 0.5 - timeZone / 24.0) / PI * 6.0).toInt()
    }

    private fun newMoonAA98(k: Double): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180.0
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 -= 0.4068 * sin(mpr * dr) + 0.0161 * sin(2 * dr * mpr)
        c1 -= 0.0004 * sin(3 * dr * mpr)
        c1 += 0.0104 * sin(2 * dr * f) - 0.0051 * sin((m + mpr) * dr)
        c1 -= 0.0074 * sin((m - mpr) * dr) + 0.0004 * sin((2 * f + m) * dr)
        c1 -= 0.0004 * sin((2 * f - m) * dr) - 0.0006 * sin((2 * f + mpr) * dr)
        c1 += 0.0010 * sin((2 * f - mpr) * dr) + 0.0005 * sin((2 * mpr + m) * dr)
        val deltaT = when {
            t < -11 -> {
                0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
            }

            else -> {
                -0.000278 + 0.000265 * t + 0.000262 * t2
            }
        }
        return jd1 + c1 - deltaT
    }

    private fun sunLongitudeAA98(jdn: Double): Double {
        val t = (jdn - 2451545.0) / 36525
        val t2 = t * t
        val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = l0 + dl
        l *= dr
        l -= PI * 2 * floor(l / (PI * 2))
        return l
    }

    private fun stemFor(value: Int): String = heavenlyStems[Math.floorMod(value, heavenlyStems.size)]

    private fun branchFor(value: Int): String = earthlyBranches[Math.floorMod(value, earthlyBranches.size)]
}
