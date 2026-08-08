package com.armsx2

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TeknoParrotArcadeStorageTest {
    @Test
    fun missingSramIsGeneratedAtNativeCabinetSize() {
        withTemporaryDirectory { directory ->
            val sram = TeknoParrotArcadeStorage.ensureBlankSram(directory, "sram.bin")

            assertEquals(TeknoParrotArcadeStorage.SRAM_SIZE_BYTES, sram.length())
            assertEquals(0, sram.inputStream().use { it.read() })
        }
    }

    @Test
    fun existingSramIsPreserved() {
        withTemporaryDirectory { directory ->
            val sram = File(directory, "cabinet.sram")
            sram.writeBytes(ByteArray(TeknoParrotArcadeStorage.SRAM_SIZE_BYTES.toInt()) { 0x5A })

            TeknoParrotArcadeStorage.ensureBlankSram(directory, sram.name)

            assertEquals(0x5A, sram.inputStream().use { it.read() })
        }
    }

    @Test
    fun partialOrEscapingSramIsRejectedWithoutOverwrite() {
        withTemporaryDirectory { directory ->
            val partial = File(directory, "sram.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }

            assertThrows(IllegalArgumentException::class.java) {
                TeknoParrotArcadeStorage.ensureBlankSram(directory, partial.name)
            }
            assertEquals(3L, partial.length())
            assertThrows(IllegalArgumentException::class.java) {
                TeknoParrotArcadeStorage.ensureBlankSram(directory, "../sram.bin")
            }
        }
    }

    private fun withTemporaryDirectory(action: (File) -> Unit) {
        val directory = Files.createTempDirectory("tekno2x6-sram-test").toFile()
        try {
            action(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
