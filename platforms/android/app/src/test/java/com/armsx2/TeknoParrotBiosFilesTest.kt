package com.armsx2

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertNull
import org.junit.Test

class TeknoParrotBiosFilesTest {
    @Test
    fun rejectsCorrectNamesWithWrongContents() {
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

            // File names and sizes alone are intentionally insufficient. The
            // production helper requires the known System 246 ROM digests.
            assertNull(result)
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
