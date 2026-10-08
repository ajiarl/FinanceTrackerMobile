package com.sena.financetracker.ui.navigation

import androidx.compose.ui.graphics.Color
import com.sena.financetracker.ui.components.RetroBorder
import com.sena.financetracker.ui.components.RetroCyberMint
import com.sena.financetracker.ui.components.RetroYellow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk validasi rute navigasi tab bawah, optimasi O(1) bottomNavRoutes,
 * serta konsistensi palet warna Neobrutalisme pada Splash Screen dan sistem navigasi.
 */
class NavigationAndSplashTest {

    @Test
    fun testBottomNavRoutesContainsAllPrimaryTabs() {
        val expectedRoutes = setOf(
            Screen.Dashboard.route,
            Screen.Transactions.route,
            Screen.Budgets.route,
            Screen.Accounts.route,
            Screen.Reports.route
        )

        assertEquals("bottomNavRoutes harus berisi tepat 5 tab navigasi utama", 5, Screen.bottomNavRoutes.size)
        assertEquals(expectedRoutes, Screen.bottomNavRoutes)
    }

    @Test
    fun testBottomNavRoutesExcludesSubScreens() {
        // Sub-screen tidak boleh berada di bottomNavRoutes
        assertFalse("Kategori bukan merupakan bottom nav item", Screen.bottomNavRoutes.contains(Screen.Categories.route))
        assertFalse("Notifikasi bukan merupakan bottom nav item", Screen.bottomNavRoutes.contains(Screen.Notifications.route))
        assertFalse("Pengaturan bukan merupakan bottom nav item", Screen.bottomNavRoutes.contains(Screen.Settings.route))
        assertFalse("Impor CSV bukan merupakan bottom nav item", Screen.bottomNavRoutes.contains(Screen.Import.route))
    }

    @Test
    fun testBottomNavItemsOrderAndCount() {
        val items = Screen.bottomNavItems
        assertEquals(5, items.size)
        assertEquals(Screen.Dashboard, items[0])
        assertEquals(Screen.Transactions, items[1])
        assertEquals(Screen.Budgets, items[2])
        assertEquals(Screen.Accounts, items[3])
        assertEquals(Screen.Reports, items[4])
    }

    @Test
    fun testNeobrutalSplashScreenPaletteConstants() {
        // Verifikasi ketepatan hex warna Neobrutalism spec
        assertEquals("RetroYellow harus #FAFF00", Color(0xFFFAFF00), RetroYellow)
        assertEquals("RetroCyberMint harus #00F0FF", Color(0xFF00F0FF), RetroCyberMint)
        assertEquals("RetroBorder harus #000000", Color(0xFF000000), RetroBorder)
    }

    @Test
    fun testBottomNavRouteFastLookupIntegrity() {
        // Memastikan efisiensi O(1) set lookup bekerja dengan benar pada semua tab
        Screen.bottomNavItems.forEach { screen ->
            assertTrue("Rute ${screen.route} wajib ada di bottomNavRoutes", Screen.bottomNavRoutes.contains(screen.route))
        }
    }
}
