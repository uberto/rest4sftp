package org.rest4sftp.main

import com.xenomachina.argparser.ArgParser

class MyArgs(parser: ArgParser) {
    val v by parser.flagging("enable verbose mode")

    val protocol by parser.mapping(
            "--ftp" to Protocol.FTP,
            "--sftp" to Protocol.SFTP,
            help = "protocol of remote server")

    val timeout: Long by parser.storing("timeout in seconds"){toLong()}

    val port: Int by parser.storing("port "){toInt()}
}

enum class Protocol {
    FTP, SFTP
}
