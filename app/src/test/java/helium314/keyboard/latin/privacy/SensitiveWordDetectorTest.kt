// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import helium314.keyboard.latin.privacy.SensitiveWordDetector.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SensitiveWordDetectorTest {
    private fun detect(word: String, vararg previous: String) = SensitiveWordDetector.detect(word, previous.toList())

    @Test fun cardNumbers() {
        assertEquals(Type.CARD_NUMBER, detect("4111111111111111"))
        assertEquals(Type.CARD_NUMBER, detect("4111-1111-1111-1111"))
        assertEquals(Type.CARD_NUMBER, detect("5500 0000 0000 0004"))
        // fails Luhn, but still a long number
        assertEquals(Type.LONG_NUMBER, detect("4111111111111112"))
    }

    @Test fun phoneNumbers() {
        assertEquals(Type.PHONE_NUMBER, detect("010-1234-5678"))
        assertEquals(Type.PHONE_NUMBER, detect("+1-202-555-0143"))
        assertEquals(Type.PHONE_NUMBER, detect("(202)555-0143"))
        assertEquals(Type.PHONE_NUMBER, detect("01012345678"))
    }

    @Test fun emails() {
        assertEquals(Type.EMAIL, detect("john.doe@example.com"))
        // the keyboard splits at '.', so only this part may reach learning
        assertEquals(Type.EMAIL, detect("john@example"))
    }

    @Test fun ssnAndLongNumbers() {
        assertEquals(Type.SSN, detect("123-45-6789"))
        assertEquals(Type.LONG_NUMBER, detect("110234567890"))
        assertEquals(Type.LONG_NUMBER, detect("acct12345678"))
    }

    @Test fun passwordLike() {
        assertEquals(Type.PASSWORD_LIKE, detect("Pa55w0rd!"))
        assertEquals(Type.PASSWORD_LIKE, detect("Summer2026"))
        assertEquals(Type.PASSWORD_LIKE, detect("hunter#22"))
    }

    @Test fun afterTriggerWord() {
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("hunter", "password"))
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("hunter", "is", "password", "my"))
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("1234", "pin"))
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("bluefish", "Password:"))
        assertNull(detect("tomorrow", "it", "is", "password"))
        // the connecting word itself is not the secret
        assertNull(detect("is", "password", "my"))
        assertNull(detect("with", "login"))
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("bluefish", "is", "password"))
    }

    @Test fun simplePasswords() {
        assertEquals(Type.PASSWORD_LIKE, detect("dragon42"))
        assertEquals(Type.PASSWORD_LIKE, detect("maple23"))
        assertEquals(Type.PASSWORD_LIKE, detect("T1g3r"))
        assertEquals(Type.PASSWORD_LIKE, detect("Dr4g0n"))
        for (w in listOf("mp3", "b2b", "i18n", "x86", "Win10", "3rd"))
            assertNull(detect(w), w)
    }

    @Test fun moreTriggerPhrases() {
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("letmein", "with", "login"))
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("falcon", "is", "code", "the"))
        assertEquals(Type.AFTER_TRIGGER_WORD, detect("falcon", "code:"))
        assertNull(detect("review", "code", "the"))
        assertNull(detect("manager", "password", "the"))
        assertNull(detect("game", "card", "the"))
        assertNull(detect("locked", "was", "account", "my"))
    }

    @Test fun normalWordsAreLearned() {
        for (w in listOf("hello", "Hello", "keyboard", "don't", "e-mail", "2026", "10:30", "COVID-19", "iPhone", "mp3", "B2B", "x86_64", ""))
            assertNull(detect(w), w)
        assertNull(detect("dinner", "for", "plans"))
    }

    @Test fun trailingPunctuationIgnored() {
        assertEquals(Type.CARD_NUMBER, detect("4111111111111111."))
        assertEquals(Type.EMAIL, detect("john@example.com,"))
    }

    @Test fun isSensitiveIgnoresContext() {
        assertTrue(SensitiveWordDetector.isSensitive("4111111111111111"))
        assertFalse(SensitiveWordDetector.isSensitive("password"))
    }

    @Test fun luhn() {
        assertTrue(SensitiveWordDetector.passesLuhn("79927398713"))
        assertFalse(SensitiveWordDetector.passesLuhn("79927398710"))
    }
}
