package com.ubertob.rest4sftp.model

import org.apache.commons.net.ftp.FTPFile
import java.io.InputStream

interface SimpleRemoteClient : AutoCloseable {

    fun listFiles(directoryName: String): List<FTPFile>?
    fun createFolder(directoryName: String): Boolean
    fun deleteFolder(directoryName: String): Boolean
    fun retrieveFile(directoryName: String, fileName: String): ByteArray?
    fun uploadFile(directoryName: String, fileName: String, upload: InputStream): Boolean
    fun deleteFile(directoryName: String, fileName: String): Boolean
    fun connect(): SimpleRemoteClient
    fun renameFile(directoryName: String, oldFileName: String, newFileName: String): Boolean
    fun isConnected(): Boolean
}
