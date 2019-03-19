package com.ubertob.rest4sftp.testing

import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import org.apache.commons.net.ftp.FTPFile
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.InputStream

class SpySimpleRemoteClient(val remoteHost: RemoteHost, val files: MutableMap<String, MutableList<FTPFile>>) : SimpleRemoteClient {
    override fun deleteFolder(directoryName: String): Boolean {
        assertTrue(isConnected)
        return files.remove(directoryName)?.let { true } ?: false
    }

    override fun createFolder(directoryName: String): Boolean {
        assertTrue(isConnected)
        return files.putIfAbsent(directoryName, mutableListOf())?.let { false } ?: true
    }

    var isConnected: Boolean = false

    override fun listFiles(directoryName: String): List<FTPFile>? {
        assertTrue(isConnected)
        return files[directoryName]
    }

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray {
        assertTrue(isConnected)
        return files.getOrDefault(directoryName, emptyList<FTPFile>()).first { it.name == fileName }.name.toByteArray()
    }

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean {
        assertTrue(isConnected)

        val ftpFile = FTPFile().apply { name = fileName; size = upload.readBytes().size.toLong() }

        return files[directoryName]?.add(ftpFile) ?: false
    }

    override fun renameFile(directoryName: String, oldFileName: String, newFileName: String): Boolean {
        assertTrue(isConnected)

        files[directoryName]?.filter { it.name == oldFileName }?.map{

            FTPFile().apply { name = newFileName; size = it.size }}
        return files[directoryName]?.any { it.name == newFileName } ?: false
    }

    override fun deleteFile(directoryName: String, fileName: String): Boolean {
        assertTrue(isConnected)
        return files[directoryName]?.removeIf { it.name == fileName } ?: false
    }

    override fun connect(): SimpleRemoteClient {
        isConnected = true
        return this
    }

    override fun close() {
        isConnected = false
    }


}