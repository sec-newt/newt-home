package io.github.secnewt.home.apps

import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * The prompt's calculator: math (`15% of 80`, `2 + 3 * 4`) and unit conversions
 * (`5 km to mi`, `350 f`, `1 cup to ml`). Everything is worked out on the phone. The answer
 * shows while you type; it never opens anything, so it's safe to show it right away.
 *
 * It only answers when what's typed is clearly math or a conversion: a plain number
 * (`2048`) or an app name that starts with a number (`1Password`, `7-Zip`) gets nothing.
 */
object Calculator {

    sealed interface Result {
        /** [shown] is what to show ("3.107 miles"); [copy] is the bare number to copy ("3.107"). */
        data class Answer(val shown: String, val copy: String) : Result

        /** Looks like a conversion, but can't be done, like kilograms to miles. */
        data class Problem(val hint: String) : Result
    }

    fun answer(typed: String): Result? {
        val text = clean(typed)
        if (text.isEmpty() || text.none { it.isDigit() } && !startsWithUnit(text)) return null
        return conversion(text) ?: math(text)
    }

    // ---- Math ----

    private fun math(text: String): Result? {
        val expr = text.removePrefix("=").trim()
        val parsed = Parser(expr).parseAll() ?: return null
        if (!parsed.hasOperator) return null
        val value = parsed.value
        if (value.isNaN() || value.isInfinite()) return null
        return Result.Answer(number(value, MATH_DIGITS, grouped = true), number(value, MATH_DIGITS, grouped = false))
    }

    /** Lowercase, single spaces, `×`/`÷`/`x` as `*` and `/`, and `1,234` as `1234`. */
    private fun clean(typed: String): String = typed.trim().lowercase(Locale.ROOT)
        .replace(Regex("""\s+"""), " ")
        .replace('×', '*').replace('÷', '/').replace('−', '-')
        .replace(Regex("""(?<=\d)\s*x\s*(?=[\d(])"""), "*")
        .replace(Regex("""(?<=\d),(?=\d{3}(\D|$))"""), "")
        .replace("°", " ")
        .replace(Regex("""\bdegrees?\b"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private class Parsed(val value: Double, val hasOperator: Boolean)

    /**
     * Numbers, + - * / ^, parentheses, `%`, and `of` after a percent. `80 + 15%` adds 15% of
     * 80, like a pocket calculator. `1 1/2` is one and a half.
     */
    private class Parser(input: String) {
        private val s = input
            .replace(Regex("""(\d+) (\d+)/(\d+)"""), "($1+$2/$3)")
            .replace(Regex("""\s*\bof\b\s*"""), "*")
            .replace(" ", "")
        private var at = 0
        private var operator = false

        fun parseAll(): Parsed? = runCatching {
            if (s.isEmpty()) return null
            val v = expr().value
            if (at != s.length) return null
            Parsed(v, operator)
        }.getOrNull()

        private class Term(val value: Double, val percent: Boolean)

        private fun expr(): Term {
            var left = term()
            while (at < s.length && (s[at] == '+' || s[at] == '-')) {
                val plus = s[at++] == '+'
                operator = true
                val right = term()
                val v = if (right.percent) {
                    left.value * (1 + if (plus) right.value else -right.value)
                } else {
                    left.value + if (plus) right.value else -right.value
                }
                left = Term(v, false)
            }
            return left
        }

        private fun term(): Term {
            var left = power()
            while (at < s.length && (s[at] == '*' || s[at] == '/')) {
                val times = s[at++] == '*'
                operator = true
                val right = power()
                if (!times && right.value == 0.0) error("divide by zero")
                left = Term(if (times) left.value * right.value else left.value / right.value, false)
            }
            return left
        }

        private fun power(): Term {
            val base = unary()
            if (at < s.length && s[at] == '^') {
                at++
                operator = true
                return Term(base.value.pow(power().value), false)
            }
            return base
        }

        private fun unary(): Term {
            if (at < s.length && s[at] == '-') {
                at++
                val t = unary()
                return Term(-t.value, t.percent)
            }
            if (at < s.length && s[at] == '+') {
                at++
                return unary()
            }
            val t = primary()
            if (at < s.length && s[at] == '%') {
                at++
                return Term(t.value / 100, true)
            }
            return t
        }

        private fun primary(): Term {
            if (at < s.length && s[at] == '(') {
                at++
                val v = expr()
                if (at >= s.length || s[at] != ')') error("missing )")
                at++
                return Term(v.value, false)
            }
            val start = at
            while (at < s.length && (s[at].isDigit() || s[at] == '.')) at++
            return Term(s.substring(start, at).toDouble(), false)
        }
    }

    // ---- Conversions ----

    private enum class Kind(val word: String) {
        LENGTH("length"), WEIGHT("weight"), VOLUME("volume"), TEMPERATURE("temperature"),
        SPEED("speed"), TIME("time"), DATA("data"), AREA("area"),
    }

    /**
     * A unit: how many base units one of it is ([factor]), its name for one and for more,
     * the words people type for it, and what it turns into when no unit is asked for.
     */
    private class Unit(
        val kind: Kind,
        val factor: Double,
        val one: String,
        val many: String,
        val words: List<String>,
        val usual: String,
        /** Cooking measures also say the nearest fraction: "0.3333 cups, about 1/3". */
        val kitchen: Boolean = false,
    )

    private val UNITS: List<Unit> = listOf(
        // Temperature first, so a bare "c" means Celsius ("2 c to cups" still finds cups).
        // Worked out separately; factor unused.
        Unit(Kind.TEMPERATURE, 0.0, "°F", "°F", listOf("f", "fahrenheit"), "°C"),
        Unit(Kind.TEMPERATURE, 0.0, "°C", "°C", listOf("c", "celsius", "centigrade"), "°F"),
        Unit(Kind.TEMPERATURE, 0.0, "kelvin", "kelvin", listOf("kelvin"), "°C"),
        // Length, in meters.
        Unit(Kind.LENGTH, 0.001, "millimeter", "millimeters", listOf("mm", "millimeter", "millimeters", "millimetre", "millimetres"), "inch"),
        Unit(Kind.LENGTH, 0.01, "centimeter", "centimeters", listOf("cm", "centimeter", "centimeters", "centimetre", "centimetres"), "inch"),
        Unit(Kind.LENGTH, 1.0, "meter", "meters", listOf("m", "meter", "meters", "metre", "metres"), "foot"),
        Unit(Kind.LENGTH, 1000.0, "kilometer", "kilometers", listOf("km", "kilometer", "kilometers", "kilometre", "kilometres", "kms"), "mile"),
        Unit(Kind.LENGTH, 0.0254, "inch", "inches", listOf("in", "inch", "inches", "\""), "centimeter"),
        Unit(Kind.LENGTH, 0.3048, "foot", "feet", listOf("ft", "foot", "feet", "'"), "meter"),
        Unit(Kind.LENGTH, 0.9144, "yard", "yards", listOf("yd", "yds", "yard", "yards"), "meter"),
        Unit(Kind.LENGTH, 1609.344, "mile", "miles", listOf("mi", "mile", "miles"), "kilometer"),
        // Weight, in grams.
        Unit(Kind.WEIGHT, 0.001, "milligram", "milligrams", listOf("mg", "milligram", "milligrams"), "gram"),
        Unit(Kind.WEIGHT, 1.0, "gram", "grams", listOf("g", "gram", "grams"), "ounce"),
        Unit(Kind.WEIGHT, 1000.0, "kilogram", "kilograms", listOf("kg", "kgs", "kilo", "kilos", "kilogram", "kilograms"), "pound"),
        Unit(Kind.WEIGHT, 28.349523125, "ounce", "ounces", listOf("oz", "ounce", "ounces"), "gram"),
        Unit(Kind.WEIGHT, 453.59237, "pound", "pounds", listOf("lb", "lbs", "pound", "pounds"), "kilogram"),
        Unit(Kind.WEIGHT, 6350.29318, "stone", "stone", listOf("st", "stone", "stones"), "pound"),
        Unit(Kind.WEIGHT, 907184.74, "ton", "tons", listOf("ton", "tons"), "metric ton"),
        Unit(Kind.WEIGHT, 1_000_000.0, "metric ton", "metric tons", listOf("tonne", "tonnes", "metric ton", "metric tons"), "ton"),
        // Volume, in milliliters.
        Unit(Kind.VOLUME, 1.0, "milliliter", "milliliters", listOf("ml", "milliliter", "milliliters", "millilitre", "millilitres"), "fluid ounce"),
        Unit(Kind.VOLUME, 1000.0, "liter", "liters", listOf("l", "liter", "liters", "litre", "litres", "ltr"), "quart"),
        Unit(Kind.VOLUME, 4.92892159375, "teaspoon", "teaspoons", listOf("tsp", "tsps", "teaspoon", "teaspoons"), "milliliter", kitchen = true),
        Unit(Kind.VOLUME, 14.78676478125, "tablespoon", "tablespoons", listOf("tbsp", "tbsps", "tbs", "tablespoon", "tablespoons"), "milliliter", kitchen = true),
        Unit(Kind.VOLUME, 29.5735295625, "fluid ounce", "fluid ounces", listOf("fl oz", "floz", "fluid ounce", "fluid ounces", "oz"), "milliliter"),
        Unit(Kind.VOLUME, 236.5882365, "cup", "cups", listOf("cup", "cups", "c"), "milliliter", kitchen = true),
        Unit(Kind.VOLUME, 473.176473, "pint", "pints", listOf("pt", "pint", "pints"), "milliliter"),
        Unit(Kind.VOLUME, 946.352946, "quart", "quarts", listOf("qt", "qts", "quart", "quarts"), "liter"),
        Unit(Kind.VOLUME, 3785.411784, "gallon", "gallons", listOf("gal", "gals", "gallon", "gallons"), "liter"),
        // Speed, in meters per second.
        Unit(Kind.SPEED, 0.44704, "mile per hour", "miles per hour", listOf("mph", "miles per hour", "mile per hour"), "kilometer per hour"),
        Unit(Kind.SPEED, 1 / 3.6, "kilometer per hour", "kilometers per hour", listOf("km/h", "kmh", "kph", "kilometers per hour", "kilometer per hour"), "mile per hour"),
        Unit(Kind.SPEED, 1.0, "meter per second", "meters per second", listOf("m/s", "meters per second", "meter per second"), "mile per hour"),
        Unit(Kind.SPEED, 1852.0 / 3600, "knot", "knots", listOf("knot", "knots", "kn", "kt", "kts"), "mile per hour"),
        // Time, in seconds.
        Unit(Kind.TIME, 1.0, "second", "seconds", listOf("s", "sec", "secs", "second", "seconds"), "minute"),
        Unit(Kind.TIME, 60.0, "minute", "minutes", listOf("min", "mins", "minute", "minutes"), "hour"),
        Unit(Kind.TIME, 3600.0, "hour", "hours", listOf("h", "hr", "hrs", "hour", "hours"), "minute"),
        Unit(Kind.TIME, 86400.0, "day", "days", listOf("day", "days"), "hour"),
        Unit(Kind.TIME, 604800.0, "week", "weeks", listOf("wk", "wks", "week", "weeks"), "day"),
        Unit(Kind.TIME, 31_557_600.0, "year", "years", listOf("yr", "yrs", "year", "years"), "day"),
        // Data, in bytes. KB, MB… are powers of 1000; KiB, MiB… powers of 1024.
        Unit(Kind.DATA, 1.0, "byte", "bytes", listOf("byte", "bytes"), "KB"),
        Unit(Kind.DATA, 1e3, "KB", "KB", listOf("kb", "kilobyte", "kilobytes"), "KiB"),
        Unit(Kind.DATA, 1e6, "MB", "MB", listOf("mb", "megabyte", "megabytes"), "MiB"),
        Unit(Kind.DATA, 1e9, "GB", "GB", listOf("gb", "gigabyte", "gigabytes"), "GiB"),
        Unit(Kind.DATA, 1e12, "TB", "TB", listOf("tb", "terabyte", "terabytes"), "TiB"),
        Unit(Kind.DATA, 1024.0, "KiB", "KiB", listOf("kib", "kibibyte", "kibibytes"), "KB"),
        Unit(Kind.DATA, 1024.0.pow(2), "MiB", "MiB", listOf("mib", "mebibyte", "mebibytes"), "MB"),
        Unit(Kind.DATA, 1024.0.pow(3), "GiB", "GiB", listOf("gib", "gibibyte", "gibibytes"), "GB"),
        Unit(Kind.DATA, 1024.0.pow(4), "TiB", "TiB", listOf("tib", "tebibyte", "tebibytes"), "TB"),
        // Area, in square meters.
        Unit(Kind.AREA, 0.09290304, "square foot", "square feet", listOf("sq ft", "sqft", "ft2", "ft²", "square foot", "square feet"), "square meter"),
        Unit(Kind.AREA, 1.0, "square meter", "square meters", listOf("sq m", "sqm", "m2", "m²", "square meter", "square meters", "square metre", "square metres"), "square foot"),
        Unit(Kind.AREA, 2_589_988.110336, "square mile", "square miles", listOf("sq mi", "sqmi", "mi2", "mi²", "square mile", "square miles"), "square kilometer"),
        Unit(Kind.AREA, 1e6, "square kilometer", "square kilometers", listOf("sq km", "sqkm", "km2", "km²", "square kilometer", "square kilometers"), "square mile"),
        Unit(Kind.AREA, 4046.8564224, "acre", "acres", listOf("acre", "acres", "ac"), "hectare"),
        Unit(Kind.AREA, 10_000.0, "hectare", "hectares", listOf("hectare", "hectares", "ha"), "acre"),
    )

    /** Every word for a unit, longest first, so "fl oz" wins over "oz" and "sq ft" over "ft". */
    private val WORDS: List<Pair<String, List<Unit>>> = UNITS
        .flatMap { u -> u.words.map { it to u } }
        .groupBy({ it.first }, { it.second })
        .toList()
        .sortedByDescending { it.first.length }

    private fun unitsFor(word: String): List<Unit> = WORDS.firstOrNull { it.first == word }?.second.orEmpty()

    private fun startsWithUnit(text: String) = WORDS.any { (w, _) -> text.startsWith("$w ") }

    /** "5 km to mi", "5 km in miles", "gallon to cups", or just "350 f". */
    private fun conversion(text: String): Result? {
        for (splitter in listOf(" to ", " into ", " in ", " as ", " = ")) {
            var from = text.indexOf(splitter)
            while (from >= 0) {
                val target = unitsFor(text.substring(from + splitter.length).trim())
                if (target.isNotEmpty()) {
                    amount(text.substring(0, from), allowNoNumber = true)?.let { (qty, units) -> return convert(qty, units, target) }
                }
                from = text.indexOf(splitter, from + 1)
            }
        }
        // Still typing the unit after "to": convert the usual way until it's finished.
        val trailing = Regex("""^(.*) (to|into|in|as)( \S*)?$""").find(text)?.groupValues?.get(1)
        if (trailing != null) amount(trailing, allowNoNumber = false)?.let { (qty, units) -> return convert(qty, units, null) }
        return amount(text, allowNoNumber = false)?.let { (qty, units) -> convert(qty, units, null) }
    }

    /** "5 km" → 5 and kilometers. The number may be math ("1/2 cup") or, with a target, left out ("cup to ml"). */
    private fun amount(text: String, allowNoNumber: Boolean): Pair<Double, List<Unit>>? {
        val t = text.trim()
        for ((word, units) in WORDS) {
            if (!t.endsWith(word)) continue
            val before = t.dropLast(word.length)
            if (before.isNotEmpty() && !before.last().let { it.isDigit() || it == ' ' || it == ')' || it == '.' }) continue
            val numberPart = before.trim().removeSuffix("a").trim()
            if (numberPart.isEmpty()) return if (allowNoNumber) 1.0 to units else null
            val parsed = Parser(numberPart).parseAll() ?: continue
            return parsed.value to units
        }
        return null
    }

    private fun convert(qty: Double, fromUnits: List<Unit>, toUnits: List<Unit>?): Result {
        val (from, to) = if (toUnits == null) {
            val f = fromUnits.first()
            f to UNITS.first { it.kind == f.kind && it.one == f.usual }
        } else {
            fromUnits.firstNotNullOfOrNull { f -> toUnits.firstOrNull { it.kind == f.kind }?.let { f to it } }
                ?: return Result.Problem("can't turn ${fromUnits.first().kind.word} into ${toUnits.first().kind.word}")
        }
        val value = if (from.kind == Kind.TEMPERATURE) temperature(qty, from, to) else qty * from.factor / to.factor
        val shown = number(value, UNIT_DIGITS, grouped = true)
        val rounded = BigDecimal(value).round(MathContext(UNIT_DIGITS)).toDouble()
        val name = if (rounded == 1.0) to.one else to.many
        return Result.Answer("$shown $name${if (to.kitchen) fraction(value) else ""}", number(value, UNIT_DIGITS, grouped = false))
    }

    private fun temperature(qty: Double, from: Unit, to: Unit): Double {
        val celsius = when (from.one) {
            "°F" -> (qty - 32) / 1.8
            "kelvin" -> qty - 273.15
            else -> qty
        }
        return when (to.one) {
            "°F" -> celsius * 1.8 + 32
            "kelvin" -> celsius + 273.15
            else -> celsius
        }
    }

    private val FRACTIONS = listOf(1.0 / 8 to "1/8", 1.0 / 4 to "1/4", 1.0 / 3 to "1/3", 1.0 / 2 to "1/2", 2.0 / 3 to "2/3", 3.0 / 4 to "3/4")

    /** ", or 1 1/2" when it's exact, ", about 1/3" when it's close, or nothing. */
    private fun fraction(value: Double): String {
        val whole = floor(value)
        val part = value - whole
        if (value <= 0 || part < 0.0005 || part > 0.9995) return ""
        val (f, words) = FRACTIONS.minBy { abs(it.first - part) }
        val off = abs(f - part)
        if (off > 0.02) return ""
        val text = if (whole > 0) "${whole.toLong()} $words" else words
        return if (off < 1e-6) ", or $text" else ", about $text"
    }

    private const val MATH_DIGITS = 10
    private const val UNIT_DIGITS = 4

    /** [digits] significant digits, no trailing zeros; "1,234.5" when [grouped]. */
    private fun number(value: Double, digits: Int, grouped: Boolean): String {
        if (value == 0.0) return "0"
        val rounded = BigDecimal(value).round(MathContext(digits)).stripTrailingZeros()
        if (rounded.signum() == 0) return "0"
        if (!grouped) return rounded.toPlainString()
        val format = DecimalFormat("#,##0.##########", DecimalFormatSymbols(Locale.US))
        return format.format(rounded)
    }
}
