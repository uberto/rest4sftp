package org.rest4sftp.ftp

import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.apache.commons.net.ftp.FTPFile
import org.rest4sftp.model.RemoteHost
import org.rest4sftp.model.SimpleRemoteClient
import java.io.InputStream



class SshJSftpClient(val remoteHost: RemoteHost): SimpleRemoteClient {

    val sshClient = SSHClient(DefaultConfig())


    fun toFtpFile(rrinfo: RemoteResourceInfo): FTPFile =
        FTPFile().apply {
//            setPermission(rrinfo.attributes)
            name = rrinfo.name
        }

    override fun listFiles(directoryName: String): List<FTPFile> =
        sshClient.newSFTPClient().use {
            sftpClient -> sftpClient.ls(directoryName).map ( ::toFtpFile )
        }

    override fun createFolder(directoryName: String): Boolean {
        sshClient.newSFTPClient().use {
            sftpClient -> sftpClient.mkdir(directoryName)
        }
        return true
    }

    override fun deleteFolder(directoryName: String): Boolean {
        sshClient.newSFTPClient().use {
            sftpClient -> sftpClient.rmdir(directoryName)
        }
        return true
    }

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray {
//        sFtpServer.get()rmdir(directoryName)
        TODO()

    }

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteFile(directoryName: String, fileName: String): Boolean {
        sshClient.newSFTPClient().use {
            sftpClient -> sftpClient.rm(directoryName + "/" + fileName)
        }
        return true
    }

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
