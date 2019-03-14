package integrationtest.com.ubertob.rest4sftp.ftp

import com.ubertob.rest4sftp.ftp.SshJSftpClient
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import java.time.Duration


class SftpClientTest: RemoteClientTest() {

    val sftpHost = RemoteHost(host = "127.0.0.1", port = 2222, userName = "bob", password = "12345")

    override fun createConnection(): SimpleRemoteClient = SshJSftpClient(sftpHost, Duration.ofSeconds(5)).connect()

}