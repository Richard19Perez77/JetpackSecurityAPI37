package com.rick.jetpacksecurityapi37

import com.rick.jetpacksecurityapi37.crypto.EncryptedPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedPathsTest {

    @Test
    fun acceptsPlainFileName() {
        assertTrue(EncryptedPaths.isSingleSegment("note.txt"))
    }

    @Test
    fun rejectsParentAndSeparators() {
        assertFalse(EncryptedPaths.isSingleSegment(""))
        assertFalse(EncryptedPaths.isSingleSegment("."))
        assertFalse(EncryptedPaths.isSingleSegment(".."))
        assertFalse(EncryptedPaths.isSingleSegment("../secrets"))
        assertFalse(EncryptedPaths.isSingleSegment("dir/note.txt"))
        assertFalse(EncryptedPaths.isSingleSegment("dir\\note.txt"))
    }
}
