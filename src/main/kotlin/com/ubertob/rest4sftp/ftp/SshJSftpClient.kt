package com.ubertob.rest4sftp.ftp

import com.ubertob.rest4sftp.http.UnauthorisedException
import com.ubertob.rest4sftp.model.CommandHandler
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import org.apache.commons.net.ftp.FTPFile
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import java.time.Duration
import java.util.logging.Logger


class SshJSftpClient(private val remoteHost: RemoteHost, private val timeout: Duration) : SimpleRemoteClient {
    private val sshClient = SSHClient(DefaultConfig())


    fun toFtpFile(rrinfo: RemoteResourceInfo): FTPFile =
            FTPFile().apply {
                size = rrinfo.attributes.size
                this.type = rrinfo.attributes.type.toTypeInt()
//            setPermission(rrinfo.attributes.permissions)
                name = rrinfo.name

            }

    override fun listFiles(directoryName: String): List<FTPFile>? =

            runCatching {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.ls(directoryName).map(::toFtpFile)
                }
            }.getOrNull()
                    .also {
                        logger.info("GET -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$directoryName/ <- ${it?.size
                                ?: "null"} items")
                    }

    override fun createFolder(directoryName: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.mkdir(directoryName)
                }
            }.also {
                logger.info("CREATE -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$directoryName/ <- $it")
            }

    override fun deleteFolder(directoryName: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.rmdir(directoryName)
                }
            }.also {
                logger.info("DELETE -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$directoryName/ <- $it")
            }

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray? =
            runCatching {
                InMemoryOutputFile().also {
                    sshClient.newSFTPClient().use { sftpClient ->
                        sftpClient.get(directoryName slash fileName, it)
                    }
                }.outputStream.toByteArray()
            }.getOrNull()
                    .also {
                        logger.info("GET -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$directoryName/$fileName <- ${it?.size
                                ?: "null"} bytes")
                    }

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean =
            runWithNoExceptions {
                val sourceFile = InMemoryInputFile(upload)
                val tempFileName = "$fileName.io"
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.put(sourceFile, directoryName slash tempFileName)
                    sftpClient.statExistence(directoryName slash fileName)?.let {
                        sftpClient.rm(directoryName slash fileName)
                    }
                    sftpClient.rename(directoryName slash tempFileName, directoryName slash fileName)
                }
            }.also {
                logger.info("UPLOAD -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$directoryName/$fileName <- $it")
            }

    override fun renameFile(directoryName: String, oldFileName: String, newFileName: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.rename(directoryName slash oldFileName, directoryName slash newFileName)
                }
            }.also {
                logger.info("RENAME -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$directoryName/$oldFileName to $newFileName <- $it")
            }


    override fun deleteFile(directoryName: String, fileName: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.rm(directoryName slash fileName)
                }
            }.also {
                logger.info("DELETE -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$directoryName/$fileName <- $it")
            }

    private fun runWithNoExceptions(block: () -> Unit): Boolean =
            runCatching(block).isSuccess


    override fun connect(): SimpleRemoteClient {

        try {
            logger.info("CONNECT -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}")
            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connect(remoteHost.host, remoteHost.port)
            sshClient.authPassword(remoteHost.userName, remoteHost.password)
        } catch (ex: UserAuthException) {
            throw UnauthorisedException(cause = ex)
        }
        return this
    }

    override fun close() {
        sshClient.close()
    }

    companion object {
        val logger: Logger = Logger.getLogger(CommandHandler::class.java.name)
    }
}

private fun FileMode.Type.toTypeInt(): Int =
        when (this) {
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


class InMemoryOutputFile() : InMemoryDestFile() {
    val outputStream = ByteArrayOutputStream()

    override fun getOutputStream(): OutputStream = outputStream
}

class InMemoryInputFile(val uploadStream: InputStream) : InMemorySourceFile() {
    override fun getLength(): Long = 0

    override fun getName(): String = "MemoryFile"

    override fun getInputStream(): InputStream = uploadStream
}

