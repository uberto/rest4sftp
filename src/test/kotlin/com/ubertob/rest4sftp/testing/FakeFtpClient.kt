package com.ubertob.rest4sftp.testing

import com.ubertob.rest4sftp.http.UnauthorisedException
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import org.apache.commons.net.ftp.FTPFile
import java.io.InputStream

class FakeFtpClient(
        private val remoteHost: RemoteHost,
        private val directories: MutableList<String>,
        private val files: MutableMap<String, ByteArray>
) : SimpleRemoteClient {

    var connected = false

    override fun listFiles(directoryName: String): List<FTPFile>? =
            if (directories.contains(directoryName.withLeadingSlash())) {
                files.map {
                    FTPFile().apply { name = it.key }
                }
            } else null

    override fun createFolder(directoryName: String): Boolean = directories.add(directoryName)

    override fun deleteFolder(directoryName: String): Boolean = directories.remove(directoryName)

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray? =
            if (directories.contains(directoryName.withLeadingSlash())) {
                files.filter { it.key == fileName }
                        .map { it.value }
                        .firstOrNull()
            } else null

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean {
        if (!directories.contains(directoryName.withLeadingSlash())) {
            return false
        }
        files[fileName] = upload.readBytes()
        return true
    }

    override fun deleteFile(directoryName: String, fileName: String): Boolean {
        files.remove(fileName)
        return true
    }

    override fun connect(): SimpleRemoteClient =
            if (remoteHost.password.contains("bad"))
                throw UnauthorisedException(message = "invalid password")
            else {
                connected = true
                this
            }

    override fun renameFile(directoryName: String, oldFileName: String, newFileName: String): Boolean =
            retrieveFile(directoryName, oldFileName)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        deleteFile(directoryName, oldFileName)
                        uploadFile(directoryName, newFileName, it.inputStream())
                    }
                    ?: false

    override fun close() {
        connected = false
    }

    override fun isConnected(): Boolean = connected

    private fun String.withLeadingSlash() = if (startsWith('/')) this else "/$this"
}