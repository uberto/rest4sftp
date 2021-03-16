package com.ubertob.rest4sftp.model

class Filter(private val pattern: String?) {

    fun accept(fileInfo: FileSystemElement): Boolean =
        pattern?.let {
            it
                .replace(Regex("[+.(){}|^$\\[\\]\\\\]")) { "\\${it.value}" }
                .replace("?", ".")
                .replace("*", ".*")
                .toRegex()
                .matches(fileInfo.name)
        } ?: true
}