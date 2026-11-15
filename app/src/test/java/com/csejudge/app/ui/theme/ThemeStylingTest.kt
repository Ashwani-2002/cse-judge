package com.csejudge.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.csejudge.app.ui.practice.DarkAppColors
import com.csejudge.app.ui.practice.LightAppColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeStylingTest {

    @Test
    fun darkAppColorsConformsToDesignGuidelines() {
        // Main background is deep charcoal / dark navy, NOT pure black
        assertNotEquals(Color.Black, DarkAppColors.screenBackground)
        assertEquals(Color(0xFF0F172A), DarkAppColors.screenBackground)

        // Surface and cards are slightly lighter charcoal
        assertEquals(Color(0xFF1E293B), DarkAppColors.cardSurface)
        assertNotEquals(DarkAppColors.screenBackground, DarkAppColors.cardSurface)

        // Primary accent is accessible clear blue
        assertEquals(Color(0xFF38BDF8), DarkAppColors.primaryNavy)

        // Off-white primary text and muted grey secondary text
        assertEquals(Color(0xFFF8FAFC), DarkAppColors.textPrimary)
        assertEquals(Color(0xFF94A3B8), DarkAppColors.textSecondary)

        // Subtle dark grey borders
        assertEquals(Color(0xFF334155), DarkAppColors.borderNeutral)

        assertTrue(DarkAppColors.isDark)
    }

    @Test
    fun lightAppColorsConformsToLightDesignPalette() {
        assertEquals(Color(0xFFFBFBFD), LightAppColors.screenBackground)
        assertEquals(Color(0xFFFFFFFF), LightAppColors.cardSurface)
        assertEquals(Color(0xFF1B365D), LightAppColors.primaryNavy)
        assertEquals(Color(0xFF1E293B), LightAppColors.textPrimary)
        assertEquals(Color(0xFF64748B), LightAppColors.textSecondary)
        assertEquals(Color(0xFFE2E8F0), LightAppColors.borderNeutral)
        assertFalse(LightAppColors.isDark)
    }

    @Test
    fun correctAndIncorrectAnswersAreDistinguishableInDarkMode() {
        // Emerald green container and vivid border vs deep red container and vivid border
        assertNotEquals(DarkAppColors.correctBackground, DarkAppColors.incorrectBackground)
        assertNotEquals(DarkAppColors.correctBorder, DarkAppColors.incorrectBorder)
        assertNotEquals(DarkAppColors.correctText, DarkAppColors.incorrectText)

        // Distinguishable from neutral card surface
        assertNotEquals(DarkAppColors.cardSurface, DarkAppColors.correctBackground)
        assertNotEquals(DarkAppColors.cardSurface, DarkAppColors.incorrectBackground)

        // Correct styling
        assertEquals(Color(0xFF064E3B), DarkAppColors.correctBackground)
        assertEquals(Color(0xFF10B981), DarkAppColors.correctBorder)
        assertEquals(Color(0xFF6EE7B7), DarkAppColors.correctText)
        assertEquals(Color(0xFF064E3B), DarkAppColors.correctBadgeText)

        // Incorrect styling
        assertEquals(Color(0xFF450A0A), DarkAppColors.incorrectBackground)
        assertEquals(Color(0xFFEF4444), DarkAppColors.incorrectBorder)
        assertEquals(Color(0xFFFCA5A5), DarkAppColors.incorrectText)
        assertEquals(Color(0xFFFFFFFF), DarkAppColors.incorrectBadgeText)
    }

    @Test
    fun correctAndIncorrectAnswersAreDistinguishableInLightMode() {
        assertNotEquals(LightAppColors.correctBackground, LightAppColors.incorrectBackground)
        assertNotEquals(LightAppColors.correctBorder, LightAppColors.incorrectBorder)
        assertNotEquals(LightAppColors.correctText, LightAppColors.incorrectText)

        assertEquals(Color(0xFFE8F5E9), LightAppColors.correctBackground)
        assertEquals(Color(0xFF2E7D32), LightAppColors.correctBorder)
        assertEquals(Color(0xFF1B5E20), LightAppColors.correctText)
        assertEquals(Color(0xFFFFFFFF), LightAppColors.correctBadgeText)

        assertEquals(Color(0xFFFFEBEE), LightAppColors.incorrectBackground)
        assertEquals(Color(0xFFD32F2F), LightAppColors.incorrectBorder)
        assertEquals(Color(0xFFB71C1C), LightAppColors.incorrectText)
        assertEquals(Color(0xFFFFFFFF), LightAppColors.incorrectBadgeText)
    }

    @Test
    fun bookmarkStarStylingRemainsVisibleInBothModes() {
        // Light mode
        assertEquals(Color(0xFFF59E0B), LightAppColors.bookmarkYellow)
        assertEquals(Color(0xFFFEF3C7), LightAppColors.bookmarkYellowLight)
        assertEquals(Color(0xFFD97706), LightAppColors.bookmarkYellowBorder)

        // Dark mode: luminous amber-400 gold and deep amber container
        assertEquals(Color(0xFFFBBF24), DarkAppColors.bookmarkYellow)
        assertEquals(Color(0xFF2E230B), DarkAppColors.bookmarkYellowLight)
        assertEquals(Color(0xFFF59E0B), DarkAppColors.bookmarkYellowBorder)

        // Clearly distinguishable from card surface
        assertNotEquals(DarkAppColors.cardSurface, DarkAppColors.bookmarkYellow)
        assertNotEquals(DarkAppColors.cardSurface, DarkAppColors.bookmarkYellowLight)
    }
}
