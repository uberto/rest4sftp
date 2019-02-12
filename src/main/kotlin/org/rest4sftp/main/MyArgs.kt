package org.rest4sftp.main

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.rest4sftp.main.Protocol.FTP
import org.rest4sftp.main.Protocol.SFTP

class MyArgs(parser: ArgParser) {
    val v by parser.flagging("enable verbose mode")

    val protocol by parser.mapping(
            "--ftp" to FTP,
            "--sftp" to SFTP,
            help = "protocol of remote server").default(SFTP)

    val timeout: Long by parser.storing("timeout in seconds for remote server"){toLong()}.default(60)

    val port: Int by parser.storing("port for restapi"){toInt()}.default(8080)
}

enum class Protocol {
    FTP, SFTP
}
