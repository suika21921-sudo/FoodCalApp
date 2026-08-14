package com.mealoo.app.data

import android.content.Context
import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.mealoo.app.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import net.sqlcipher.database.SQLiteDatabase
import com.mealoo.app.util.SecurityHelper

enum class BackupCategory(val label: String, val tables: List<String>) {
    RECORDS("飲食與浪費紀錄", listOf("intake_entries", "waste_records")),
    RECIPES("食譜庫", listOf("recipes")),
    PLANNER("飲食計畫", listOf("plans", "meal_plan_entries")),
    SHOPPING("購物與省錢", listOf("shops", "shop_products", "savings_records", "shopping_replacements")),
    INVENTORY("我的食材", listOf("inventory_items", "foods"))
}

class BackupRepository(private val context: Context, private val drive: Drive) {

    private val BACKUP_FILE_NAME = "mealoo_backup.zip"
    private val DB_FILES = listOf("mealoo.db", "mealoo.db-wal", "mealoo.db-shm")

    class GoogleSyncException(message: String) : Exception(message)

    suspend fun backupDatabase(): String = withContext(Dispatchers.IO) {
        try {
            val dbVersion = AppDatabase.getInstance(context).openHelper.readableDatabase.version
            AppDatabase.forceCheckpoint()
            AppDatabase.safeClose()

            val zipFile = File(context.cacheDir, BACKUP_FILE_NAME)
            zipDatabaseFiles(zipFile)

            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }

            val appProperties = mapOf(
                "versionCode" to versionCode,
                "user_version" to dbVersion.toString(),
                "last_modified" to System.currentTimeMillis().toString()
            )

            val existingId = getExistingBackupFileId()
            if (existingId != null) {
                val existingFile = drive.files().get(existingId).setFields("md5Checksum, appProperties").execute()
                val localMd5 = calculateMd5(zipFile)
                if (existingFile.md5Checksum == localMd5) {
                    zipFile.delete()
                    return@withContext "云端已有最新版本，跳过上传。"
                }
            }

            val fileMetadata = DriveFile().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
                this.appProperties = appProperties
            }
            val mediaContent = FileContent("application/zip", zipFile)

            if (existingId != null) {
                drive.files().update(existingId, fileMetadata, mediaContent).execute()
            } else {
                drive.files().create(fileMetadata, mediaContent).execute()
            }

            zipFile.delete()

            return@withContext "备份成功！"
        } catch (e: Exception) {
            Log.e("BackupRepository", "Backup failed", e)
            throw GoogleSyncException("备份失败: ${e.message}")
        }
    }

    suspend fun backupSelectedDatabase(categories: Set<BackupCategory>): String = withContext(Dispatchers.IO) {
        try {
            // 1. Initialize SQLCipher native libraries (Required for low-level API)
            SQLiteDatabase.loadLibs(context)

            val dbVersion = AppDatabase.getInstance(context).openHelper.readableDatabase.version
            AppDatabase.forceCheckpoint()
            AppDatabase.safeClose()

            val mainDbFile = context.getDatabasePath("mealoo.db")
            val tmpDbFile = File(context.cacheDir, "tmp_mealoo.db")
            mainDbFile.copyTo(tmpDbFile, overwrite = true)
            val passphrase = SecurityHelper.getDatabasePassphrase(context.applicationContext).toString()

            val sqlDb = SQLiteDatabase.openDatabase(
                tmpDbFile.absolutePath,
                passphrase,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            // The following will now resolve correctly because sqlDb is properly typed
            val selectedTables = categories.flatMap { it.tables }.toSet()
            val allTables = BackupCategory.entries.flatMap { it.tables }.toSet()
            val tablesToDelete = allTables - selectedTables

            sqlDb.beginTransaction()
            try {
                tablesToDelete.forEach { table ->
                    sqlDb.execSQL("DELETE FROM $table")
                }
                sqlDb.setTransactionSuccessful()
            } finally {
                sqlDb.endTransaction()
            }

            sqlDb.execSQL("VACUUM")
            sqlDb.close()

            // 4. Zip ONLY the modified tmp db (we don't need wal/shm for a vacuumed snapshot)
            val zipFile = File(context.cacheDir, BACKUP_FILE_NAME)
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                FileInputStream(tmpDbFile).use { fis ->
                    BufferedInputStream(fis).use { bis ->
                        // Important: Zip entry name must be "mealoo.db" so it restores correctly
                        val entry = ZipEntry("mealoo.db")
                        zos.putNextEntry(entry)
                        bis.copyTo(zos, 1024)
                        zos.closeEntry()
                    }
                }
            }

            // Cleanup tmp db
            tmpDbFile.delete()

            // 5. Upload to Google Drive
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }

            val appProperties = mapOf(
                "versionCode" to versionCode,
                "user_version" to dbVersion.toString(),
                "last_modified" to System.currentTimeMillis().toString(),
                "is_partial" to "true"
            )

            val existingId = getExistingBackupFileId()
            val fileMetadata = DriveFile().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
                this.appProperties = appProperties
            }
            val mediaContent = FileContent("application/zip", zipFile)

            if (existingId != null) {
                drive.files().update(existingId, fileMetadata, mediaContent).execute()
            } else {
                drive.files().create(fileMetadata, mediaContent).execute()
            }

            zipFile.delete()

            return@withContext "部分資料备份成功！"
        } catch (e: Exception) {
            Log.e("BackupRepository", "Partial backup failed", e)
            throw GoogleSyncException("部分备份失败: ${e.message}")
        }
    }

    suspend fun restoreDatabase(): String = withContext(Dispatchers.IO) {
        try {
            val existingId = getExistingBackupFileId()
                ?: throw GoogleSyncException("云端未找到备份文件。")

            val driveFile = drive.files().get(existingId).setFields("id, appProperties").execute()
            val cloudVersion = driveFile.appProperties?.get("user_version")?.toIntOrNull() ?: 0
            
            val localVersion = AppDatabase.getInstance(context).openHelper.readableDatabase.version

            if (cloudVersion > localVersion) {
                throw GoogleSyncException("云端备份来自更新的 App 版本 (v$cloudVersion > v$localVersion)，请先更新 App。")
            }

            val tmpZip = File(context.cacheDir, "mealoo.db.tmp.zip")
            val outputStream = FileOutputStream(tmpZip)
            drive.files().get(existingId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            AppDatabase.forceCheckpoint()
            AppDatabase.safeClose()

            unzipToDatabase(tmpZip)
            tmpZip.delete()

            return@withContext "还原成功！"
        } catch (e: GoogleSyncException) {
            throw e
        } catch (e: Exception) {
            Log.e("BackupRepository", "Restore failed", e)
            throw GoogleSyncException("还原失败: ${e.message}")
        }
    }

    suspend fun getLastBackupMetadata(): Triple<String, String, String>? = withContext(Dispatchers.IO) {
        try {
            val existingId = getExistingBackupFileId() ?: return@withContext null
            val file = drive.files().get(existingId).setFields("appProperties").execute()
            val cloudTime = file.appProperties?.get("last_modified") ?: "0"
            val ver = file.appProperties?.get("user_version") ?: "未知"
            val localTime = context.getDatabasePath("mealoo.db").lastModified().toString()
            return@withContext Triple(cloudTime, ver, localTime)
        } catch (e: Exception) {
            null
        }
    }

    private fun getExistingBackupFileId(): String? {
        val query = "name = '$BACKUP_FILE_NAME' and 'appDataFolder' in parents and trashed = false"
        val existing = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ(query)
            .setFields("files(id)")
            .execute()
        return existing.files?.firstOrNull()?.id
    }

    private fun zipDatabaseFiles(zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            DB_FILES.forEach { fileName ->
                val dbFile = context.getDatabasePath(fileName)
                if (dbFile.exists()) {
                    FileInputStream(dbFile).use { fis ->
                        BufferedInputStream(fis).use { bis ->
                            val entry = ZipEntry(fileName)
                            zos.putNextEntry(entry)
                            bis.copyTo(zos, 1024)
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
    }

    private fun unzipToDatabase(zipFile: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (DB_FILES.contains(entry.name)) {
                    val dbFile = context.getDatabasePath(entry.name)
                    FileOutputStream(dbFile).use { fos ->
                        BufferedOutputStream(fos).use { bos ->
                            zis.copyTo(bos, 1024)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun calculateMd5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read = fis.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = fis.read(buffer)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
