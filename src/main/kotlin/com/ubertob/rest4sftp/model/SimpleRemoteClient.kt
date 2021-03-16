package com.ubertob.rest4sftp.model

import java.io.InputStream

interface SimpleRemoteClient : AutoCloseable {

    fun listFiles(folderPath: String, filter: Filter): List<FileSystemElement>?
    fun createFolder(folderPath: String): Boolean
    fun deleteFolder(folderPath: String): Boolean
    fun retrieveFile(folderPath: String, fileName: String): ByteArray?
    fun uploadFile(folderPath: String, fileName: String, upload: InputStream): Boolean
    fun deleteFile(folderPath: String, fileName: String): Boolean
    fun connect(): SimpleRemoteClient
    fun renameFile(folderPath: String, oldFileName: String, newFileName: String): Boolean
    fun isConnected(): Boolean
}