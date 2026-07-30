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

/**
 * Single-purpose, signature-protected BIOS picker used by TeknoParrotUI.
 * It intentionally exposes none of PCSX2X6's standalone library UI.
 */
class TeknoParrotBiosImportActivity : ComponentActivity() {
    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@registerForActivityResult
            }

            Thread {
                val result = importBios(uri)
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
                            "The selected file is not a valid PlayStation 2 BIOS.",
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

    private fun importBios(uri: Uri): File? = runCatching {
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
        if (temporary.length() !in MIN_BIOS_BYTES..MAX_BIOS_BYTES) {
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
            .remove("biosDir")
            .apply()

        // Keep a live frontend coherent if Android happened to retain it.
        if (MainActivityRuntime.instance != null) {
            MainActivityRuntime.bios.value = target.absolutePath
            MainActivityRuntime.biosDir.value = null
        }
        target
    }.getOrNull()

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
        private const val MIN_BIOS_BYTES = 4L * 1024L * 1024L
        private const val MAX_BIOS_BYTES = 8L * 1024L * 1024L
    }
}
