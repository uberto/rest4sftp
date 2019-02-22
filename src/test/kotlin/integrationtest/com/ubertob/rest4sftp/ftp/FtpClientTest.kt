package integrationtest.com.ubertob.rest4sftp.ftp

import com.ubertob.rest4sftp.ftp.ApacheCommonsFtpClient
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import java.time.Duration


class FtpClientTest: RemoteClientTest() {

    val ftpHost = RemoteHost(host = "127.0.0.1", port = 21, userName = "bob", password = "12345")

    override fun createConnection(): SimpleRemoteClient = ApacheCommonsFtpClient(ftpHost, Duration.ofSeconds(2)).withDebug().connect()

}