package com.armsx2

import java.io.File
import java.io.RandomAccessFile

/** App-owned writable storage required by the System 246/256 arcade core. */
internal object TeknoParrotArcadeStorage {
    internal const val SRAM_SIZE_BYTES = 32L * 1024L
    private val safeName = Regex("""[A-Za-z0-9_. -]+""")

    /**
     * Create the same blank 32 KiB SRAM image the desktop core creates on a
     * game's first boot. Existing valid state is never overwritten.
     */
    fun ensureBlankSram(dataDirectory: File, manifestName: String): File {
        require(safeName.matches(manifestName) && manifestName !in setOf(".", "..")) {
            "The game manifest has an invalid SRAM file name."
        }

        val canonicalDirectory = dataDirectory.canonicalFile
        require(canonicalDirectory.isDirectory) {
            "The game data directory is unavailable."
        }
        val sram = File(canonicalDirectory, manifestName).canonicalFile
        require(sram.parentFile == canonicalDirectory) {
            "The game manifest SRAM path escapes its data directory."
        }

        if (sram.exists()) {
            require(sram.isFile) { "The SRAM path is not a file." }
            require(sram.length() == 0L || sram.length() == SRAM_SIZE_BYTES) {
                "The existing SRAM image has an invalid size (${sram.length()} bytes)."
            }
        }

        if (!sram.exists() || sram.length() == 0L) {
            RandomAccessFile(sram, "rw").use { file ->
                file.setLength(SRAM_SIZE_BYTES)
            }
        }
        return sram
    }
}
