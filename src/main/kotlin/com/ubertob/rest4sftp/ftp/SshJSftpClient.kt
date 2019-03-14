package com.ubertob.rest4sftp.ftp

import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import org.apache.commons.net.ftp.FTPFile
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import net.schmizz.sshj.sftp.FileMode
import org.apache.commons.net.ftp.FTPFile.FILE_TYPE
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import java.time.Duration


class SshJSftpClient(val remoteHost: RemoteHost, val timeout: Duration): SimpleRemoteClient {
    val sshClient = SSHClient(DefaultConfig())


    fun toFtpFile(rrinfo: RemoteResourceInfo): FTPFile =
        FTPFile().apply {
            size = rrinfo.attributes.size
            this.type = rrinfo.attributes.type.toTypeInt()
//            setPermission(rrinfo.attributes.permissions)
            name = rrinfo.name

        }

    override fun listFiles(directoryName: String): List<FTPFile> =

        runCatching {
            sshClient.newSFTPClient().use {
            sftpClient -> sftpClient.ls(directoryName).map ( ::toFtpFile )
        }}.getOrDefault(emptyList())

    override fun createFolder(directoryName: String): Boolean =
        runWithNoExceptions {
            sshClient.newSFTPClient().use {
            sftpClient -> sftpClient.mkdir(directoryName)
            } }

    override fun deleteFolder(directoryName: String): Boolean =
        runWithNoExceptions {
            sshClient.newSFTPClient().use { sftpClient ->
                sftpClient.rmdir(directoryName)
            } }

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray =
        runCatching {
            InMemoryOutputFile().also {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.get(directoryName slash fileName, it)
                }
            }.outputStream.toByteArray()
        }.getOrDefault(ByteArray(0))

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean =
        runWithNoExceptions {
            val sourceFile = InMemoryInputFile(upload)
            val tempFileName = "$fileName.io"
            sshClient.newSFTPClient().use {
                sftpClient -> sftpClient.put(sourceFile, directoryName slash tempFileName)
                sftpClient.rename(directoryName slash tempFileName, directoryName slash fileName)
                }
            }

    override fun renameFile(directoryName: String, oldFileName: String, newFileName: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.rename(directoryName slash oldFileName, directoryName slash newFileName)
                }
            }


    override fun deleteFile(directoryName: String, fileName: String): Boolean =
         runWithNoExceptions {
             sshClient.newSFTPClient().use { sftpClient ->
                 sftpClient.rm(directoryName slash fileName)
             }
         }

    private fun runWithNoExceptions(block: () -> Unit): Boolean =
        runCatching(block).isSuccess


    override fun connect(): SimpleRemoteClient {

        sshClient.addHostKeyVerifier(PromiscuousVerifier())
        sshClient.connect(remoteHost.host, remoteHost.port)
        sshClient.authPassword(remoteHost.userName, remoteHost.password)
        return this
    }

    override fun close() {
        sshClient.close()
    }

}

private fun FileMode.Type.toTypeInt(): Int =
    when(this){
        FileMode.Type.BLOCK_SPECIAL -> FTPFile.UNKNOWN_TYPE
        FileMode.Type.CHAR_SPECIAL -> FTPFile.UNKNOWN_TYPE
        FileMode.Type.FIFO_SPECIAL -> FTPFile.UNKNOWN_TYPE
        FileMode.Type.SOCKET_SPECIAL -> FTPFile.UNKNOWN_TYPE
        FileMode.Type.REGULAR -> FTPFile.FILE_TYPE
        FileMode.Type.DIRECTORY -> FTPFile.DIRECTORY_TYPE
        FileMode.Type.SYMLINK -> FTPFile.SYMBOLIC_LINK_TYPE
        FileMode.Type.UNKNOWN -> FTPFile.UNKNOWN_TYPE
    }


private infix fun String.slash(fileName: String): String = Paths.get(this, fileName).toString()


class InMemoryOutputFile(): InMemoryDestFile(){
    val outputStream = ByteArrayOutputStream()

    override fun getOutputStream(): OutputStream = outputStream
}

class InMemoryInputFile(val uploadStream: InputStream) : InMemorySourceFile(){
    override fun getLength(): Long = 0

    override fun getName(): String = "MemoryFile"

    override fun getInputStream(): InputStream = uploadStream
}

