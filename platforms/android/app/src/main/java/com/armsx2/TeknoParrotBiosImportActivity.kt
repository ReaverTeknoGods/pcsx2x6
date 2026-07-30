package com.armsx2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.armsx2.runtime.MainActivityRuntime
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicInteger

/**
 * Single-purpose, signature-protected BIOS picker used by TeknoParrotUI.
 * It intentionally exposes none of PCSX2X6's standalone library UI.
 */
class TeknoParrotBiosImportActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var selectButton: Button
    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isEmpty()) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@registerForActivityResult
            }

            status.text = "Validating both arcade BIOS chips…"
            selectButton.isEnabled = false
            Thread {
                val result = importSystem246SplitSet(uris)
                runOnUiThread {
                    if (result != null) {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_BIOS_NAME, result.name),
                        )
                        finish()
                    } else {
                        status.text = BIOS_INSTRUCTION
                        selectButton.isEnabled = true
                    }
                }
            }.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        activeInstances.incrementAndGet()
        super.onCreate(savedInstanceState)
        status = TextView(this).apply {
            text = BIOS_INSTRUCTION
            textSize = 18f
            setPadding(24, 16, 24, 20)
        }
        selectButton = Button(this).apply {
            text = "Select both BIOS files"
            setOnClickListener { openPicker() }
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                addView(status)
                addView(selectButton)
            },
        )
        if (savedInstanceState == null) {
            openPicker()
        }
    }

    override fun onDestroy() {
        activeInstances.decrementAndGet()
        super.onDestroy()
    }

    private fun openPicker() {
        status.text = BIOS_INSTRUCTION
        picker.launch(arrayOf("*/*"))
    }

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

    companion object {
        /**
         * The system document picker covers this activity while the BIOS files
         * are selected. Session health queries must not treat the companion
         * process as idle and kill it while that result is still pending.
         */
        private val activeInstances = AtomicInteger()

        internal fun isActive(): Boolean = activeInstances.get() > 0

        private const val EXTRA_BIOS_NAME =
            "com.teknoparrot.pcsx2x6.extra.BIOS_NAME"
        private val BIOS_INSTRUCTION =
            "System 246/256 requires both arcade BIOS files.\n\n" +
                "Select these two files together:\n" +
                "• ${TeknoParrotBiosFiles.SYSTEM_246_PRIMARY_NAME}\n" +
                "• ${TeknoParrotBiosFiles.SYSTEM_246_SECONDARY_NAME}"
    }
}
