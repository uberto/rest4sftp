package com.ubertob.rest4sftp.http

import com.ubertob.rest4sftp.model.SimpleFtpClientFactory
import com.ubertob.rest4sftp.testing.FakeFtpClient

class SimpleRestfulServerTest : RestfulServerContract() {

    private val directories = mutableListOf("/upload")
    private val files = mutableMapOf<String, ByteArray>()


    override val ftpHost: String = "fake"
    override val ftpPort: Int = 21
    override val ftpUser: String = "fake"
    override val ftpPassword: String = "fake"

    override val ftpClientFactory: SimpleFtpClientFactory = { remoteHost -> FakeFtpClient(remoteHost, directories, files) }
}