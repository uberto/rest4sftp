package com.ubertob.rest4sftp.model

class Filter(private val pattern: Regex?)  {

    fun accept(fileInfo: FileSystemElement): Boolean =
        pattern?.let { pattern.containsMatchIn(fileInfo.name) } ?: true
}
