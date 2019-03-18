package com.ubertob.rest4sftp.http

import com.ubertob.rest4sftp.model.CommandHandler
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.ubertob.rest4sftp.model.FolderResponse
import com.ubertob.rest4sftp.model.toFolderResponse
import org.apache.commons.net.ftp.FTPFile
import org.http4k.core.Body
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import com.ubertob.rest4sftp.testing.SpySimpleRemoteClient
import org.apache.commons.net.ftp.FTPFile.DIRECTORY_TYPE
import org.apache.commons.net.ftp.FTPFile.FILE_TYPE

class RestfulServerTest {

    private val file1 = FTPFile().apply { name = "file1"; size = 123; type = FILE_TYPE }
    private val file2 = FTPFile().apply { name = "file2"; size = 234; type = FILE_TYPE }
    private val dir1 = FTPFile().apply { name = "subFolder"; size = 0; type = DIRECTORY_TYPE }
    private val files = mutableMapOf("folder1" to mutableListOf(file1, file2, dir1))

    private lateinit var fakeFtpClient: SpySimpleRemoteClient
    private val handler = RestfulServer(CommandHandler {
        fakeFtpClient = SpySimpleRemoteClient(it, files)
        fakeFtpClient
    })

    private val connectionHeaders = listOf(
        RestfulServer.HOST_HEADER to "server",
        RestfulServer.PORT_HEADER to "22",
        RestfulServer.USER_HEADER to "user",
        RestfulServer.PWD_HEADER to "pwd"
    )

    @Test
    fun `map files to folder response`() {
        val expectedJson = ObjectMapper().writeValueAsString(files["folder1"]?.toFolderResponse())

        assertThat(expectedJson).isEqualTo("""{"folders":[{"name":"subFolder"}],"files":[{"name":"file1"},{"name":"file2"}]}""")
    }


    @Test
    fun `retrieve list of all files in dir`() {
        val expectedJson = ObjectMapper().writeValueAsString(files["folder1"]?.toFolderResponse())
        val req = Request(GET, "/folder/folder1").headers(connectionHeaders)

        val response = handler(req)
        assertThat(response).all {
            hasStatus(OK)
            hasBody(expectedJson)
        }

        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `non existent folder returns empty list`() {
        val expectedJson = ObjectMapper().writeValueAsString(FolderResponse(emptyList(), emptyList()))
        val req = Request(GET, "/folder/folder3").headers(connectionHeaders)

        val response = handler(req)
        assertThat(response).all {
            hasStatus(OK)
            hasBody(expectedJson)
        }
        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `can create a directory`() {
        val folderPath = "folder2"
        val createDirectory = Request(PUT, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(OK)
            hasBody("created folder: $folderPath")
        }

        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `can't create a directory that exists`() {
        val folderPath = "folder1"
        val createDirectory = Request(PUT, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(NOT_FOUND)
            hasBody("impossible to create folder: $folderPath")
        }
        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `delete a directory`() {
        val folderPath = "folder1"
        val createDirectory = Request(DELETE, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(OK)
            hasBody("deleted folder: $folderPath")
        }
        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `can't delete a directory that doesn't exist`() {
        val folderPath = "folder2"
        val createDirectory = Request(DELETE, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(NOT_FOUND)
            hasBody("impossible to delete folder: $folderPath")
        }
        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `retrieve the content of a file`() {
        val req = Request(GET, "/file/folder1/file1").headers(connectionHeaders)
        val response = handler(req)


        assertThat(response).all {
            hasStatus(OK)
            hasBody(file1.name)
        }
        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `delete a file`() {
        val filePath = "folder1/file1"
        val delete = Request(DELETE, "/file/$filePath").headers(connectionHeaders)

        assertThat(handler(delete)).all {
            hasStatus(OK)
            hasBody("deleted: $filePath")
        }
        assertFalse(fakeFtpClient.isConnected)
    }


    @Test
    fun `can't delete a file that doesn't exist`() {
        val filePath = "folder1/file5"
        val delete = Request(DELETE, "/file/$filePath").headers(connectionHeaders)

        assertThat(handler(delete)).all {
            hasStatus(NOT_FOUND)
            hasBody("impossible to delete: $filePath")
        }

        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `upload a file`() {
        val filePath = "folder1/file1"
        val upload = Request(PUT, "/file/$filePath").headers(connectionHeaders).body(Body("test".byteInputStream()))

        assertThat(handler(upload)).all {
            hasStatus(OK)
            hasBody("uploaded: $filePath")
        }
        assertFalse(fakeFtpClient.isConnected)
    }

    @Test
    fun `can't upload a file to a directory that doesn't exist`() {
        val filePath = "folder10/file1"
        val upload = Request(PUT, "/file/$filePath").headers(connectionHeaders).body(Body("test".byteInputStream()))

        assertThat(handler(upload)).all {
            hasStatus(BAD_REQUEST)
            hasBody("could not upload: $filePath")
        }
        assertFalse(fakeFtpClient.isConnected)
    }

    fun Assert<Response>.hasStatus(expected: Status) {
        transform { assertThat(it.status).isEqualTo(expected) }
    }

    fun Assert<Response>.hasBody(expected: String) {
        transform { assertThat(it.bodyString()).isEqualTo(expected) }
    }
}

