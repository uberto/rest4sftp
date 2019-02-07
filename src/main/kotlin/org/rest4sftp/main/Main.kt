package org.rest4sftp.main

import com.xenomachina.argparser.ArgParser
import org.rest4sftp.ftp.ApacheCommonsFtpClient
import org.rest4sftp.ftp.SshJSftpClient
import org.rest4sftp.http.RestfulServer
import org.rest4sftp.model.CommandHandler
import java.time.Duration


fun main(args: Array<String>) {
    ArgParser(args).parseInto(::MyArgs).run {

        when(protocol){

            Protocol.FTP -> RestfulServer(CommandHandler { ApacheCommonsFtpClient(it, Duration.ofSeconds(timeout)) }).start(port)

            Protocol.SFTP -> RestfulServer(CommandHandler { SshJSftpClient(it, Duration.ofSeconds(timeout)) }).start(port)

        }
    }
}