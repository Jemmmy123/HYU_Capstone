// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.privacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserBlockListTest {
    private fun listOf(vararg words: String): UserBlockList {
        val empty = UserBlockList("salt", emptyList())
        return UserBlockList("salt", words.map { empty.entryFor(it)!!.serialize() })
    }

    @Test fun matchesIgnoringCaseAndPunctuation() {
        val list = listOf("bluefish")
        assertTrue("bluefish" in list)
        assertTrue("Bluefish" in list)
        assertTrue("BLUEFISH." in list)
        assertFalse("bluefin" in list)
        assertFalse("" in list)
    }

    @Test fun wordsAreNotStored() {
        val entry = listOf("bluefish").entries.single()
        assertFalse(entry.serialize().contains("bluefish"))
        assertEquals("b•••••••", entry.mask)
        assertEquals(64, entry.hash.length)
    }

    @Test fun saltChangesHash() {
        assertFalse(UserBlockList.hash("a", "bluefish") == UserBlockList.hash("b", "bluefish"))
    }

    @Test fun blankAndBrokenEntries() {
        assertNull(listOf().entryFor("  "))
        assertTrue(UserBlockList("salt", kotlin.collections.listOf("no separator", "|x")).isEmpty())
    }

    @Test fun usedByDetector() {
        val list = listOf("bluefish")
        assertEquals(SensitiveWordDetector.Type.USER_LIST, SensitiveWordDetector.detect("Bluefish", emptyList(), list))
        assertNull(SensitiveWordDetector.detect("goldfish", emptyList(), list))
        assertTrue(SensitiveWordDetector.isSensitive("bluefish", list))
        assertFalse(SensitiveWordDetector.isSensitive("bluefish"))
    }
}
