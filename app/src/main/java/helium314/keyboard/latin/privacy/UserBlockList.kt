// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import java.security.MessageDigest

/**
 * Words the user never wants learned, for secrets that automatic detection can't recognize.
 * The words themselves are not stored: each entry keeps a salted SHA-256 hash for matching and a mask
 * (first letter and length, like "b•••••••") so the user can tell entries apart.
 *
 * Pure Kotlin without Android dependencies, so it can be unit tested on the JVM.
 */
class UserBlockList(private val salt: String, serializedEntries: Collection<String>) {

    data class Entry(val hash: String, val mask: String) {
        fun serialize() = "$hash$SEPARATOR$mask"
    }

    val entries: List<Entry> = serializedEntries.mapNotNull { parse(it) }.sortedBy { it.mask }
    private val hashes = entries.mapTo(HashSet()) { it.hash }

    fun isEmpty() = entries.isEmpty()

    /** Case-insensitive, ignores trailing punctuation like "bluefish." */
    operator fun contains(word: String): Boolean {
        if (hashes.isEmpty()) return false
        val w = normalize(word)
        return w.isNotEmpty() && hash(salt, w) in hashes
    }

    /** @return the entry to store for [word], or null if [word] is blank */
    fun entryFor(word: String): Entry? {
        val w = normalize(word)
        if (w.isEmpty()) return null
        return Entry(hash(salt, w), mask(w))
    }

    companion object {
        private const val SEPARATOR = '|'

        fun normalize(word: String) = word.trim().trimEnd('.', ',', '!', '?', ';').lowercase()

        fun mask(normalized: String) = normalized.take(1) + "•".repeat(normalized.length - 1)

        fun hash(salt: String, normalized: String): String =
            MessageDigest.getInstance("SHA-256").digest((salt + normalized).toByteArray())
                .joinToString("") { "%02x".format(it) }

        private fun parse(serialized: String): Entry? {
            val i = serialized.indexOf(SEPARATOR)
            if (i <= 0) return null
            return Entry(serialized.substring(0, i), serialized.substring(i + 1))
        }
    }
}
