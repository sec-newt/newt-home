package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {

    private fun shown(typed: String) = (Calculator.answer(typed) as Calculator.Result.Answer).shown
    private fun copied(typed: String) = (Calculator.answer(typed) as Calculator.Result.Answer).copy

    @Test
    fun `everyday math`() {
        assertEquals("4", shown("2+2"))
        assertEquals("14", shown("2 + 3 * 4"))
        assertEquals("20", shown("(2 + 3) * 4"))
        assertEquals("2.5", shown("5 / 2"))
        assertEquals("12", shown("3 x 4"))
        assertEquals("12", shown("3×4"))
        assertEquals("2", shown("8 ÷ 4"))
        assertEquals("1,024", shown("2^10"))
        assertEquals("-3", shown("2 - 5"))
        assertEquals("0.3333333333", shown("1/3"))
        assertEquals("1,234,567", shown("1,234,000 + 567"))
        assertEquals("6", shown("= 2*3"))
    }

    @Test
    fun `percentages the way a calculator does them`() {
        assertEquals("12", shown("15% of 80"))
        assertEquals("92", shown("80 + 15%"))
        assertEquals("64", shown("80 - 20%"))
        assertEquals("5", shown("50 * 10%"))
    }

    @Test
    fun `the copy is the plain number`() {
        assertEquals("1234567", copied("1,234,000 + 567"))
        assertEquals("3.107", copied("5 km to mi"))
    }

    @Test
    fun `app names and plain numbers are left alone`() {
        assertNull(Calculator.answer("2048"))
        assertNull(Calculator.answer("1password"))
        assertNull(Calculator.answer("signal"))
        assertNull(Calculator.answer("-5"))
        assertNull(Calculator.answer("15%"))
        assertNull(Calculator.answer("4k camera"))
        assertNull(Calculator.answer("7zip"))
        assertNull(Calculator.answer("2 +"))
        assertNull(Calculator.answer("5 / 0"))
        assertNull(Calculator.answer(""))
    }

    @Test
    fun `conversions with a target`() {
        assertEquals("3.107 miles", shown("5 km to mi"))
        assertEquals("3.107 miles", shown("5 kilometers in miles"))
        assertEquals("30.48 centimeters", shown("12 in to cm"))
        assertEquals("2.205 pounds", shown("1 kg to lb"))
        assertEquals("236.6 milliliters", shown("1 cup to ml"))
        assertEquals("8 fluid ounces", shown("1 cup to oz"))
        assertEquals("16 cups", shown("1 gallon to cups"))
        assertEquals("3 teaspoons", shown("1 tbsp to tsp"))
        assertEquals("16 cups", shown("gallon to cups"))
        assertEquals("1.5 kilograms", shown("1500 g to kg"))
        assertEquals("100 kilometers per hour", shown("62.14 mph to km/h"))
        assertEquals("90 minutes", shown("1.5 hours to minutes"))
        assertEquals("0.9313 GiB", shown("1 GB to GiB"))
        assertEquals("118.3 milliliters", shown("1/2 cup to ml"))
        assertEquals("2 cups", shown("2 c to cups"))
    }

    @Test
    fun `temperatures`() {
        assertEquals("176.7 °C", shown("350 f to c"))
        assertEquals("212 °F", shown("100°C to F"))
        assertEquals("-40 °C", shown("-40 f to c"))
        assertEquals("0 °C", shown("273.15 kelvin to celsius"))
        assertEquals("98.6 °F", shown("37 degrees c to fahrenheit"))
    }

    @Test
    fun `a number and a unit converts the usual way`() {
        assertEquals("176.7 °C", shown("350 f"))
        assertEquals("350 °F", shown("176.67 c"))
        assertEquals("3.107 miles", shown("5 km"))
        assertEquals("5 kilometers", shown("3.107 mi"))
        assertEquals("236.6 milliliters", shown("1 cup"))
        assertEquals("2.205 pounds", shown("1kg"))
        assertEquals("1 mile", shown("1.6093 km"))
        // Still typing the unit after "to": the usual conversion until it's finished.
        assertEquals("3.107 miles", shown("5 km to"))
    }

    @Test
    fun `kitchen amounts also say the fraction`() {
        assertEquals("0.3333 cups, about 1/3", shown("78.86 ml to cups"))
        assertEquals("1.5 cups, or 1 1/2", shown("12 fl oz to cups"))
        assertEquals("0.25 cups, or 1/4", shown("4 tbsp to cups"))
        assertEquals("1.25 cups, or 1 1/4", shown("1 1/4 cups to cups"))
        assertEquals("16 tablespoons", shown("1 cup to tbsp"))
        assertEquals("0.4227 cups", shown("100 ml to cups"))
    }

    @Test
    fun `units that don't go together say so`() {
        val problem = Calculator.answer("5 kg to miles")
        assertTrue(problem is Calculator.Result.Problem)
        assertEquals("can't turn weight into length", (problem as Calculator.Result.Problem).hint)
    }

    @Test
    fun `review fixes`() {
        // An unknown finished unit says so instead of converting to something else.
        assertTrue(Calculator.answer("5 km to furlongs") is Calculator.Result.Problem)
        // Tiny and huge answers never show as 0 or a wall of digits.
        assertEquals("1.102 × 10^-9 tons", shown("1 mg to ton"))
        assertEquals("1.102E-9", copied("1 mg to ton"))
        assertEquals("1 × 10^300", shown("10^300"))
        assertEquals("0.03333333333", shown("1/30"))
        // Phone numbers and dates aren't sums.
        assertNull(Calculator.answer("555-1234"))
        assertNull(Calculator.answer("555-123-4567"))
        assertNull(Calculator.answer("(555) 123-4567"))
        assertNull(Calculator.answer("2026-11-08"))
        assertNull(Calculator.answer("11/8/2026"))
        assertEquals("5", shown("10-5"))
        // A one-letter unit stuck to a number, or "1st", is more likely an app or a word.
        assertNull(Calculator.answer("5g"))
        assertNull(Calculator.answer("1m"))
        assertNull(Calculator.answer("4h"))
        assertNull(Calculator.answer("1st"))
        assertEquals("0.1764 ounces", shown("5 g"))
        assertEquals("0.1764 ounces", shown("5g to oz"))
        // m is minutes when the other unit is time, like the timer's 1h 30m.
        assertEquals("120 minutes", shown("2 h to m"))
        assertEquals("295.3 feet", shown("90 m"))
        // Two numbers with only a space between them aren't joined into one.
        assertNull(Calculator.answer("2 3 + 1"))
    }
}
