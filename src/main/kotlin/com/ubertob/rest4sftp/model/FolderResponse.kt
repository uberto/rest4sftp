package com.ubertob.rest4sftp.model



class FolderResponse(val folders: List<FolderInfo>,
                     val files: List<FileInfo>)


fun Iterable<FileSystemElement>.toFolderResponse(): FolderResponse =
    FolderResponse(
        this.filterIsInstance<FolderInfo>() ,
        this.filterIsInstance<FileInfo>()
    )