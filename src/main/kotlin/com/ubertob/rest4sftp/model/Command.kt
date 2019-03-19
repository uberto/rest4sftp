package com.ubertob.rest4sftp.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import java.io.InputStream

typealias SimpleFtpClientFactory = (RemoteHost) -> SimpleRemoteClient

sealed class Command

data class RetrieveFolder(val path: String) : Command()
data class DeleteFolder(val path: String) : Command()
data class CreateFolder(val path: String) : Command()
data class DeleteFile(val path: String, val fileName: String) : Command()
data class RetrieveFile(val path: String, val fileName: String) : Command()
data class UploadFile(val path: String, val fileName: String, val inputStream: InputStream) : Command()

class CommandHandler(internal val ftpClientFactory: SimpleFtpClientFactory) {

    private val jsonMapper = ObjectMapper()

    fun handle(remoteHost: RemoteHost, cmd: Command): HttpResult = when (cmd) {
        is DeleteFile -> {
            if (remoteHost.execute { deleteFile(cmd.path, cmd.fileName) })
                HttpResult(OK, StringResponseBody("deleted: ${cmd.path}/${cmd.fileName}"))
            else
                HttpResult(NOT_FOUND, StringResponseBody("impossible to delete: ${cmd.path}/${cmd.fileName}"))
        }
        is UploadFile -> {
            if (remoteHost.execute { uploadFile(cmd.path, cmd.fileName, cmd.inputStream) })
                HttpResult(OK, StringResponseBody("uploaded: ${cmd.path}/${cmd.fileName}"))
            else
                HttpResult(BAD_REQUEST, StringResponseBody("could not upload: ${cmd.path}/${cmd.fileName}"))
        }
        is RetrieveFile -> {
            remoteHost.execute { retrieveFile(cmd.path, cmd.fileName) }
                    ?.let { HttpResult(OK, InputStreamResponseBody(it.inputStream())) }
                    ?: HttpResult(NOT_FOUND, StringResponseBody(""))
        }
        is RetrieveFolder -> {
            remoteHost.execute { listFiles(cmd.path) }?.let { listFiles ->
                val json = jsonMapper.writeValueAsString(listFiles.toFolderResponse())
                HttpResult(OK, JsonResponseBody(json))
            } ?: HttpResult(NOT_FOUND, StringResponseBody(""))
        }
        is DeleteFolder -> {
            if (remoteHost.execute { deleteFolder(cmd.path) })
                HttpResult(OK, StringResponseBody("deleted folder: ${cmd.path}"))
            else
                HttpResult(NOT_FOUND, StringResponseBody("impossible to delete folder: ${cmd.path}"))
        }
        is CreateFolder -> {
            if (remoteHost.execute { createFolder(cmd.path) })
                HttpResult(OK, StringResponseBody("created folder: ${cmd.path}"))
            else
                HttpResult(NOT_FOUND, StringResponseBody("impossible to create folder: ${cmd.path}"))
        }
    }

    private fun <T> RemoteHost.execute(block: SimpleRemoteClient.() -> T): T = ftpClientFactory(this).connect().use(block)

}

