// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

/**
 * Decides whether a typed word looks like sensitive information that must not be learned
 * into the user history dictionary (card numbers, phone numbers, emails, password-like words, ...).
 *
 * Pure Kotlin without Android dependencies, so it can be unit tested on the JVM.
 */
object SensitiveWordDetector {

    enum class Type { CARD_NUMBER, PHONE_NUMBER, EMAIL, SSN, LONG_NUMBER, PASSWORD_LIKE, AFTER_TRIGGER_WORD, USER_LIST }

    /** Words after which the next word is never learned, e.g. "password: hunter2" or "my pin is 1234". */
    private val TRIGGER_WORDS = setOf(
        "password", "passwd", "pwd", "pw", "pass", "passcode", "passphrase", "pin", "otp",
        "ssn", "account", "acct", "card", "cvv", "cvc", "iban", "routing", "login", "username",
    )

    /** Words allowed between a trigger word and the sensitive word, e.g. "password is X". */
    private val FILLER_WORDS = setOf("is", "was", "=", ":", "-", "number", "no", "#")

    /** Common short words that are never the secret themselves, e.g. "with" in "login with X". */
    private val FUNCTION_WORDS = FILLER_WORDS + setOf(
        "a", "an", "the", "and", "or", "to", "for", "of", "in", "on", "at", "by", "with", "from",
        "my", "your", "our", "his", "her", "their", "me", "it", "this", "that", "are", "be",
    )

    private val EMAIL = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")
    private val PARTIAL_EMAIL = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+$""")
    private val SSN = Regex("""^\d{3}-\d{2}-\d{4}$""")
    private val NUMBER_LIKE = Regex("""^\+?[\d\s().-]+$""")
    private val PHONE = Regex("""^\+?\(?(\d[\s().-]?){7,15}$""")

    private const val MIN_LONG_NUMBER_DIGITS = 6
    private const val MIN_PASSWORD_LENGTH = 8

    /**
     * @param word the word about to be learned
     * @param previousWords words typed before [word], nearest first (index 0 is the word right before)
     * @param userList words the user added to never be learned
     * @return the kind of sensitive information, or null if the word may be learned
     */
    fun detect(word: String, previousWords: List<String> = emptyList(), userList: UserBlockList? = null): Type? {
        val w = word.trim().trimEnd('.', ',', '!', '?', ';')
        if (w.isEmpty()) return null
        if (userList != null && w in userList) return Type.USER_LIST
        detectByShape(w)?.let { return it }
        if (w.lowercase() in FUNCTION_WORDS) return null
        if (followsTriggerWord(previousWords)) return Type.AFTER_TRIGGER_WORD
        return null
    }

    /** True if [word] alone looks sensitive, ignoring context. Used to clean previous-word context. */
    fun isSensitive(word: String, userList: UserBlockList? = null): Boolean =
        detectByShape(word.trim().trimEnd('.', ',', '!', '?', ';')) != null || (userList != null && word in userList)

    private fun detectByShape(w: String): Type? {
        if (w.isEmpty()) return null
        if (EMAIL.matches(w)) return Type.EMAIL
        if (NUMBER_LIKE.matches(w)) {
            val digits = w.filter { it.isDigit() }
            if (SSN.matches(w)) return Type.SSN
            if (digits.length in 13..19 && passesLuhn(digits)) return Type.CARD_NUMBER
            if (PHONE.matches(w) && (w.startsWith("+") || w.any { !it.isDigit() } || digits.length in 10..11))
                return Type.PHONE_NUMBER
            if (digits.length >= MIN_LONG_NUMBER_DIGITS) return Type.LONG_NUMBER
            return null
        }
        // the keyboard splits "john@mail.com" at '.', so a learned word may be just "john@mail"
        if (PARTIAL_EMAIL.matches(w)) return Type.EMAIL
        if (looksLikePassword(w)) return Type.PASSWORD_LIKE
        // letters mixed with many digits, e.g. "acct12345678"
        if (w.count { it.isDigit() } >= MIN_LONG_NUMBER_DIGITS) return Type.LONG_NUMBER
        return null
    }

    /** Password-like: long enough, has lowercase and a digit or symbol, and mixes at least 3 character classes. */
    private fun looksLikePassword(w: String): Boolean {
        if (w.length < MIN_PASSWORD_LENGTH || w.any { it.isWhitespace() }) return false
        val hasLower = w.any { it.isLowerCase() }
        val hasUpper = w.any { it.isUpperCase() }
        val hasDigit = w.any { it.isDigit() }
        val hasSymbol = w.any { !it.isLetterOrDigit() }
        if (!hasDigit && !hasSymbol) return false
        if (!hasLower) return false // all-caps words like "COVID-19" are usually not passwords
        val classes = listOf(hasLower, hasUpper, hasDigit, hasSymbol).count { it }
        return classes >= 3
    }

    private fun followsTriggerWord(previousWords: List<String>): Boolean {
        for (prev in previousWords.take(3)) {
            val p = prev.lowercase().trim().trimEnd(':', '=', '-', '#')
            if (p in TRIGGER_WORDS) return true
            if (p.isEmpty() || p in FILLER_WORDS) continue
            return false
        }
        return false
    }

    /** Luhn checksum used by payment card numbers. */
    fun passesLuhn(digits: String): Boolean {
        var sum = 0
        var double = false
        for (i in digits.indices.reversed()) {
            var d = digits[i] - '0'
            if (d !in 0..9) return false
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return sum % 10 == 0
    }
}
