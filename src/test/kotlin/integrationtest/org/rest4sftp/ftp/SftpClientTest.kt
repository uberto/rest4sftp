package integrationtest.org.rest4sftp.ftp

import org.junit.jupiter.api.Disabled
import org.rest4sftp.model.RemoteHost
import org.rest4sftp.model.SimpleRemoteClient


@Disabled
class SftpClientTest: RemoteClientTest() {

    val ftpHost = RemoteHost(host = "127.0.0.1", port = 2222, userName = "foo", password = "pass")

    override fun createConnection(): SimpleRemoteClient = SshJSftpClient(ftpHost).connect()

}