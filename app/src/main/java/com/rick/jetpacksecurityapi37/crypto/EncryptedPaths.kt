package com.rick.jetpacksecurityapi37.crypto

import java.io.File

/**
 * Keeps lab ciphertext under a dedicated filesDir subdirectory and rejects
 * names that could walk out of that directory.
 */
internal object EncryptedPaths {

    fun fileInDir(parent: File, dirName: String, fileName: String): File {
        require(isSingleSegment(fileName)) {
            "fileName must be a single path segment, was '$fileName'"
        }
        val dir = File(parent, dirName).apply { mkdirs() }
        return File(dir, fileName)
    }

    fun isSingleSegment(fileName: String): Boolean {
        if (fileName.isEmpty() || fileName == "." || fileName == "..") return false
        if (fileName.contains('/') || fileName.contains('\\')) return false
        return true
    }
}
