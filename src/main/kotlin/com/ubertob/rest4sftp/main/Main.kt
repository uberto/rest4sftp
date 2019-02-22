package com.ubertob.rest4sftp.main

import com.xenomachina.argparser.ArgParser
import com.ubertob.rest4sftp.ftp.ApacheCommonsFtpClient
import com.ubertob.rest4sftp.ftp.SshJSftpClient
import com.ubertob.rest4sftp.http.RestfulServer
import com.ubertob.rest4sftp.model.CommandHandler
import java.time.Duration


fun main(args: Array<String>) {
    ArgParser(args).parseInto(::MyArgs).run {

        RestfulServer(CommandHandler {
            when (protocol) {
                Protocol.FTP -> ApacheCommonsFtpClient(it, Duration.ofSeconds(timeout))
                Protocol.SFTP -> SshJSftpClient(it, Duration.ofSeconds(timeout))
            }
        })
        .also { println("Starting RestfulServer in $protocol mode on port $port...") }
        .start(port)
        .block()
    }
}