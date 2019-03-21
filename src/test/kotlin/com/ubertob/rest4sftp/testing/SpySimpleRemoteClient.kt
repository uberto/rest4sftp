package com.ubertob.rest4sftp.testing

import com.ubertob.rest4sftp.http.UnauthorisedException
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import org.apache.commons.net.ftp.FTPFile
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.InputStream

class SpySimpleRemoteClient(val remoteHost: RemoteHost, val files: MutableMap<String, MutableList<FTPFile>>, val filesContent: MutableMap<String, ByteArray>) : SimpleRemoteClient {

    override fun deleteFolder(directoryName: String): Boolean {
        assertTrue(connected)
        return files.remove(directoryName)?.let { true } ?: false
    }

    override fun createFolder(directoryName: String): Boolean {
        assertTrue(connected)
        return files.putIfAbsent(directoryName, mutableListOf())?.let { false } ?: true
    }

    var connected: Boolean = false
    override fun isConnected(): Boolean = connected

    override fun listFiles(directoryName: String): List<FTPFile>? {
        assertTrue(connected)
        return files[directoryName]
    }

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray? {
        assertTrue(connected)
        return filesContent["$directoryName/$fileName"]
    }

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean {
        assertTrue(connected)


        return if (!files.containsKey(directoryName))
             false
        else {
            val bytesContent = upload.readBytes()
            val ftpFile = FTPFile().apply { name = fileName; size = bytesContent.size.toLong() }
            files[directoryName]?.add(ftpFile)

            filesContent["$directoryName/$fileName"] = bytesContent
            true
        }
    }

    override fun renameFile(directoryName: String, oldFileName: String, newFileName: String): Boolean {
        assertTrue(connected)

        files[directoryName]?.filter { it.name == oldFileName }?.map{

            FTPFile().apply { name = newFileName; size = it.size }}
        return files[directoryName]?.any { it.name == newFileName } ?: false
    }

    override fun deleteFile(directoryName: String, fileName: String): Boolean {
        assertTrue(connected)
        filesContent.remove("$directoryName/$fileName")
        return files[directoryName]?.removeIf { it.name == fileName } ?: false
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