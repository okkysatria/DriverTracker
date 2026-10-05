package com.example.drivertracker.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testLongToRupiahStringWithSymbol() {
        assertEquals("Rp 10.000", 10000L.toRupiahString(includeSymbol = true))
        assertEquals("Rp 1.250.000", 1250000L.toRupiahString(includeSymbol = true))
        assertEquals("Rp 0", 0L.toRupiahString(includeSymbol = true))
        assertEquals("-Rp 10.000", (-10000L).toRupiahString(includeSymbol = true))
    }

    @Test
    fun testLongToRupiahStringWithoutSymbol() {
        assertEquals("10.000", 10000L.toRupiahString(includeSymbol = false))
        assertEquals("1.250.000", 1250000L.toRupiahString(includeSymbol = false))
        assertEquals("0", 0L.toRupiahString(includeSymbol = false))
        assertEquals("-10.000", (-10000L).toRupiahString(includeSymbol = false))
    }

    @Test
    fun testDoubleToRupiahString() {
        assertEquals("Rp 10.000", 10000.0.toRupiahString(includeSymbol = true))
        assertEquals("10.000", 10000.0.toRupiahString(includeSymbol = false))
        assertEquals("Rp 1.250.000", 1250000.0.toRupiahString(includeSymbol = true))
        assertEquals("1.250.000", 1250000.0.toRupiahString(includeSymbol = false))
        assertEquals("Rp 0", 0.0.toRupiahString(includeSymbol = true))
        assertEquals("0", 0.0.toRupiahString(includeSymbol = false))
    }

    @Test
    fun testParseRupiahToDouble() {
        assertEquals(10000.0, "10.000".parseRupiahToDouble(), 0.001)
        assertEquals(10000.0, "Rp 10.000".parseRupiahToDouble(), 0.001)
        assertEquals(10000.0, "10000".parseRupiahToDouble(), 0.001)
        assertEquals(1250000.0, "Rp. 1.250.000".parseRupiahToDouble(), 0.001)
        assertEquals(0.0, "".parseRupiahToDouble(), 0.001)
        assertEquals(0.0, "abc".parseRupiahToDouble(), 0.001)
        assertEquals(10000.5, "10.000,50".parseRupiahToDouble(), 0.001)
        assertEquals(-10000.0, "-10.000".parseRupiahToDouble(), 0.001)
    }
}
