package integrationtest.org.rest4sftp.ftp

import org.apache.commons.net.ftp.FTPFile
import org.rest4sftp.model.RemoteHost
import org.rest4sftp.model.SimpleRemoteClient
import java.io.InputStream

class SshJSftpClient(remoteHost: RemoteHost): SimpleRemoteClient {
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
