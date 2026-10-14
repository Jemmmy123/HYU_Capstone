// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import android.content.Context
import androidx.core.content.edit
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.NgramContext.WordInfo
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.prefs
import java.security.SecureRandom

/** Connects [SensitiveWordDetector] to learning: decides what may go into the user history dictionary. */
object PrivacyFilter {
    private const val TAG = "PrivacyFilter"

    /** Key prefix for the number of blocked words per type. Only counts are stored, never the words. */
    const val PREF_BLOCKED_COUNT_PREFIX = "privacy_blocked_count_"

    /** Entries of [UserBlockList] (hashes and masks, not the words) and the salt for hashing them. */
    const val PREF_USER_BLOCK_LIST = "privacy_user_block_list"
    private const val PREF_USER_BLOCK_LIST_SALT = "privacy_user_block_list_salt"

    /** Removes a word from what the keyboard has already learned. Set by the keyboard service while it runs. */
    fun interface WordForgetter { fun forget(word: String) }

    @JvmStatic
    var wordForgetter: WordForgetter? = null

    private var cachedUserList: UserBlockList? = null
    private var cachedUserListSource: Pair<String, Set<String>>? = null

    /** @return true if [word] must not be learned. */
    @JvmStatic
    fun shouldBlock(word: String, ngramContext: NgramContext, context: Context): Boolean {
        val start = System.nanoTime()
        val previousWords = (1..ngramContext.prevWordCount).mapNotNull { ngramContext.getNthPrevWord(it)?.toString() }
        val userList = userBlockList(context)
        val type = word.split(' ').firstNotNullOfOrNull { SensitiveWordDetector.detect(it, previousWords, userList) }
        if (type != null) countBlocked(context, type)
        // timing for the evaluation, debug builds only; never logs the word
        if (BuildConfig.DEBUG) Log.d(TAG, "check took ${(System.nanoTime() - start) / 1000} µs, blocked: ${type != null}")
        if (type == null) return false
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

    @JvmStatic
    fun userBlockList(context: Context): UserBlockList {
        val prefs = context.prefs()
        val source = (prefs.getString(PREF_USER_BLOCK_LIST_SALT, null) ?: "") to
                (prefs.getStringSet(PREF_USER_BLOCK_LIST, null) ?: emptySet())
        synchronized(this) {
            if (source != cachedUserListSource) {
                cachedUserList = UserBlockList(source.first, source.second)
                cachedUserListSource = source.first to source.second.toSet()
            }
            return cachedUserList!!
        }
    }

    /**
     * Adds [word] to the user's block list and removes it from what was already learned.
     * @return false if the word is blank or already in the list
     */
    @JvmStatic
    fun addToUserBlockList(context: Context, word: String): Boolean {
        val prefs = context.prefs()
        val salt = prefs.getString(PREF_USER_BLOCK_LIST_SALT, null) ?: ByteArray(16)
            .also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val list = UserBlockList(salt, prefs.getStringSet(PREF_USER_BLOCK_LIST, null) ?: emptySet())
        val entry = list.entryFor(word) ?: return false
        if (list.entries.any { it.hash == entry.hash }) return false
        prefs.edit {
            putString(PREF_USER_BLOCK_LIST_SALT, salt)
            putStringSet(PREF_USER_BLOCK_LIST, list.entries.map { it.serialize() }.toSet() + entry.serialize())
        }
        val forgetter = wordForgetter
        if (forgetter == null) Log.w(TAG, "keyboard not running, can't remove the word from what was learned")
        else forgetter.forget(word.trim().trimEnd('.', ',', '!', '?', ';'))
        return true
    }

    @JvmStatic
    fun removeFromUserBlockList(context: Context, entry: UserBlockList.Entry) {
        val prefs = context.prefs()
        val current = prefs.getStringSet(PREF_USER_BLOCK_LIST, null) ?: return
        prefs.edit { putStringSet(PREF_USER_BLOCK_LIST, current - entry.serialize()) }
    }

    /**
     * The previous words are stored together with the learned word (for next-word suggestions),
     * so a sensitive previous word like a card number would end up in the dictionary file.
     * Cut the context before the first sensitive previous word.
     */
    @JvmStatic
    fun sanitize(ngramContext: NgramContext, context: Context): NgramContext {
        val userList = userBlockList(context)
        val kept = mutableListOf<WordInfo>()
        for (n in 1..ngramContext.prevWordCount) {
            val prev = ngramContext.getNthPrevWord(n)
            if (ngramContext.isNthPrevWordBeginningOfSentence(n)) {
                kept.add(WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO)
                break
            }
            if (prev == null || SensitiveWordDetector.isSensitive(prev.toString(), userList)) break
            kept.add(WordInfo(prev))
        }
        if (kept.size == ngramContext.prevWordCount) return ngramContext
        return if (kept.isEmpty()) NgramContext.EMPTY_PREV_WORDS_INFO else NgramContext(*kept.toTypedArray())
    }
}
