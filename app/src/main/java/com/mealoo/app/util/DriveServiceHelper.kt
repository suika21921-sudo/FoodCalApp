package com.mealoo.app.data

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class DriveServiceHelper(private val driveService: Drive) {

    /**
     * 在 appDataFolder 中搜尋指定檔名的檔案 ID
     */
    suspend fun searchFileId(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setQ("name = '$fileName' and 'appDataFolder' in parents and trashed = false")
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()

            result.files?.firstOrNull()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 讀取檔案內容為字串
     */
    suspend fun readFileContent(fileId: String): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = driveService.files().get(fileId).executeMediaAsInputStream()
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 建立或更新檔案 (在 appDataFolder)
     */
    suspend fun createOrUpdateFile(fileName: String, content: String) = withContext(Dispatchers.IO) {
        try {
            val fileId = searchFileId(fileName)
            val mediaContent = ByteArrayContent("application/json", content.toByteArray())

            if (fileId == null) {
                // 建立新檔案
                val fileMetadata = File().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            } else {
                // 更新現有檔案
                val fileMetadata = File().apply {
                    name = fileName
                }
                driveService.files().update(fileId, fileMetadata, mediaContent)
                    .execute()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}