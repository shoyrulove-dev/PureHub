package com.purehub.app.feature.vault

import java.security.SecureRandom

data class VaultHealth(
    val total: Int,
    val weak: Int,
    val reused: Int,
    val score: Int,
)

object VaultSecurity {
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#%&*+-_="
    private const val ALPHABET = UPPER + LOWER + DIGITS + SYMBOLS

    fun generate(length: Int = 20, random: SecureRandom = SecureRandom()): String {
        val safeLength = length.coerceIn(12, 64)
        val values = MutableList(safeLength) { ALPHABET[random.nextInt(ALPHABET.length)] }
        values[0] = UPPER[random.nextInt(UPPER.length)]
        values[1] = LOWER[random.nextInt(LOWER.length)]
        values[2] = DIGITS[random.nextInt(DIGITS.length)]
        values[3] = SYMBOLS[random.nextInt(SYMBOLS.length)]
        for (index in values.lastIndex downTo 1) {
            val other = random.nextInt(index + 1)
            val current = values[index]
            values[index] = values[other]
            values[other] = current
        }
        return values.joinToString("")
    }

    fun isWeak(password: String): Boolean {
        if (password.length < 12) return true
        val classes = listOf(
            password.any(Char::isLowerCase),
            password.any(Char::isUpperCase),
            password.any(Char::isDigit),
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        return classes < 3
    }

    fun health(entries: List<VaultEntry>): VaultHealth {
        val reusedPasswords = entries.groupingBy { it.password }.eachCount().filterValues { it > 1 }.keys
        val weak = entries.count { isWeak(it.password) }
        val reused = entries.count { it.password in reusedPasswords }
        val deductions = weak * 18 + reused * 14
        return VaultHealth(entries.size, weak, reused, (100 - deductions).coerceIn(0, 100))
    }
}
