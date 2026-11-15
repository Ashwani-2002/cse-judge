package com.csejudge.app.ui.practice

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Semantic color tokens for CSE Judge supporting Light and Dark modes.
 */
data class AppColors(
    val screenBackground: Color,
    val cardSurface: Color,
    val solutionBackground: Color,
    val solutionBorder: Color,
    val primaryNavy: Color,
    val primaryNavyLight: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderNeutral: Color,
    val optionNeutralBackground: Color,
    val correctBackground: Color,
    val correctBorder: Color,
    val correctText: Color,
    val correctBadgeText: Color,
    val incorrectBackground: Color,
    val incorrectBorder: Color,
    val incorrectText: Color,
    val incorrectBadgeText: Color,
    val skipButtonBackground: Color,
    val skipButtonText: Color,
    val skipButtonBorder: Color,
    val bookmarkYellow: Color,
    val bookmarkYellowLight: Color,
    val bookmarkYellowBorder: Color,
    val isDark: Boolean
)

val LightAppColors = AppColors(
    screenBackground = Color(0xFFFBFBFD),
    cardSurface = Color(0xFFFFFFFF),
    solutionBackground = Color(0xFFF8FAFC),
    solutionBorder = Color(0xFFE2E8F0),
    primaryNavy = Color(0xFF1B365D),
    primaryNavyLight = Color(0xFFEBF1F7),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF64748B),
    borderNeutral = Color(0xFFE2E8F0),
    optionNeutralBackground = Color(0xFFFFFFFF),
    correctBackground = Color(0xFFE8F5E9),
    correctBorder = Color(0xFF2E7D32),
    correctText = Color(0xFF1B5E20),
    correctBadgeText = Color(0xFFFFFFFF),
    incorrectBackground = Color(0xFFFFEBEE),
    incorrectBorder = Color(0xFFD32F2F),
    incorrectText = Color(0xFFB71C1C),
    incorrectBadgeText = Color(0xFFFFFFFF),
    skipButtonBackground = Color(0xFFFFFFFF),
    skipButtonText = Color(0xFF475569),
    skipButtonBorder = Color(0xFFCBD5E1),
    bookmarkYellow = Color(0xFFF59E0B),
    bookmarkYellowLight = Color(0xFFFEF3C7),
    bookmarkYellowBorder = Color(0xFFD97706),
    isDark = false
)

val DarkAppColors = AppColors(
    screenBackground = Color(0xFF0F172A), // Deep charcoal / dark navy
    cardSurface = Color(0xFF1E293B),      // Slightly lighter charcoal
    solutionBackground = Color(0xFF162032),// Subtle dark navy container
    solutionBorder = Color(0xFF334155),    // Slate-700 subtle dark border
    primaryNavy = Color(0xFF38BDF8),       // Accessible sky-400 clear blue accent
    primaryNavyLight = Color(0xFF1E3A5F),  // Dark navy container for chips/chevrons
    onPrimary = Color(0xFF0F172A),         // Deep slate text on luminous accent buttons
    textPrimary = Color(0xFFF8FAFC),       // Off-white slate-50
    textSecondary = Color(0xFF94A3B8),     // Muted slate-400 grey
    borderNeutral = Color(0xFF334155),     // Subtle dark grey
    optionNeutralBackground = Color(0xFF1E293B),
    correctBackground = Color(0xFF064E3B), // Emerald-900 deep container
    correctBorder = Color(0xFF10B981),     // Emerald-500 vivid border
    correctText = Color(0xFF6EE7B7),       // Emerald-300 accessible light green
    correctBadgeText = Color(0xFF064E3B),  // Dark emerald text on vivid border
    incorrectBackground = Color(0xFF450A0A),// Red-950 deep container
    incorrectBorder = Color(0xFFEF4444),   // Red-500 vivid border
    incorrectText = Color(0xFFFCA5A5),     // Red-300 accessible light red
    incorrectBadgeText = Color(0xFFFFFFFF),// White text on red border
    skipButtonBackground = Color(0xFF1E293B),// Charcoal surface
    skipButtonText = Color(0xFFE2E8F0),    // Slate-200
    skipButtonBorder = Color(0xFF475569),  // Slate-600
    bookmarkYellow = Color(0xFFFBBF24),    // Amber-400 luminous gold
    bookmarkYellowLight = Color(0xFF2E230B),// Deep amber container
    bookmarkYellowBorder = Color(0xFFF59E0B),// Amber-500 border
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/**
 * Design colors and styling tokens for the CSE Judge interface.
 * Formulated for a minimal, distraction-free, professional UPSC exam preparation experience.
 * Accessors dynamically resolve tokens from [LocalAppColors].
 */
object PracticeColors {
    // Backgrounds
    val ScreenBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.screenBackground

    val CardSurface: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.cardSurface

    val SolutionBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.solutionBackground

    val SolutionBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.solutionBorder

    // Primary Accents
    val PrimaryNavy: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.primaryNavy

    val PrimaryNavyLight: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.primaryNavyLight

    val OnPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.onPrimary

    // Neutral Text & Borders
    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.textPrimary

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.textSecondary

    val BorderNeutral: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.borderNeutral

    val OptionNeutralBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.optionNeutralBackground

    // Correct Answer Highlight (Green)
    val CorrectBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.correctBackground

    val CorrectBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.correctBorder

    val CorrectText: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.correctText

    val CorrectBadgeText: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.correctBadgeText

    // Incorrect Answer Highlight (Red)
    val IncorrectBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.incorrectBackground

    val IncorrectBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.incorrectBorder

    val IncorrectText: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.incorrectText

    val IncorrectBadgeText: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.incorrectBadgeText

    // Action Buttons
    val SkipButtonBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.skipButtonBackground

    val SkipButtonText: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.skipButtonText

    val SkipButtonBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.skipButtonBorder

    // Bookmark Star Colors
    val BookmarkYellow: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.bookmarkYellow

    val BookmarkYellowLight: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.bookmarkYellowLight

    val BookmarkYellowBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.bookmarkYellowBorder

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current.isDark
}

/**
 * Global application theme for CSE Judge.
 * Centrally wraps [LocalAppColors] and Material 3 [MaterialTheme].
 * Automatically configures status and navigation bar icons for edge-to-edge display.
 */
@Composable
fun CSEJudgeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            background = appColors.screenBackground,
            onBackground = appColors.textPrimary,
            surface = appColors.cardSurface,
            onSurface = appColors.textPrimary,
            surfaceContainer = appColors.cardSurface,
            surfaceContainerHigh = Color(0xFF283548),
            surfaceContainerHighest = Color(0xFF334155),
            surfaceContainerLow = Color(0xFF162032),
            surfaceContainerLowest = Color(0xFF0F172A),
            surfaceVariant = Color(0xFF283548),
            onSurfaceVariant = appColors.textSecondary,
            primary = appColors.primaryNavy,
            onPrimary = appColors.onPrimary,
            outline = appColors.borderNeutral,
            outlineVariant = appColors.borderNeutral
        )
    } else {
        lightColorScheme(
            background = appColors.screenBackground,
            onBackground = appColors.textPrimary,
            surface = appColors.cardSurface,
            onSurface = appColors.textPrimary,
            surfaceContainer = appColors.cardSurface,
            surfaceContainerHigh = Color(0xFFF1F5F9),
            surfaceContainerHighest = Color(0xFFE2E8F0),
            surfaceContainerLow = Color(0xFFF8FAFC),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = appColors.textSecondary,
            primary = appColors.primaryNavy,
            onPrimary = appColors.onPrimary,
            outline = appColors.borderNeutral,
            outlineVariant = appColors.borderNeutral
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
