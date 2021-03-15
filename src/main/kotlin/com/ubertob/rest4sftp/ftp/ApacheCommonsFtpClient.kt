package com.ubertob.rest4sftp.ftp

import com.ubertob.rest4sftp.http.UnauthorisedException
import com.ubertob.rest4sftp.model.*
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream
import java.io.PrintWriter
import java.time.Duration

class ApacheCommonsFtpClient(private val remoteHost: RemoteHost, private val timeout: Duration, private val tempExtension: String = ".io") : SimpleRemoteClient {

    private val ftp = FTPClient()

    fun withDebug() = apply {
        ftp.addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
    }

    override fun connect() = apply {
        ftp.setDataTimeout(timeout.toMillis().toInt())
        check("FTP server refused connection.") { ftp.connect(remoteHost.host, remoteHost.port) }
        check("Invalid login.") { ftp.login(remoteHost.userName, remoteHost.password) }
        check("FTP server not able to passive mode.") { ftp.enterLocalPassiveMode() }
    }

    override fun close() {
        if (ftp.isConnected) {
            runCatching {
                ftp.logout()
                ftp.disconnect()
            }
        }
    }

    override fun isConnected(): Boolean =
        ftp.isConnected

    override fun listFiles(folderPath: String): List<FileSystemElement>? =
        if (ftp.changeWorkingDirectory(folderPath))
            ftp.listFiles().toFileSystemElements(folderPath)
        else null

    override fun listFiles(folderPath: String, filter: Filter): List<FileSystemElement>? =
        if (ftp.changeWorkingDirectory(folderPath))
            ftp.listFiles(null) { resource -> resource.toFileSystemElement(folderPath)?.let { filter.accept(it) } ?: false } .toFileSystemElements(folderPath)
        else null

    override fun createFolder(folderPath: String): Boolean = ftp.makeDirectory(folderPath)

    override fun deleteFolder(folderPath: String): Boolean = ftp.removeDirectory(folderPath)

    override fun retrieveFile(folderPath: String, fileName: String): ByteArray? =
        if (ftp.changeWorkingDirectory(folderPath))
            ftp.retrieveFileStream(fileName)?.use {
                it.readBytes()
            }
        else null

    override fun uploadFile(folderPath: String, fileName: String, upload: InputStream): Boolean =
            if (ftp.changeWorkingDirectory(folderPath)
                    && ftp.storeFile(fileName.tempExt(), upload)) {
                    ftp.status
                    ftp.rename(fileName.tempExt(), fileName)
            } else { false }


    override fun renameFile(folderPath: String, oldFileName: String, newFileName: String): Boolean =
            ftp.rename("$folderPath/$oldFileName", "$folderPath/$newFileName")

    private fun String.tempExt() = this + tempExtension

    override fun deleteFile(folderPath: String, fileName: String): Boolean =
        ftp.changeWorkingDirectory(folderPath)
        && ftp.deleteFile(fileName)

    private fun <R> check(errorMsg: String, block: () -> R): R {
        try {
            val result = block()

            if (! FTPReply.isPositiveCompletion(ftp.replyCode)) {
                System.err.println(errorMsg)
                throw UnauthorisedException(message = errorMsg)
            }
            return result
        } catch (ex: Throwable) {
            throw UnauthorisedException(cause = ex)
        }
    }

}

private fun Array<FTPFile>.toFileSystemElements(folderPath: String): List<FileSystemElement> =
    mapNotNull { ftpFile -> ftpFile.toFileSystemElement(folderPath) }

private fun FTPFile.toFileSystemElement(folderPath: String): FileSystemElement? =
    when {
        isFile -> FileInfo(name, timestamp.toInstant(), size, folderPath)
        isDirectory -> FolderInfo(name, timestamp.toInstant(), folderPath)
        else -> null
    }
