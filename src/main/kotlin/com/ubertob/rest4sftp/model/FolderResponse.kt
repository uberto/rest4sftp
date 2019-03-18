package com.ubertob.rest4sftp.model

import org.apache.commons.net.ftp.FTPFile


class FolderResponse(val folders: List<FolderItem>,
                     val files: List<FileItem>)

class FolderItem(val name: String)

class FileItem(val name: String)

fun Iterable<FTPFile>.toFolderResponse(): FolderResponse {
    val folders = this.filter{it.isDirectory }.map { FolderItem(it.name) }
    val files = this.filter{it.isFile }.map { FileItem(it.name) }
    return FolderResponse(folders, files)
}