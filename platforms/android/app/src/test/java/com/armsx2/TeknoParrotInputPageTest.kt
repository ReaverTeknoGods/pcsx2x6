package com.armsx2

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class TeknoParrotInputPageTest {
    @Test
    fun newPageUsesCabinetNeutralSteering() {
        withTemporaryFile { page ->
            TeknoParrotInputPage.prepare(page)

            assertEquals(TeknoParrotInputPage.PAGE_SIZE_BYTES, page.length())
            val legacy = page.inputStream().use {
                it.readNBytes(TeknoParrotInputPage.LEGACY_SIZE_BYTES)
            }
            assertEquals(TeknoParrotInputPage.LEGACY_SIZE_BYTES, legacy.size)
            assertEquals(TeknoParrotInputPage.STEERING_NEUTRAL, legacy[13].toInt() and 0xff)
            assertEquals(0, legacy.filterIndexed { index, _ -> index != 13 }.sum())
        }
    }

    @Test
    fun staleSessionInputIsClearedWithoutTruncatingPage() {
        withTemporaryFile { page ->
            page.writeBytes(ByteArray(8192) { 0x5A })

            TeknoParrotInputPage.prepare(page)

            assertEquals(8192L, page.length())
            val bytes = page.readBytes()
            assertEquals(TeknoParrotInputPage.STEERING_NEUTRAL, bytes[13].toInt() and 0xff)
            assertEquals(0, bytes[9].toInt())
            assertEquals(0x5A, bytes[64].toInt() and 0xff)
        }
    }

    private fun withTemporaryFile(action: (File) -> Unit) {
        val directory = Files.createTempDirectory("tekno2x6-input-page-test").toFile()
        try {
            action(File(directory, "TeknoParrot_JvsState.page"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
