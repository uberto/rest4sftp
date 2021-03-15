package com.ubertob.rest4sftp.ftp

import com.ubertob.rest4sftp.http.UnauthorisedException
import com.ubertob.rest4sftp.model.*
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.logging.Logger

class SshJSftpClient(private val remoteHost: RemoteHost, private val timeout: Duration) : SimpleRemoteClient {
    private val sshClient = SSHClient(DefaultConfig())


    private fun toFtpFile(rrinfo: RemoteResourceInfo, folderPath: String): FileSystemElement =
        if (rrinfo.attributes.type == FileMode.Type.REGULAR) {
            FileInfo(rrinfo.name, Instant.ofEpochSecond(rrinfo.attributes.atime), rrinfo.attributes.size, folderPath)
        } else {
            FolderInfo(rrinfo.name, Instant.ofEpochSecond(rrinfo.attributes.atime), folderPath)
        }

    override fun listFiles(folderPath: String): List<FileSystemElement>? =
            runCatching {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.ls(folderPath).map{toFtpFile(it, folderPath)}
                }
            }.getOrNull()
                    .also {
                        logger.info("GET -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/ <- ${it?.size
                                ?: "null"} items")
                    }

    override fun listFiles(folderPath: String, filter: Filter): List<FileSystemElement>? =
        runCatching {
            sshClient.newSFTPClient().use { sftpClient ->
                sftpClient.ls(folderPath) { resource -> filter.accept(toFtpFile(resource, folderPath)) }.map{toFtpFile(it, folderPath)}
            }
        }.getOrNull()
            .also {
                logger.info("GET -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/ <- ${it?.size
                    ?: "null"} items")
            }

    override fun createFolder(folderPath: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.mkdir(folderPath)
                }
            }.also {
                logger.info("CREATE -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/ <- $it")
            }

    override fun deleteFolder(folderPath: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.rmdir(folderPath)
                }
            }.also {
                logger.info("DELETE -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/ <- $it")
            }

    override fun retrieveFile(folderPath: String, fileName: String): ByteArray? =
            runCatching {
                InMemoryOutputFile().also {
                    sshClient.newSFTPClient().use { sftpClient ->
                        sftpClient.get(folderPath slash fileName, it)
                    }
                }.outputStream.toByteArray()
            }.getOrNull()
                    .also {
                        logger.info("GET -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/$fileName <- ${it?.size
                                ?: "null"} bytes")
                    }

    override fun uploadFile(folderPath: String, fileName: String, upload: InputStream): Boolean =
            runWithNoExceptions {
                val sourceFile = InMemoryInputFile(upload)
                val tempFileName = "$fileName.io"
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.put(sourceFile, folderPath slash tempFileName)
                    sftpClient.statExistence(folderPath slash fileName)?.let {
                        sftpClient.rm(folderPath slash fileName)
                    }
                    sftpClient.rename(folderPath slash tempFileName, folderPath slash fileName)
                }
            }.also {
                logger.info("UPLOAD -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/$fileName <- $it")
            }

    override fun renameFile(folderPath: String, oldFileName: String, newFileName: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.rename(folderPath slash oldFileName, folderPath slash newFileName)
                }
            }.also {
                logger.info("RENAME -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/$oldFileName to $newFileName <- $it")
            }


    override fun deleteFile(folderPath: String, fileName: String): Boolean =
            runWithNoExceptions {
                sshClient.newSFTPClient().use { sftpClient ->
                    sftpClient.rm(folderPath slash fileName)
                }
            }.also {
                logger.info("DELETE -> ${remoteHost.userName}@${remoteHost.host}:${remoteHost.port}/$folderPath/$fileName <- $it")
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

    override fun isConnected(): Boolean =
        sshClient.isConnected

    override fun close() {
        sshClient.close()
    }

    companion object {
        val logger: Logger = Logger.getLogger(CommandHandler::class.java.name)
    }
}


private infix fun String.slash(fileName: String): String = Paths.get(this, fileName).toString()


class InMemoryOutputFile : InMemoryDestFile() {
    val outputStream = ByteArrayOutputStream()

    override fun getOutputStream(): OutputStream = outputStream
}

class InMemoryInputFile(private val uploadStream: InputStream) : InMemorySourceFile() {
    override fun getLength(): Long = 0

    override fun getName(): String = "MemoryFile"

    override fun getInputStream(): InputStream = uploadStream
}

