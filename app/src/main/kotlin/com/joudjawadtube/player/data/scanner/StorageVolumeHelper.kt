package com.joudjawadtube.player.data.scanner

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat
import java.io.File

data class MountedVolumeInfo(
    val id: String,
    val label: String,
    val path: String,
    val isUsbOrRemovable: Boolean,
    val isBootOrSystemPartition: Boolean = false
)

object StorageVolumeHelper {

    fun getMountedVolumes(context: Context, hideBootPartitions: Boolean = true): List<MountedVolumeInfo> {
        val result = linkedMapOf<String, MountedVolumeInfo>()

        // 1. Primary Internal Storage
        val primaryDir = Environment.getExternalStorageDirectory()
        if (primaryDir != null && primaryDir.exists()) {
            val canonical = runCatching { primaryDir.canonicalPath }.getOrDefault(primaryDir.absolutePath)
            result[canonical] = MountedVolumeInfo(
                id = "primary",
                label = "Internal Storage",
                path = canonical,
                isUsbOrRemovable = false
            )
        }

        // 2. StorageManager volumes (API 24+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            sm?.storageVolumes?.forEach { vol ->
                val state = vol.state
                if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                    val dir: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        vol.directory
                    } else {
                        runCatching {
                            val getPath = vol.javaClass.getMethod("getPathFile")
                            getPath.invoke(vol) as? File
                        }.getOrNull()
                    }
                    if (dir != null && dir.exists()) {
                        val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
                        val isRemovable = vol.isRemovable && !vol.isPrimary
                        val desc = vol.getDescription(context)?.takeIf { it.isNotBlank() }
                            ?: if (isRemovable) "USB Drive (${dir.name})" else "Internal Storage"
                        val isBoot = isLikelyBootPartition(dir, desc)
                        result[canonical] = MountedVolumeInfo(
                            id = vol.uuid ?: dir.name,
                            label = if (isRemovable && !desc.contains("USB", ignoreCase = true)) {
                                "USB: $desc"
                            } else {
                                desc
                            },
                            path = canonical,
                            isUsbOrRemovable = isRemovable,
                            isBootOrSystemPartition = isBoot
                        )
                    }
                }
            }
        }

        // 3. Fallback via getExternalFilesDirs for API 23 and custom OEM mounts
        val appDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (appDir in appDirs) {
            if (appDir == null) continue
            val rootPath = appDir.absolutePath.substringBefore("/Android/")
            val rootFile = File(rootPath)
            if (rootFile.exists() && rootFile.isDirectory) {
                val canonical = runCatching { rootFile.canonicalPath }.getOrDefault(rootFile.absolutePath)
                if (!result.containsKey(canonical)) {
                    val isRemovable = try {
                        Environment.isExternalStorageRemovable(rootFile)
                    } catch (_: Exception) {
                        !canonical.contains("emulated")
                    }
                    val label = if (isRemovable) "USB Drive (${rootFile.name})" else "Internal Storage"
                    result[canonical] = MountedVolumeInfo(
                        id = rootFile.name,
                        label = label,
                        path = canonical,
                        isUsbOrRemovable = isRemovable,
                        isBootOrSystemPartition = isLikelyBootPartition(rootFile, label)
                    )
                }
            }
        }

        // 4. Inspect /storage and /mnt/media_rw for Android TV direct USB OTG mounts
        listOf("/storage", "/mnt/media_rw", "/mnt/usb_storage", "/mnt/usb").forEach { mountParent ->
            val parent = File(mountParent)
            val children = parent.listFiles() ?: return@forEach
            for (child in children) {
                val name = child.name
                if (name.equals("emulated", ignoreCase = true) ||
                    name.equals("self", ignoreCase = true) ||
                    name.equals("knox-emulated", ignoreCase = true) ||
                    name.equals("enc_emulated", ignoreCase = true)
                ) {
                    continue
                }
                if (child.isDirectory && child.canRead()) {
                    val canonical = runCatching { child.canonicalPath }.getOrDefault(child.absolutePath)
                    if (!result.containsKey(canonical)) {
                        val label = "USB Drive ($name)"
                        result[canonical] = MountedVolumeInfo(
                            id = name,
                            label = label,
                            path = canonical,
                            isUsbOrRemovable = true,
                            isBootOrSystemPartition = isLikelyBootPartition(child, label)
                        )
                    }
                }
            }
        }

        return result.values.filter { !hideBootPartitions || !it.isBootOrSystemPartition }
    }

    private fun isLikelyBootPartition(rootDir: File, label: String): Boolean {
        if (label.contains("VTOYEFI", ignoreCase = true) || label.equals("EFI", ignoreCase = true)) {
            return true
        }
        val hasMokCert = File(rootDir, "ENROLL_THIS_KEY_IN_MOKMANAGER.cer").exists()
        val hasGrubAndVentoy = File(rootDir, "grub").isDirectory && File(rootDir, "ventoy").isDirectory
        return hasMokCert || hasGrubAndVentoy
    }

    fun classifyFilePath(filePath: String, volumes: List<MountedVolumeInfo>): MountedVolumeInfo {
        val matched = volumes
            .sortedByDescending { it.path.length }
            .firstOrNull { filePath.startsWith(it.path, ignoreCase = true) }
        if (matched != null) return matched

        val isUsbHeuristic = !filePath.contains("/emulated/", ignoreCase = true) &&
            (filePath.startsWith("/storage/") || filePath.startsWith("/mnt/"))
        val seg = filePath.trimStart('/').split('/').getOrNull(1) ?: "Storage"
        return MountedVolumeInfo(
            id = seg,
            label = if (isUsbHeuristic) "USB Drive ($seg)" else "Internal Storage",
            path = "/storage/$seg",
            isUsbOrRemovable = isUsbHeuristic
        )
    }
}
