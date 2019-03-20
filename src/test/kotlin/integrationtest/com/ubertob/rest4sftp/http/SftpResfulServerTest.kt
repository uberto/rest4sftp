package integrationtest.com.ubertob.rest4sftp.http

import com.ubertob.rest4sftp.ftp.SshJSftpClient
import com.ubertob.rest4sftp.http.RestfulServerContract
import com.ubertob.rest4sftp.model.SimpleFtpClientFactory
import java.time.Duration

class SftpResfulServerTest : RestfulServerContract() {

    override val ftpHost: String = "127.0.0.1"
    override val ftpPort: Int = 2222
    override val ftpUser: String = "bob"
    override val ftpPassword: String = "12345"

    override val ftpClientFactory: SimpleFtpClientFactory = {
        SshJSftpClient(it, Duration.ofSeconds(5))
    }
}