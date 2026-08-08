package com.armsx2

import java.io.File
import java.io.RandomAccessFile

/** Initializes the shared legacy JVS prefix consumed by the arcade core. */
internal object TeknoParrotInputPage {
    internal const val PAGE_SIZE_BYTES = 4096L
    internal const val LEGACY_SIZE_BYTES = 64
    internal const val STEERING_OFFSET = 13L
    internal const val STEERING_NEUTRAL = 0x80

    /**
     * Reset input left behind by an interrupted session and publish the exact
     * unsigned steering center expected by the System 246/256 driving boards.
     */
    fun prepare(page: File) {
        page.parentFile?.mkdirs()
        RandomAccessFile(page, "rw").use { file ->
            if (file.length() < PAGE_SIZE_BYTES)
                file.setLength(PAGE_SIZE_BYTES)

            file.seek(0L)
            file.write(ByteArray(LEGACY_SIZE_BYTES))
            file.seek(STEERING_OFFSET)
            file.write(STEERING_NEUTRAL)
        }
    }
}
