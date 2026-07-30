package com.armsx2

import java.io.File

/**
 * File-only BIOS checks shared by the protected TeknoParrot setup surface and
 * its readiness receiver. Conventional PS2 BIOS images are still validated by
 * the native ROMVER parser; the System 246 path is intentionally narrower
 * because its two 2 MiB ROM chips are not individually parseable as a normal
 * PS2 BIOS image.
 */
internal object TeknoParrotBiosFiles {
    const val MIN_STANDARD_BIOS_BYTES = 4L * 1024L * 1024L
    const val MAX_STANDARD_BIOS_BYTES = 8L * 1024L * 1024L
    const val SYSTEM_246_CHIP_BYTES = 2L * 1024L * 1024L
    const val SYSTEM_246_PRIMARY_NAME = "r27v1602f.7d"
    const val SYSTEM_246_SECONDARY_NAME = "r27v1602f.8g"

    data class System246SplitSet(
        val primary: File,
        val secondary: File,
    )

    fun findSystem246SplitSet(directory: File): System246SplitSet? {
        val files = directory.listFiles()?.filter(File::isFile).orEmpty()
        val primary =
            files.singleOrNull {
                it.name.equals(SYSTEM_246_PRIMARY_NAME, ignoreCase = true) &&
                    it.length() == SYSTEM_246_CHIP_BYTES
            } ?: return null
        val secondary =
            files.singleOrNull {
                it.name.equals(SYSTEM_246_SECONDARY_NAME, ignoreCase = true) &&
                    it.length() == SYSTEM_246_CHIP_BYTES
            } ?: return null
        return System246SplitSet(primary, secondary)
    }

    fun hasStandardBiosSize(file: File): Boolean =
        file.length() in MIN_STANDARD_BIOS_BYTES..MAX_STANDARD_BIOS_BYTES
}
