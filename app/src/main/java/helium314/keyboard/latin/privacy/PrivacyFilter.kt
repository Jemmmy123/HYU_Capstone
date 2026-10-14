// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.NgramContext.WordInfo
import helium314.keyboard.latin.utils.Log

/** Connects [SensitiveWordDetector] to learning: decides what may go into the user history dictionary. */
object PrivacyFilter {
    private const val TAG = "PrivacyFilter"

    /** Number of words blocked since the keyboard started, by type. Only counts are kept, never the words. */
    val blockedCounts = mutableMapOf<SensitiveWordDetector.Type, Int>()

    /** @return true if [word] must not be learned. */
    @JvmStatic
    fun shouldBlock(word: String, ngramContext: NgramContext): Boolean {
        val previousWords = (1..ngramContext.prevWordCount).mapNotNull { ngramContext.getNthPrevWord(it)?.toString() }
        val type = word.split(' ').firstNotNullOfOrNull { SensitiveWordDetector.detect(it, previousWords) }
            ?: return false
        blockedCounts[type] = (blockedCounts[type] ?: 0) + 1
        Log.i(TAG, "not learning a word of type $type")
        return true
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
