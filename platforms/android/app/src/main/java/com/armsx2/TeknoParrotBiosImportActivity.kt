package com.armsx2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.armsx2.runtime.MainActivityRuntime
import kr.co.iefriends.pcsx2.NativeApp
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Single-purpose, signature-protected BIOS picker used by TeknoParrotUI.
 * It intentionally exposes none of PCSX2X6's standalone library UI.
 */
class TeknoParrotBiosImportActivity : ComponentActivity() {
    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isEmpty()) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@registerForActivityResult
            }

            Thread {
                val result = importBios(uris)
                runOnUiThread {
                    if (result != null) {
                        Toast.makeText(
                            this,
                            "PCSX2X6 BIOS configured: ${result.name}",
                            Toast.LENGTH_LONG,
                        ).show()
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_BIOS_NAME, result.name),
                        )
                    } else {
                        Toast.makeText(
                            this,
                            "Select one valid PlayStation 2 BIOS, or both " +
                                "${TeknoParrotBiosFiles.SYSTEM_246_PRIMARY_NAME} and " +
                                "${TeknoParrotBiosFiles.SYSTEM_246_SECONDARY_NAME}.",
                            Toast.LENGTH_LONG,
                        ).show()
                        setResult(Activity.RESULT_CANCELED)
                    }
                    finish()
                }
            }.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            picker.launch(arrayOf("application/octet-stream", "application/x-bios"))
        }
    }

    private fun importBios(uris: List<Uri>): File? =
        if (uris.size == 1) {
            importStandardBios(uris.single())
        } else {
            importSystem246SplitSet(uris)
        }

    private fun importStandardBios(uri: Uri): File? = runCatching {
        val descriptor = contentResolver.openFileDescriptor(uri, "r")
            ?: return@runCatching null
        val info = NativeApp.getBiosInfoFromFd(descriptor.detachFd())
            ?: return@runCatching null
        if (info.description.isBlank())
            return@runCatching null

        val rawName =
            DocumentFile.fromSingleUri(this, uri)
                ?.name
                ?.takeIf(String::isNotBlank)
                ?: "bios.bin"
        val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val directory =
            MainActivityRuntime.internalBiosDir(applicationContext).apply { mkdirs() }
        val target = uniqueTarget(directory, safeName)
        val temporary = File(directory, ".${target.name}.import")

        contentResolver.openInputStream(uri)?.use { input ->
            temporary.outputStream().use(input::copyTo)
        } ?: return@runCatching null
        if (!TeknoParrotBiosFiles.hasStandardBiosSize(temporary)) {
            temporary.delete()
            return@runCatching null
        }
        // Validate the bytes that were actually copied, not only the provider's
        // first descriptor. This closes the document-provider TOCTOU gap and
        // guarantees the path persisted below is itself a parseable BIOS.
        val copiedDescriptor = ParcelFileDescriptor.open(
            temporary,
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
        if (NativeApp.getBiosInfoFromFd(copiedDescriptor.detachFd()) == null) {
            temporary.delete()
            return@runCatching null
        }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = false)
            temporary.delete()
        }

        getSharedPreferences("ARMSX2", Context.MODE_PRIVATE)
            .edit()
            .putString("bios", target.absolutePath)
            .putString("biosDir", directory.absolutePath)
            .apply()

        // Keep a live frontend coherent if Android happened to retain it.
        if (MainActivityRuntime.instance != null) {
            MainActivityRuntime.bios.value = target.absolutePath
            MainActivityRuntime.biosDir.value = directory.absolutePath
        }
        target
    }.getOrNull()

    private fun importSystem246SplitSet(uris: List<Uri>): File? = runCatching {
        if (uris.size != 2)
            return@runCatching null
        val namedUris =
            uris.mapNotNull { uri ->
                DocumentFile.fromSingleUri(this, uri)
                    ?.name
                    ?.takeIf(String::isNotBlank)
                    ?.let { it to uri }
            }
        val primaryUri =
            namedUris.singleOrNull {
                it.first.equals(
                    TeknoParrotBiosFiles.SYSTEM_246_PRIMARY_NAME,
                    ignoreCase = true,
                )
            }?.second ?: return@runCatching null
        val secondaryUri =
            namedUris.singleOrNull {
                it.first.equals(
                    TeknoParrotBiosFiles.SYSTEM_246_SECONDARY_NAME,
                    ignoreCase = true,
                )
            }?.second ?: return@runCatching null

        val directory =
            MainActivityRuntime.internalBiosDir(applicationContext).apply { mkdirs() }
        val primary =
            File(directory, TeknoParrotBiosFiles.SYSTEM_246_PRIMARY_NAME)
        val secondary =
            File(directory, TeknoParrotBiosFiles.SYSTEM_246_SECONDARY_NAME)
        val primaryTemporary = File(directory, ".${primary.name}.import")
        val secondaryTemporary = File(directory, ".${secondary.name}.import")
        try {
            if (!copyChip(primaryUri, primaryTemporary) ||
                !copyChip(secondaryUri, secondaryTemporary)
            ) {
                return@runCatching null
            }
            Files.move(
                primaryTemporary.toPath(),
                primary.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
            Files.move(
                secondaryTemporary.toPath(),
                secondary.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            primaryTemporary.delete()
            secondaryTemporary.delete()
        }

        val splitSet =
            TeknoParrotBiosFiles.findSystem246SplitSet(directory)
                ?: return@runCatching null
        getSharedPreferences("ARMSX2", Context.MODE_PRIVATE)
            .edit()
            .putString("bios", splitSet.primary.absolutePath)
            .putString("biosDir", directory.absolutePath)
            .apply()
        if (MainActivityRuntime.instance != null) {
            MainActivityRuntime.bios.value = splitSet.primary.absolutePath
            MainActivityRuntime.biosDir.value = directory.absolutePath
        }
        splitSet.primary
    }.getOrNull()

    private fun copyChip(uri: Uri, destination: File): Boolean {
        destination.delete()
        contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use(input::copyTo)
        } ?: return false
        return destination.length() == TeknoParrotBiosFiles.SYSTEM_246_CHIP_BYTES
    }

    private fun uniqueTarget(directory: File, requestedName: String): File {
        val preferred = File(directory, requestedName)
        if (!preferred.exists())
            return preferred
        val stem = preferred.nameWithoutExtension.ifBlank { "bios" }
        val extension =
            preferred.extension.takeIf(String::isNotBlank)?.let { ".$it" }.orEmpty()
        var suffix = 2
        while (true) {
            val candidate = File(directory, "$stem-$suffix$extension")
            if (!candidate.exists())
                return candidate
            suffix++
        }
    }

    companion object {
        private const val EXTRA_BIOS_NAME =
            "com.teknoparrot.pcsx2x6.extra.BIOS_NAME"
    }
}
