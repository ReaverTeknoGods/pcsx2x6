package com.armsx2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Narrow, signature-protected System 246/256 package importer used by
 * TeknoParrotUI. The user grants temporary access with Android's document
 * picker; validated game data is copied into this companion's scoped storage.
 */
class TeknoParrotGameImportActivity : ComponentActivity() {
    private lateinit var status: TextView
    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                finishWithFailure("Game import cancelled.")
                return@registerForActivityResult
            }

            status.text = "Importing game files… This can take several minutes."
            Thread {
                val result = importPackage(uri)
                runOnUiThread {
                    if (result.isSuccess) {
                        val manifestName = result.getOrThrow()
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_IMPORTED_MANIFEST, manifestName),
                        )
                        status.text = "Game files imported."
                        finish()
                    } else {
                        finishWithFailure(
                            result.exceptionOrNull()?.message
                                ?: "The selected game package is invalid.",
                        )
                    }
                }
            }.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        activeInstances.incrementAndGet()
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            text = "Select the folder containing the .acgame file and its game-data folder."
            textSize = 18f
            setPadding(32, 24, 32, 24)
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                addView(status)
                addView(ProgressBar(this@TeknoParrotGameImportActivity))
            },
        )

        if (savedInstanceState == null)
            picker.launch(null)
    }

    override fun onDestroy() {
        activeInstances.decrementAndGet()
        super.onDestroy()
    }

    private fun importPackage(uri: Uri): Result<String> = runCatching {
        val expectedManifest =
            intent.getStringExtra(EXTRA_EXPECTED_MANIFEST)
                ?.trim()
                ?.takeIf {
                    SAFE_MANIFEST.matches(it) &&
                        it.endsWith(".acgame", ignoreCase = true)
                }
                ?: error("TeknoParrotUI did not provide a valid game manifest name.")
        val sourceRoot =
            DocumentFile.fromTreeUri(this, uri)
                ?.takeIf(DocumentFile::isDirectory)
                ?: error("The selected game folder cannot be opened.")
        val sourceManifest =
            sourceRoot.listFiles().singleOrNull {
                it.isFile && it.name.equals(expectedManifest, ignoreCase = true)
            } ?: error(
                "Select the folder containing $expectedManifest and its matching data folder.",
            )
        val manifestBytes = readManifestBytes(sourceManifest.uri)
        val values = parseManifest(manifestBytes.toString(Charsets.UTF_8))
        val gameId =
            values["game.gameid"]
                ?.takeIf { GAME_ID.matches(it) }
                ?: error("The game manifest has an invalid cabinet ID.")
        val subdir =
            values["data.subdir"]
                ?.takeIf { SAFE_DATA_NAME.matches(it) }
                ?: error("The game manifest has an invalid data folder.")
        val requiredNames =
            listOf("elf", "dongle", "mediasrc").associateWith { key ->
                values["data.$key"]
                    ?.takeIf { SAFE_DATA_NAME.matches(it) }
                    ?: error("The game manifest has an invalid $key file.")
            }
        require(
            expectedManifest.substringBeforeLast('.').equals(gameId, ignoreCase = true) ||
                expectedManifest.substringBeforeLast('.').contains(' ')
        ) {
            "The selected manifest does not match the requested game."
        }
        val sourceData =
            sourceRoot.listFiles().singleOrNull {
                it.isDirectory && it.name.equals(subdir, ignoreCase = true)
            } ?: error("The selected folder does not contain the $subdir game-data folder.")

        val externalRoot =
            getExternalFilesDir(null) ?: error("Android app storage is unavailable.")
        val gameRoot = File(externalRoot, "TeknoParrot/games").apply { mkdirs() }
        val staging = File(gameRoot, ".importing-${UUID.randomUUID()}").apply {
            check(mkdirs()) { "Could not create temporary game storage." }
        }
        try {
            val stagedManifest = File(staging, expectedManifest)
            stagedManifest.outputStream().use { it.write(manifestBytes) }
            val stagedData = File(staging, subdir).apply {
                check(mkdirs()) { "Could not create temporary game-data storage." }
            }
            val copiedFiles = intArrayOf(0)
            copyTree(sourceData, stagedData, 0, copiedFiles)

            val requiredFiles = requiredNames.values.map { File(stagedData, it) }
            val missing = requiredFiles.firstOrNull { !it.isFile || it.length() <= 0L }
            require(missing == null) {
                "The selected package is incomplete: ${missing?.name ?: "required file"} is missing."
            }
            // SRAM is writable cabinet state, not dump content. Desktop creates
            // it automatically; do the same here so users never need to source
            // or copy another machine's per-game state image.
            TeknoParrotArcadeStorage.ensureBlankSram(
                stagedData,
                values["data.sram"] ?: "sram.bin",
            )

            installAtomically(
                gameRoot,
                stagedManifest,
                stagedData,
                expectedManifest,
                subdir,
            )
        } finally {
            staging.deleteRecursively()
        }
        expectedManifest
    }

    private fun copyTree(
        source: DocumentFile,
        destination: File,
        depth: Int,
        copiedFiles: IntArray,
    ) {
        require(depth <= MAX_TREE_DEPTH) { "The selected game folder is nested too deeply." }
        for (child in source.listFiles()) {
            val name =
                child.name?.takeIf(::isSafeDocumentName)
                    ?: error("The selected folder contains an unsafe file name.")
            copiedFiles[0]++
            require(copiedFiles[0] <= MAX_FILES) {
                "The selected game folder contains too many files."
            }
            val target = File(destination, name)
            require(target.canonicalFile.parentFile == destination.canonicalFile) {
                "The selected folder contains an unsafe path."
            }
            require(!target.exists()) {
                "The selected folder contains duplicate file names."
            }
            if (child.isDirectory) {
                check(target.mkdir()) { "Could not create $name." }
                copyTree(child, target, depth + 1, copiedFiles)
            } else if (child.isFile) {
                contentResolver.openInputStream(child.uri)?.use { input ->
                    target.outputStream().use(input::copyTo)
                } ?: error("Could not read $name.")
                require(target.length() > 0L) { "$name is empty." }
            }
        }
    }

    private fun installAtomically(
        gameRoot: File,
        stagedManifest: File,
        stagedData: File,
        manifestName: String,
        subdir: String,
    ) {
        val finalManifest = File(gameRoot, manifestName)
        val finalData = File(gameRoot, subdir)
        val backupRoot = File(gameRoot, ".backup-${UUID.randomUUID()}").apply {
            check(mkdir()) { "Could not prepare the game update." }
        }
        val backupManifest = File(backupRoot, manifestName)
        val backupData = File(backupRoot, subdir)
        var oldManifestMoved = false
        var oldDataMoved = false
        var installedManifestMoved = false
        var installedDataMoved = false
        var completed = false
        try {
            if (finalManifest.exists()) {
                check(finalManifest.renameTo(backupManifest)) {
                    "Could not replace the existing game manifest."
                }
                oldManifestMoved = true
            }
            if (finalData.exists()) {
                check(finalData.renameTo(backupData)) {
                    "Could not replace the existing game data."
                }
                oldDataMoved = true
            }
            check(stagedData.renameTo(finalData)) { "Could not install the game data." }
            installedDataMoved = true
            check(stagedManifest.renameTo(finalManifest)) {
                "Could not install the game manifest."
            }
            installedManifestMoved = true
            completed = true
        } catch (error: Throwable) {
            if (installedManifestMoved)
                finalManifest.delete()
            if (installedDataMoved)
                finalData.deleteRecursively()
            if (oldManifestMoved)
                backupManifest.renameTo(finalManifest)
            if (oldDataMoved)
                backupData.renameTo(finalData)
            throw error
        } finally {
            // Never delete the only remaining copy after a failed rollback.
            // A successful install can safely discard its temporary backup.
            if (completed)
                backupRoot.deleteRecursively()
        }
    }

    private fun parseManifest(text: String): Map<String, String> {
        var section = ""
        return buildMap {
            text.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith(';') || line.startsWith('#'))
                    return@forEach
                if (line.startsWith('[') && line.endsWith(']')) {
                    section = line.substring(1, line.length - 1).trim().lowercase()
                    return@forEach
                }
                val separator = line.indexOf('=')
                if (separator <= 0 || section.isBlank())
                    return@forEach
                val key = line.substring(0, separator).trim().lowercase()
                val value = line.substring(separator + 1).trim()
                put("$section.$key", value)
            }
        }
    }

    private fun readManifestBytes(uri: Uri): ByteArray =
        contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val read = input.read(buffer)
                if (read < 0)
                    break
                require(output.size() + read <= MAX_MANIFEST_BYTES) {
                    "The game manifest is unexpectedly large."
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: error("The game manifest cannot be read.")

    private fun isSafeDocumentName(name: String): Boolean =
        name.isNotBlank() &&
            name.length <= 200 &&
            name != "." &&
            name != ".." &&
            name.none { it == '/' || it == '\\' || it.isISOControl() }

    private fun finishWithFailure(message: String) {
        status.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(EXTRA_IMPORT_ERROR, message),
        )
        finish()
    }

    companion object {
        private val activeInstances = AtomicInteger()

        internal fun isActive(): Boolean = activeInstances.get() > 0

        const val EXTRA_EXPECTED_MANIFEST =
            "com.teknoparrot.pcsx2x6.extra.EXPECTED_MANIFEST"
        const val EXTRA_IMPORTED_MANIFEST =
            "com.teknoparrot.pcsx2x6.extra.IMPORTED_MANIFEST"
        const val EXTRA_IMPORT_ERROR =
            "com.teknoparrot.pcsx2x6.extra.IMPORT_ERROR"
        private const val MAX_MANIFEST_BYTES = 64 * 1024
        private const val MAX_FILES = 2048
        private const val MAX_TREE_DEPTH = 8
        private val GAME_ID = Regex("""NM\d{5}(?:_[A-Za-z0-9]+)?""")
        private val SAFE_MANIFEST = Regex("""[A-Za-z0-9_. -]+""")
        private val SAFE_DATA_NAME = Regex("""[A-Za-z0-9_. -]+""")
    }
}
