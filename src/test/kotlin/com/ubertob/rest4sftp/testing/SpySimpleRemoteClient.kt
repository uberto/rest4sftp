package com.ubertob.rest4sftp.testing

import com.ubertob.rest4sftp.http.UnauthorisedException
import com.ubertob.rest4sftp.model.*
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.InputStream
import java.time.Instant

class SpySimpleRemoteClient(
    private val remoteHost: RemoteHost,
    private val files: MutableMap<String, MutableList<FileSystemElement>>,
    private val filesContent: MutableMap<String, ByteArray>
) : SimpleRemoteClient {

    override fun deleteFolder(folderPath: String): Boolean {
        assertTrue(connected)
        return files.remove(folderPath)?.let { true } ?: false
    }

    override fun createFolder(folderPath: String): Boolean {
        assertTrue(connected)
        return files.putIfAbsent(folderPath, mutableListOf())?.let { false } ?: true
    }

    var connected: Boolean = false
    override fun isConnected(): Boolean = connected

    override fun listFiles(folderPath: String, filter: Filter): List<FileSystemElement>? {
        assertTrue(connected)
        return files[folderPath]?.filter { filter.accept(it) }
    }

    override fun retrieveFile(folderPath: String, fileName: String): ByteArray? {
        assertTrue(connected)
        return filesContent["$folderPath/$fileName"]
    }

    override fun uploadFile(folderPath: String, fileName: String, upload: InputStream): Boolean {
        assertTrue(connected)


        return if (!files.containsKey(folderPath))
             false
        else {
            val bytesContent = upload.readBytes()
            val ftpFile = FileInfo(fileName, Instant.now(), bytesContent.size.toLong(), folderPath)
            files[folderPath]?.add(ftpFile)

            filesContent["$folderPath/$fileName"] = bytesContent
            true
        }
    }

    override fun renameFile(folderPath: String, oldFileName: String, newFileName: String): Boolean {
        assertTrue(connected)

        files[folderPath]?.filter { it.name == oldFileName }
            ?.filterIsInstance<FileInfo>()
            ?.map{  it.copy(name = newFileName) }

        return files[folderPath]?.any { it.name == newFileName } ?: false
    }

    override fun deleteFile(folderPath: String, fileName: String): Boolean {
        assertTrue(connected)
        filesContent.remove("$folderPath/$fileName")
        return files[folderPath]?.removeIf { it.name == fileName } ?: false
    }

    override fun connect(): SimpleRemoteClient =
        if (remoteHost.password.contains("bad"))
            throw UnauthorisedException(message = "invalid password")
        else {
            connected = true
            this
        }

    override fun close() {
        connected = false
    }


}