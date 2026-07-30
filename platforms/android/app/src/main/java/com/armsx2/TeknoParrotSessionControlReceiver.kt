package com.armsx2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import com.armsx2.runtime.MainActivityRuntime
import kr.co.iefriends.pcsx2.NativeApp
import java.io.File

/**
 * Process-independent control endpoint used by TeknoParrotUi's foreground
 * session owner. MainActivityRuntime validates the callback package and the
 * unguessable per-session token before reporting or stopping a game.
 */
class TeknoParrotSessionControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_QUERY_CATALOG -> sendReadyCatalog(context, intent)
            ACTION_QUERY_BIOS -> sendBiosStatus(context, intent)
            else -> MainActivityRuntime.handleTeknoParrotSessionControl(context, intent)
        }
        // A health query can cold-start the package after Android killed or the
        // user force-stopped the emulator. Once the terminal reply is queued,
        // do not retain an otherwise empty ~80 MB frontend process. Recheck
        // after a short grace period so a simultaneous real Activity launch is
        // never terminated.
        if (!hasActiveUi()) {
            val pendingResult = goAsync()
            Handler(Looper.getMainLooper()).postDelayed({
                pendingResult.finish()
                if (!hasActiveUi())
                    Process.killProcess(Process.myPid())
            }, 250L)
        }
    }

    private fun hasActiveUi(): Boolean =
        MainActivityRuntime.instance != null || TeknoParrotBiosImportActivity.isActive()

    private fun sendBiosStatus(context: Context, request: Intent) {
        val callbackPackage = validatedCallbackPackage(request) ?: return
        val token = validatedToken(request) ?: return
        val configured = hasValidConfiguredBios(context)

        context.applicationContext.sendBroadcast(
            Intent(ACTION_BIOS_STATUS)
                .setPackage(callbackPackage)
                .putExtra(EXTRA_SESSION_TOKEN, token)
                .putExtra(EXTRA_BIOS_READY, configured)
        )
        println("@@TPUI_BIOS_STATUS@@ ready=$configured")
    }

    /**
     * Do not trust a non-empty preference alone. A restored Android backup can
     * leave it pointing at a missing file, and users can accidentally select a
     * non-BIOS image. Re-run PCSX2's native ROMVER parser for conventional PS2
     * BIOS images. System 246/256 uses a paired 2 MiB ROM set that is not
     * individually parseable by ROMVER, so recognize only the exact complete
     * pair already used by the companion runtime.
     */
    private fun hasValidConfiguredBios(context: Context): Boolean = runCatching {
        val preferences =
            context.applicationContext.getSharedPreferences("ARMSX2", Context.MODE_PRIVATE)
        val biosRoot =
            MainActivityRuntime.internalBiosDir(context.applicationContext).canonicalFile
        val path = preferences.getString("bios", null)?.takeIf(String::isNotBlank)
        if (path != null) {
            val canonicalFile = File(path).canonicalFile
            if (canonicalFile.isFile &&
                canonicalFile.parentFile == biosRoot &&
                TeknoParrotBiosFiles.hasStandardBiosSize(canonicalFile)
            ) {
                val descriptor = ParcelFileDescriptor.open(
                    canonicalFile,
                    ParcelFileDescriptor.MODE_READ_ONLY,
                )
                if (NativeApp.getBiosInfoFromFd(descriptor.detachFd()) != null)
                    return@runCatching true
            }
        }

        val splitSet =
            TeknoParrotBiosFiles.findSystem246SplitSet(biosRoot)
                ?: return@runCatching false
        // Repair stale/missing preferences after an app update or Android
        // backup restore. Both files are already inside this app's private
        // external-files directory and have passed the exact pair checks.
        preferences.edit()
            .putString("bios", splitSet.primary.absolutePath)
            .putString("biosDir", biosRoot.absolutePath)
            .apply()
        true
    }.getOrDefault(false)

    private fun sendReadyCatalog(context: Context, request: Intent) {
        val callbackPackage = validatedCallbackPackage(request) ?: return
        val token = validatedToken(request) ?: return
        val gameIds = findReadyGameIds(context)

        context.applicationContext.sendBroadcast(
            Intent(ACTION_CATALOG_STATUS)
                .setPackage(callbackPackage)
                .putExtra(EXTRA_SESSION_TOKEN, token)
                .putStringArrayListExtra(EXTRA_GAME_IDS, ArrayList(gameIds))
        )
        println("@@TPUI_CATALOG@@ ready=${gameIds.size}")
    }

    private fun validatedCallbackPackage(request: Intent): String? =
        request.getStringExtra(EXTRA_CALLBACK_PACKAGE)
            ?.takeIf { it == TPUI_PACKAGE }

    private fun validatedToken(request: Intent): String? =
        request.getStringExtra(EXTRA_SESSION_TOKEN)
            ?.takeIf {
                it.length in 32..128 &&
                    it.all { character ->
                        character.isLetterOrDigit() || character == '-' || character == '_'
                    }
            }

    private fun findReadyGameIds(context: Context): List<String> {
        val externalRoot = context.getExternalFilesDir(null) ?: return emptyList()
        val gameRoot = File(externalRoot, "TeknoParrot/games")
        val manifests =
            gameRoot.listFiles { file ->
                file.isFile && file.name.endsWith(".acgame", ignoreCase = true)
            } ?: return emptyList()

        return manifests.mapNotNull { manifest ->
            val values =
                parseManifest(manifest)
                    ?: run {
                        println("@@TPUI_CATALOG_SKIP@@ file=${manifest.name} reason=parse")
                        return@mapNotNull null
                    }
            val gameId = values["game.gameid"]
                ?.takeIf { GAME_ID.matches(it) }
                ?: run {
                    println(
                        "@@TPUI_CATALOG_SKIP@@ file=${manifest.name} " +
                            "reason=gameid value=${values["game.gameid"]}"
                    )
                    return@mapNotNull null
                }
            val subdir = values["data.subdir"]
                ?.takeIf { SAFE_NAME.matches(it) }
                ?: run {
                    println(
                        "@@TPUI_CATALOG_SKIP@@ file=${manifest.name} " +
                            "reason=subdir value=${values["data.subdir"]}"
                    )
                    return@mapNotNull null
                }
            val requiredNames =
                listOf("elf", "dongle", "mediasrc").map { key ->
                    values["data.$key"]
                        ?.takeIf { SAFE_NAME.matches(it) }
                        ?: run {
                            println(
                                "@@TPUI_CATALOG_SKIP@@ file=${manifest.name} " +
                                    "reason=$key value=${values["data.$key"]}"
                            )
                            return@mapNotNull null
                        }
                }
            val dataDirectory = File(gameRoot, subdir)
            val requiredFiles =
                requiredNames.map { File(dataDirectory, it) } +
                    File(dataDirectory, "sram.bin")
            val missing = requiredFiles.firstOrNull { !it.isFile || it.length() <= 0L }
            if (missing != null) {
                println(
                    "@@TPUI_CATALOG_SKIP@@ file=${manifest.name} " +
                        "reason=required path=${missing.absolutePath} " +
                        "exists=${missing.isFile} size=${missing.length()}"
                )
                return@mapNotNull null
            }

            gameId
        }.distinct().sorted()
    }

    private fun parseManifest(manifest: File): Map<String, String>? =
        runCatching {
            var section = ""
            buildMap {
                manifest.useLines { lines ->
                    lines.forEach { rawLine ->
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
        }.getOrNull()

    companion object {
        private const val TPUI_PACKAGE = "com.teknoparrot.ui"
        private const val ACTION_QUERY_CATALOG =
            "com.teknoparrot.pcsx2x6.action.QUERY_CATALOG"
        private const val ACTION_CATALOG_STATUS =
            "com.teknoparrot.pcsx2x6.action.CATALOG_STATUS"
        private const val ACTION_QUERY_BIOS =
            "com.teknoparrot.pcsx2x6.action.QUERY_BIOS"
        private const val ACTION_BIOS_STATUS =
            "com.teknoparrot.pcsx2x6.action.BIOS_STATUS"
        private const val EXTRA_CALLBACK_PACKAGE =
            "com.teknoparrot.pcsx2x6.extra.CALLBACK_PACKAGE"
        private const val EXTRA_SESSION_TOKEN =
            "com.teknoparrot.pcsx2x6.extra.SESSION_TOKEN"
        private const val EXTRA_GAME_IDS =
            "com.teknoparrot.pcsx2x6.extra.GAME_IDS"
        private const val EXTRA_BIOS_READY =
            "com.teknoparrot.pcsx2x6.extra.BIOS_READY"
        private val GAME_ID = Regex("""NM\d{5}(?:_[A-Za-z0-9]+)?""")
        private val SAFE_NAME = Regex("""[A-Za-z0-9_. -]+""")
    }
}
