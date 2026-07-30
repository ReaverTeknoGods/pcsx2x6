package com.armsx2

import java.io.File
import java.security.MessageDigest

/**
 * File-only BIOS checks shared by the protected TeknoParrot setup surface and
 * its readiness receiver. This companion runs System 246/256 arcade software,
 * so only the exact two-chip 2 MiB arcade ROM set is accepted.
 */
internal object TeknoParrotBiosFiles {
    const val SYSTEM_246_CHIP_BYTES = 2L * 1024L * 1024L
    const val SYSTEM_246_PRIMARY_NAME = "r27v1602f.7d"
    const val SYSTEM_246_SECONDARY_NAME = "r27v1602f.8g"
    private const val SYSTEM_246_PRIMARY_SHA256 =
        "8eb8f4d963dde4659de4fbb6eca870077f9af59a9100e8eecd59ec836ff79fae"
    private const val SYSTEM_246_SECONDARY_SHA256 =
        "5ec8cacdc07583691e12d8260be8359c43980429edd455878508faa5d0644ed0"

    data class System246SplitSet(
        val primary: File,
        val secondary: File,
    )

    fun findSystem246SplitSet(directory: File): System246SplitSet? {
        val files = directory.listFiles()?.filter(File::isFile).orEmpty()
        val primary =
            files.singleOrNull {
                it.name.equals(SYSTEM_246_PRIMARY_NAME, ignoreCase = true) &&
                    hasDigest(it, SYSTEM_246_PRIMARY_SHA256)
            } ?: return null
        val secondary =
            files.singleOrNull {
                it.name.equals(SYSTEM_246_SECONDARY_NAME, ignoreCase = true) &&
                    hasDigest(it, SYSTEM_246_SECONDARY_SHA256)
            } ?: return null
        return System246SplitSet(primary, secondary)
    }

    private fun hasDigest(file: File, expected: String): Boolean {
        if (file.length() != SYSTEM_246_CHIP_BYTES)
            return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0)
                    break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        } == expected
    }

}
