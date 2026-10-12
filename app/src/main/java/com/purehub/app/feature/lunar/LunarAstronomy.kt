package com.purehub.app.feature.lunar

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import kotlin.math.*

data class LunarSkyData(
    val illuminationPercent: Int,
    val moonAgeDays: Double,
    val moonrise: LocalTime?,
    val moonset: LocalTime?,
    val altitudeDegrees: Double,
    val azimuthDegrees: Double,
    val nextNewMoon: ZonedDateTime,
    val nextFullMoon: ZonedDateTime,
)

/** Offline low-precision astronomy suitable for a daily calendar, not navigation. */
object LunarAstronomy {
    private const val SYNODIC_MONTH = 29.530588853

    fun calculate(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId = ZoneId.systemDefault()): LunarSkyData {
        val start = date.atStartOfDay(zone).toInstant()
        val moon = equatorial(julian(start), moon = true)
        val sun = equatorial(julian(start), moon = false)
        val elongation = acos((sin(moon.dec) * sin(sun.dec) + cos(moon.dec) * cos(sun.dec) * cos(moon.ra - sun.ra)).coerceIn(-1.0, 1.0))
        val illumination = ((1.0 - cos(elongation)) * 50.0).roundToInt().coerceIn(0, 100)
        val phaseAngle = normalizeRadians(moon.eclipticLongitude - sun.eclipticLongitude)
        val age = phaseAngle / (2 * PI) * SYNODIC_MONTH
        val observation = date.atTime(12, 0).atZone(zone).toInstant()
        val horizontal = horizontal(observation, latitude, longitude)
        var rise: Instant? = null; var set: Instant? = null
        var previousTime = start; var previousAltitude = altitude(previousTime, latitude, longitude)
        for (step in 1..72) {
            val currentTime = start.plus(Duration.ofMinutes(step * 20L))
            val currentAltitude = altitude(currentTime, latitude, longitude)
            if (previousAltitude < -0.3 && currentAltitude >= -0.3 && rise == null) rise = interpolate(previousTime, currentTime, previousAltitude, currentAltitude, -0.3)
            if (previousAltitude >= -0.3 && currentAltitude < -0.3 && set == null) set = interpolate(previousTime, currentTime, previousAltitude, currentAltitude, -0.3)
            previousTime = currentTime; previousAltitude = currentAltitude
        }
        val nextNewDays = if (age < 0.05) SYNODIC_MONTH else SYNODIC_MONTH - age
        val nextFullDays = ((SYNODIC_MONTH / 2.0 - age) + SYNODIC_MONTH) % SYNODIC_MONTH
        return LunarSkyData(
            illumination, age, rise?.atZone(zone)?.toLocalTime(), set?.atZone(zone)?.toLocalTime(),
            horizontal.first, horizontal.second,
            observation.plusSeconds((nextNewDays * 86_400).roundToLong()).atZone(zone),
            observation.plusSeconds((nextFullDays * 86_400).roundToLong()).atZone(zone),
        )
    }

    private data class Equatorial(val ra: Double, val dec: Double, val eclipticLongitude: Double)

    private fun altitude(time: Instant, latitude: Double, longitude: Double): Double = horizontal(time, latitude, longitude).first

    private fun horizontal(time: Instant, latitude: Double, longitude: Double): Pair<Double, Double> {
        val jd = julian(time); val position = equatorial(jd, moon = true)
        val sidereal = normalizeDegrees(280.46061837 + 360.98564736629 * (jd - 2451545.0) + longitude)
        val hourAngle = Math.toRadians(normalizeDegrees(sidereal - Math.toDegrees(position.ra)))
        val lat = Math.toRadians(latitude)
        val altitude = asin(sin(lat) * sin(position.dec) + cos(lat) * cos(position.dec) * cos(hourAngle))
        val azimuth = atan2(sin(hourAngle), cos(hourAngle) * sin(lat) - tan(position.dec) * cos(lat)) + PI
        return Math.toDegrees(altitude) to normalizeDegrees(Math.toDegrees(azimuth))
    }

    private fun equatorial(jd: Double, moon: Boolean): Equatorial {
        val d = jd - 2451543.5
        val obliquity = Math.toRadians(23.4393 - 3.563E-7 * d)
        val lon: Double; val lat: Double
        if (moon) {
            val node = Math.toRadians(normalizeDegrees(125.1228 - 0.0529538083 * d))
            val inclination = Math.toRadians(5.1454)
            val periapsis = Math.toRadians(normalizeDegrees(318.0634 + 0.1643573223 * d))
            val meanAnomaly = Math.toRadians(normalizeDegrees(115.3654 + 13.0649929509 * d))
            val eccentricity = 0.0549
            val eccentricAnomaly = meanAnomaly + eccentricity * sin(meanAnomaly) * (1.0 + eccentricity * cos(meanAnomaly))
            val xv = cos(eccentricAnomaly) - eccentricity; val yv = sqrt(1 - eccentricity * eccentricity) * sin(eccentricAnomaly)
            val trueAnomaly = atan2(yv, xv); val radius = sqrt(xv * xv + yv * yv)
            val x = radius * (cos(node) * cos(trueAnomaly + periapsis) - sin(node) * sin(trueAnomaly + periapsis) * cos(inclination))
            val y = radius * (sin(node) * cos(trueAnomaly + periapsis) + cos(node) * sin(trueAnomaly + periapsis) * cos(inclination))
            val z = radius * sin(trueAnomaly + periapsis) * sin(inclination)
            lon = atan2(y, x); lat = atan2(z, sqrt(x * x + y * y))
        } else {
            val periapsis = Math.toRadians(normalizeDegrees(282.9404 + 4.70935E-5 * d))
            val eccentricity = 0.016709 - 1.151E-9 * d
            val meanAnomaly = Math.toRadians(normalizeDegrees(356.0470 + 0.9856002585 * d))
            val eccentricAnomaly = meanAnomaly + eccentricity * sin(meanAnomaly) * (1 + eccentricity * cos(meanAnomaly))
            val xv = cos(eccentricAnomaly) - eccentricity; val yv = sqrt(1 - eccentricity * eccentricity) * sin(eccentricAnomaly)
            lon = atan2(yv, xv) + periapsis; lat = 0.0
        }
        val x = cos(lon) * cos(lat); val y = sin(lon) * cos(lat); val z = sin(lat)
        val equatorialY = y * cos(obliquity) - z * sin(obliquity); val equatorialZ = y * sin(obliquity) + z * cos(obliquity)
        return Equatorial(normalizeRadians(atan2(equatorialY, x)), asin(equatorialZ), normalizeRadians(lon))
    }

    private fun interpolate(start: Instant, end: Instant, startAltitude: Double, endAltitude: Double, target: Double): Instant {
        val fraction = ((target - startAltitude) / (endAltitude - startAltitude)).coerceIn(0.0, 1.0)
        return start.plusMillis((Duration.between(start, end).toMillis() * fraction).toLong())
    }
    private fun julian(time: Instant) = time.epochSecond / 86400.0 + time.nano / 86_400_000_000_000.0 + 2440587.5
    private fun normalizeDegrees(value: Double) = ((value % 360.0) + 360.0) % 360.0
    private fun normalizeRadians(value: Double) = ((value % (2 * PI)) + 2 * PI) % (2 * PI)
}
