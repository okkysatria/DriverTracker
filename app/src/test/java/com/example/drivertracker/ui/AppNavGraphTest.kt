package com.example.drivertracker.ui

import com.example.drivertracker.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppNavGraphTest {

    @Test
    fun testScreenRoutesAndTitles() {
        assertEquals("Perekam", Screen.RekamOrder.title)
        assertEquals("Radar", Screen.SmartRadar.title)
        assertEquals("Rute", Screen.TrackPoster.title)
        assertEquals("Laporan", Screen.Dashboard.title)
        assertEquals("Pengaturan", Screen.Settings.title)

        assertEquals("rekam_order", Screen.RekamOrder.route)
        assertEquals("smart_radar", Screen.SmartRadar.route)
        assertEquals("track_poster", Screen.TrackPoster.route)
        assertEquals("dashboard", Screen.Dashboard.route)
        assertEquals("settings", Screen.Settings.route)

        assertNotNull(Screen.RekamOrder.icon)
        assertNotNull(Screen.SmartRadar.icon)
        assertNotNull(Screen.TrackPoster.icon)
        assertNotNull(Screen.Dashboard.icon)
        assertNotNull(Screen.Settings.icon)
    }

    @Test
    fun testBottomNavItemsOrder() {
        val items = Screen.bottomNavItems
        assertEquals(5, items.size)
        assertEquals(Screen.RekamOrder, items[0])
        assertEquals(Screen.SmartRadar, items[1])
        assertEquals(Screen.TrackPoster, items[2])
        assertEquals(Screen.Dashboard, items[3])
        assertEquals(Screen.Settings, items[4])
    }

    @Test
    fun testBackStackNavigationLogic() {
        val backStack = mutableListOf<Screen>(Screen.RekamOrder)

        val screen1 = Screen.SmartRadar
        backStack.remove(screen1)
        backStack.add(screen1)
        assertEquals(2, backStack.size)
        assertEquals(Screen.SmartRadar, backStack.last())

        val screen2 = Screen.Dashboard
        backStack.remove(screen2)
        backStack.add(screen2)
        assertEquals(3, backStack.size)
        assertEquals(Screen.Dashboard, backStack.last())

        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        assertEquals(2, backStack.size)
        assertEquals(Screen.SmartRadar, backStack.last())

        val screenStart = Screen.RekamOrder
        backStack.clear()
        backStack.add(screenStart)
        assertEquals(1, backStack.size)
        assertEquals(Screen.RekamOrder, backStack.last())
    }
}
