// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.dpToPx
import helium314.keyboard.latin.utils.prefs

/** Tints the whole keyboard and draws a colored border while nothing is being learned, so incognito is obvious. */
object IncognitoIndicator {
    private const val COLOR = 0xFF7C3AED.toInt() // violet, stands out against every built-in theme
    private const val TINT_ALPHA = 0x26 // about 15%, keys stay readable
    private const val BORDER_DP = 3

    @JvmStatic
    fun isActive(view: FrameLayout): Boolean =
        view.context.prefs().getBoolean(Settings.PREF_ALWAYS_INCOGNITO_MODE, Defaults.PREF_ALWAYS_INCOGNITO_MODE)
            || Settings.getValues().mInputAttributes.mNoLearning

    @JvmStatic
    fun apply(view: FrameLayout) {
        view.foreground = if (!isActive(view)) null else GradientDrawable().apply {
            setColor(ColorUtils.setAlphaComponent(COLOR, TINT_ALPHA))
            setStroke(BORDER_DP.dpToPx(view.resources), COLOR)
        }
    }
}
