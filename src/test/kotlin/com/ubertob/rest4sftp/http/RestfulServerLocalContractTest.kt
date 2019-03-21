package com.ubertob.rest4sftp.http

import com.ubertob.rest4sftp.model.SimpleFtpClientFactory
import com.ubertob.rest4sftp.testing.SpySimpleRemoteClient
import org.apache.commons.net.ftp.FTPFile

class RestfulServerLocalContractTest : RestfulServerContract() {

    private val files = mutableMapOf("upload" to mutableListOf<FTPFile>())

    private val contents = mutableMapOf<String, ByteArray>()

    override val ftpHost: String = "fake"
    override val ftpPort: Int = 21
    override val ftpUser: String = "fake"
    override val ftpPassword: String = "fake"

    override val ftpClientFactory: SimpleFtpClientFactory = { remoteHost -> SpySimpleRemoteClient(remoteHost, files, contents) }
}