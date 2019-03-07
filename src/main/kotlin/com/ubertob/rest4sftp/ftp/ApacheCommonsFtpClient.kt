package com.ubertob.rest4sftp.ftp

import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import java.io.InputStream
import java.io.PrintWriter
import java.time.Duration

class ApacheCommonsFtpClient(private val remoteHost: RemoteHost, val timeout: Duration, val tempExtension: String = ".io") : SimpleRemoteClient {

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

    override fun listFiles(directoryName: String): List<FTPFile> =
        if (ftp.changeWorkingDirectory(directoryName))
            ftp.listFiles().toList()
        else emptyList()

    override fun createFolder(directoryName: String): Boolean = ftp.makeDirectory(directoryName)

    override fun deleteFolder(directoryName: String): Boolean = ftp.removeDirectory(directoryName)

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray =
        if (ftp.changeWorkingDirectory(directoryName))
            ftp.retrieveFileStream(fileName)?.use {
                it.readBytes()
            } ?: ByteArray(0)
        else ByteArray(0)

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean =
            if (ftp.changeWorkingDirectory(directoryName)
                    && ftp.storeFile(fileName.tempExt(), upload)) {
                    ftp.status
                    ftp.rename(fileName.tempExt(), fileName)
            } else { false }


    override fun renameFile(directoryName: String, oldFileName: String, newFileName: String): Boolean =
            ftp.rename("$directoryName/$oldFileName", "$directoryName/$newFileName")

    private fun String.tempExt() = this + tempExtension

    override fun deleteFile(directoryName: String, fileName: String): Boolean =
        ftp.changeWorkingDirectory(directoryName)
        && ftp.deleteFile(fileName)

    private fun <R> check(errorMsg: String, block: () -> R): R {
        try {
            val result = block()

            if (! FTPReply.isPositiveCompletion(ftp.replyCode)) {
                System.err.println(errorMsg)
                throw RuntimeException(errorMsg)
            }
            return result
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

}