package com.ubertob.rest4sftp.model

import org.apache.commons.net.ftp.FTPFile


class FolderResponse(val folders: List<String>,
                     val files: List<String>)

fun Iterable<FTPFile>.toFolderResponse(): FolderResponse {
    val folders = this.filter{it.isDirectory }.map { it.name }
    val files = this.filter{it.isFile }.map { it.name }
    return FolderResponse(folders, files)
}