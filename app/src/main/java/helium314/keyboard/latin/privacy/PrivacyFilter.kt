// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import android.content.Context
import androidx.core.content.edit
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.NgramContext.WordInfo
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.prefs

/** Connects [SensitiveWordDetector] to learning: decides what may go into the user history dictionary. */
object PrivacyFilter {
    private const val TAG = "PrivacyFilter"

    /** Key prefix for the number of blocked words per type. Only counts are stored, never the words. */
    const val PREF_BLOCKED_COUNT_PREFIX = "privacy_blocked_count_"

    /** @return true if [word] must not be learned. */
    @JvmStatic
    fun shouldBlock(word: String, ngramContext: NgramContext, context: Context): Boolean {
        val previousWords = (1..ngramContext.prevWordCount).mapNotNull { ngramContext.getNthPrevWord(it)?.toString() }
        val type = word.split(' ').firstNotNullOfOrNull { SensitiveWordDetector.detect(it, previousWords) }
            ?: return false
        countBlocked(context, type)
        Log.i(TAG, "not learning a word of type $type")
        return true
    }

    private fun countBlocked(context: Context, type: SensitiveWordDetector.Type) {
        val prefs = context.prefs()
        val key = PREF_BLOCKED_COUNT_PREFIX + type.name
        prefs.edit { putInt(key, prefs.getInt(key, 0) + 1) }
    }

    /** Blocked words per type since the last reset; types with no blocked words are left out. */
    @JvmStatic
    fun readBlockedCounts(context: Context): Map<SensitiveWordDetector.Type, Int> {
        val prefs = context.prefs()
        return SensitiveWordDetector.Type.entries
            .associateWith { prefs.getInt(PREF_BLOCKED_COUNT_PREFIX + it.name, 0) }
            .filterValues { it > 0 }
    }

    @JvmStatic
    fun resetBlockedCounts(context: Context) {
        context.prefs().edit { SensitiveWordDetector.Type.entries.forEach { remove(PREF_BLOCKED_COUNT_PREFIX + it.name) } }
    }

    /**
     * The previous words are stored together with the learned word (for next-word suggestions),
     * so a sensitive previous word like a card number would end up in the dictionary file.
     * Cut the context before the first sensitive previous word.
     */
    @JvmStatic
    fun sanitize(ngramContext: NgramContext): NgramContext {
        val kept = mutableListOf<WordInfo>()
        for (n in 1..ngramContext.prevWordCount) {
            val prev = ngramContext.getNthPrevWord(n)
            if (ngramContext.isNthPrevWordBeginningOfSentence(n)) {
                kept.add(WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO)
                break
            }
            if (prev == null || SensitiveWordDetector.isSensitive(prev.toString())) break
            kept.add(WordInfo(prev))
        }
        if (kept.size == ngramContext.prevWordCount) return ngramContext
        return if (kept.isEmpty()) NgramContext.EMPTY_PREV_WORDS_INFO else NgramContext(*kept.toTypedArray())
    }
}
