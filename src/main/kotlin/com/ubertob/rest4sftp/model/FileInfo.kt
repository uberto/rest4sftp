package com.ubertob.rest4sftp.model

import java.time.Instant


interface FileMetaInfo{
    val name: String
    val date: Instant
}

sealed class FileSystemElement: FileMetaInfo

data class FileInfo(override val name: String, override val date: Instant, val size: Long, val folderPath: String): FileSystemElement()

data class FolderInfo(override val name: String, override val date: Instant, val fullFolderPath: String): FileSystemElement()