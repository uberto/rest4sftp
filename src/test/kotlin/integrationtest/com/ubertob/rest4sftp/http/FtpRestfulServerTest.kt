package integrationtest.com.ubertob.rest4sftp.http

import com.ubertob.rest4sftp.ftp.ApacheCommonsFtpClient
import com.ubertob.rest4sftp.http.RestfulServerContract
import com.ubertob.rest4sftp.model.SimpleFtpClientFactory
import java.time.Duration

class FtpRestfulServerTest : RestfulServerContract() {

    override val ftpHost: String = "127.0.0.1"
    override val ftpPort: Int = 21
    override val ftpUser: String = "bob"
    override val ftpPassword: String = "12345"

    override val ftpClientFactory: SimpleFtpClientFactory = {
        ApacheCommonsFtpClient(it, Duration.ofSeconds(2)).withDebug()
    }
}