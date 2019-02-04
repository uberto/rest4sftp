package org.rest4sftp.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import java.io.InputStream

typealias SimpleFtpClientFactory = (FtpHost) -> SimpleFtpClient

sealed class Command

data class RetrieveFolder(val path: String) : Command()
data class DeleteFolder(val path: String) : Command()
data class CreateFolder(val path: String) : Command()
data class DeleteFile(val path: String, val fileName: String) : Command()
data class RetrieveFile(val path: String, val fileName: String) : Command()
data class UploadFile(val path: String, val fileName: String, val inputStream: InputStream) : Command()

class CommandHandler(internal val ftpClientFactory: SimpleFtpClientFactory) {

    private val jsonMapper = ObjectMapper()

    fun handle(ftpHost: FtpHost, cmd: Command): HttpResult = when (cmd) {
        is DeleteFile -> {
            if (ftpHost.execute { deleteFile(cmd.path, cmd.fileName) })
                HttpResult(OK, StringResponseBody("deleted: ${cmd.path}/${cmd.fileName}"))
            else
                HttpResult(NOT_FOUND, StringResponseBody("impossible to delete: ${cmd.path}/${cmd.fileName}"))
        }
        is UploadFile -> {
            if (ftpHost.execute { uploadFile(cmd.path, cmd.fileName, cmd.inputStream) })
                HttpResult(OK, StringResponseBody("uploaded: ${cmd.path}/${cmd.fileName}"))
            else
                HttpResult(BAD_REQUEST, StringResponseBody("could not upload: ${cmd.path}/${cmd.fileName}"))
        }
        is RetrieveFile -> {
            val retrieveFile = ftpHost.execute { retrieveFile(cmd.path, cmd.fileName) }
            HttpResult(OK, InputStreamResponseBody(retrieveFile.inputStream()))
        }
        is RetrieveFolder -> {
            val listFiles = ftpHost.execute { listFiles(cmd.path) }
            val json = jsonMapper.writeValueAsString(listFiles)
            HttpResult(OK, JsonResponseBody(json))
        }
        is DeleteFolder -> {
            if (ftpHost.execute { deleteFolder(cmd.path) })
                HttpResult(OK, StringResponseBody("deleted folder: ${cmd.path}"))
            else
                HttpResult(NOT_FOUND, StringResponseBody("impossible to delete folder: ${cmd.path}"))
        }
        is CreateFolder -> {
            if (ftpHost.execute { createFolder(cmd.path) })
                HttpResult(OK, StringResponseBody("created folder: ${cmd.path}"))
            else
                HttpResult(NOT_FOUND, StringResponseBody("impossible to create folder: ${cmd.path}"))
        }
    }

    private fun <T> FtpHost.execute(block: SimpleFtpClient.() -> T): T = ftpClientFactory(this).connect().use(block)

}
