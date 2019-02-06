package org.rest4sftp.ftp

import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.apache.commons.net.ftp.FTPFile
import org.rest4sftp.model.RemoteHost
import org.rest4sftp.model.SimpleRemoteClient
import java.io.InputStream

class SshJSftpClient(val remoteHost: RemoteHost): SimpleRemoteClient {

    val sshClient = SSHClient(DefaultConfig())

    override fun listFiles(directoryName: String): List<FTPFile> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createFolder(directoryName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteFolder(directoryName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retrieveFile(directoryName: String, fileName: String): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteFile(directoryName: String, fileName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun connect(): SimpleRemoteClient {
        sshClient.addHostKeyVerifier(PromiscuousVerifier())
        sshClient.connect(remoteHost.host, remoteHost.port)
        return this
    }

    override fun close() {
        sshClient.close()
    }

}
