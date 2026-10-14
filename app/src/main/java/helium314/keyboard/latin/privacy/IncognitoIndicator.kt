// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.dpToPx
import helium314.keyboard.latin.utils.prefs

/** Tints the keyboard and draws a blue border while nothing is being learned, so incognito is obvious. */
object IncognitoIndicator {
    private const val COLOR = 0xFF2563EB.toInt() // blue, matches the test app and stands out against the built-in themes
    private const val TINT_ALPHA = 0x26 // about 15%, keys stay readable
    private const val BORDER_DP = 3

    @JvmStatic
    fun isActive(view: View): Boolean =
        view.context.prefs().getBoolean(Settings.PREF_ALWAYS_INCOGNITO_MODE, Defaults.PREF_ALWAYS_INCOGNITO_MODE)
            || Settings.getValues().mInputAttributes.mNoLearning

    /** @param keyboardFrame the main keyboard frame (suggestion strip and keys), not the full-screen input view */
    @JvmStatic
    fun apply(keyboardFrame: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return // View.setForeground needs API 23
        keyboardFrame.foreground = if (!isActive(keyboardFrame)) null else GradientDrawable().apply {
            setColor(ColorUtils.setAlphaComponent(COLOR, TINT_ALPHA))
            setStroke(BORDER_DP.dpToPx(keyboardFrame.resources), COLOR)
        }
    }
}
