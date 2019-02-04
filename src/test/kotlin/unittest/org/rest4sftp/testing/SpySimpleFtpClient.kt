package unittest.org.rest4sftp.testing

import org.rest4sftp.model.FtpHost
import org.rest4sftp.model.SimpleFtpClient
import org.apache.commons.net.ftp.FTPFile
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.InputStream

class SpySimpleFtpClient(val ftpHost: FtpHost, val files: MutableMap<String, MutableList<FTPFile>>) : SimpleFtpClient {
    override fun deleteFolder(directoryName: String): Boolean {
        assertTrue(isConnected)
        return files.remove(directoryName)?.let { true } ?: false
    }

    override fun createFolder(directoryName: String): Boolean {
        assertTrue(isConnected)
        return files.putIfAbsent(directoryName, mutableListOf())?.let { false } ?: true
    }

    var isConnected: Boolean = false

    override fun listFiles(directoryName: String): List<FTPFile> {
        assertTrue(isConnected)
        return files[directoryName] ?: emptyList()
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

    override fun deleteFile(directoryName: String, fileName: String): Boolean {
        assertTrue(isConnected)
        return files[directoryName]?.removeIf { it.name == fileName } ?: false
    }

    override fun connect(): SimpleFtpClient {
        isConnected = true
        return this
    }

    override fun close() {
        isConnected = false
    }


}