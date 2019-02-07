package integrationtest.org.rest4sftp.ftp

import org.rest4sftp.ftp.SshJSftpClient
import org.rest4sftp.model.RemoteHost
import org.rest4sftp.model.SimpleRemoteClient
import java.time.Duration


class SftpClientTest: RemoteClientTest() {

    val ftpHost = RemoteHost(host = "127.0.0.1", port = 2222, userName = "bob", password = "12345")

    override fun createConnection(): SimpleRemoteClient = SshJSftpClient(ftpHost, Duration.ofSeconds(5)).connect()

}