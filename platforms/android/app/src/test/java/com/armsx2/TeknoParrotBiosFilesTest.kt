package com.armsx2

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeknoParrotBiosFilesTest {
    @Test
    fun findsExactCompleteSystem246Pair() {
        withTemporaryDirectory { directory ->
            sizedFile(
                directory,
                TeknoParrotBiosFiles.SYSTEM_246_PRIMARY_NAME,
                TeknoParrotBiosFiles.SYSTEM_246_CHIP_BYTES,
            )
            sizedFile(
                directory,
                TeknoParrotBiosFiles.SYSTEM_246_SECONDARY_NAME.uppercase(),
                TeknoParrotBiosFiles.SYSTEM_246_CHIP_BYTES,
            )

            val result = TeknoParrotBiosFiles.findSystem246SplitSet(directory)

            assertEquals(
                TeknoParrotBiosFiles.SYSTEM_246_PRIMARY_NAME,
                result?.primary?.name,
            )
            assertEquals(
                TeknoParrotBiosFiles.SYSTEM_246_SECONDARY_NAME.uppercase(),
                result?.secondary?.name,
            )
        }
    }

    @Test
    fun rejectsMissingOrWrongSizedSystem246Pair() {
        withTemporaryDirectory { directory ->
            sizedFile(
                directory,
                TeknoParrotBiosFiles.SYSTEM_246_PRIMARY_NAME,
                TeknoParrotBiosFiles.SYSTEM_246_CHIP_BYTES,
            )
            assertNull(TeknoParrotBiosFiles.findSystem246SplitSet(directory))

            sizedFile(
                directory,
                TeknoParrotBiosFiles.SYSTEM_246_SECONDARY_NAME,
                TeknoParrotBiosFiles.SYSTEM_246_CHIP_BYTES - 1,
            )
            assertNull(TeknoParrotBiosFiles.findSystem246SplitSet(directory))
        }
    }

    @Test
    fun standardBiosSizeDoesNotAcceptOneSystem246Chip() {
        withTemporaryDirectory { directory ->
            val chip =
                sizedFile(
                    directory,
                    "bios.bin",
                    TeknoParrotBiosFiles.SYSTEM_246_CHIP_BYTES,
                )
            assertTrue(!TeknoParrotBiosFiles.hasStandardBiosSize(chip))
        }
    }

    private fun sizedFile(directory: File, name: String, size: Long): File =
        File(directory, name).also { file ->
            RandomAccessFile(file, "rw").use { it.setLength(size) }
        }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = createTempDir(prefix = "tp-bios-test-")
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
