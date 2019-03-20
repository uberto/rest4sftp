package com.ubertob.rest4sftp.http

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.fail
import com.ubertob.rest4sftp.model.CommandHandler
import com.ubertob.rest4sftp.model.SimpleFtpClientFactory
import org.apache.http.HttpHeaders
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

abstract class RestfulServerContract {

    protected abstract val ftpClientFactory: SimpleFtpClientFactory

    protected abstract val ftpHost: String
    protected abstract val ftpPort: Int
    protected abstract val ftpUser: String
    protected abstract val ftpPassword: String

    private fun Request.withAuthentication() =
        headers(listOf(
            "FTP-Host" to ftpHost,
            "FTP-Port" to ftpPort.toString(),
            "FTP-User" to ftpUser,
            "FTP-Password" to ftpPassword
        ))

    private val service: RestfulServer = RestfulServer(CommandHandler { ftpClientFactory(it) })

    @Test
    fun `returns unauthorised if authorisation headers not present`() {
        val response = service.invoke(Request(Method.GET, "/folder/upload"))
        assertThat(response.status).isEqualTo(Status.UNAUTHORIZED)
    }

    @Test
    fun `returns unauthorised if invalid credentials supplied`() {
        val request = Request(Method.GET, "/folder/upload").withAuthentication().replaceHeader("FTP-Password", "bad password")
        val response = service.invoke(request)
        assertThat(response.status).isEqualTo(Status.UNAUTHORIZED)
    }

    @Test
    fun `returns file not found for invalid path`() {
        val request = Request(Method.GET, "/file/upload/unknown_file.txt").withAuthentication()
        val response = service.invoke(request)
        assertThat(response.status).isEqualTo(Status.NOT_FOUND)
    }

    @Test
    fun `returns file not found if given a folder path`() {
        val request = Request(Method.GET, "/file/upload").withAuthentication()
        val response = service.invoke(request)
        assertThat(response.status).isEqualTo(Status.NOT_FOUND)
    }

    @Test
    fun `returns folder not found if folder does not exist on server`() {
        val request = Request(Method.GET, "/folder/upload/unknown_subfolder").withAuthentication()
        val response = service.invoke(request)
        assertThat(response.status).isEqualTo(Status.NOT_FOUND)
    }

    @Test
    fun `returns valid folder`() {
        val request = Request(Method.GET, "/folder/upload").withAuthentication()
        val response = service.invoke(request)
        assertThat(response.status).isEqualTo(Status.OK)
        assertThat(response.contentType).isEqualTo(ContentType.APPLICATION_JSON.value)
    }

    @Test
    fun `can upload a new file, retrieve it, update it, and delete it`() {

        val uploadFilePath = "/upload/uploaded-file-${LocalDateTime.now()}.txt"
        val testFileContents = "this is an uploaded file"
        val updatedFileContents = "this is a revised uploaded file"

        service.invoke(Request(Method.GET, "/file$uploadFilePath").withAuthentication())
            .handleResponse {
                assertThat(it.status).isEqualTo(Status.NOT_FOUND)
            }

        service.invoke(Request(Method.PUT, "/file$uploadFilePath").body(testFileContents).withAuthentication()).handleResponse {
            assertThat(it.status).isEqualTo(Status.OK)
        }

        service.invoke(Request(Method.GET, "/file$uploadFilePath").withAuthentication())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
                assertThat(response.contentType).isEqualTo(OCTET_STREAM.value)
                assertThat(response.bodyString()).isEqualTo(testFileContents)
            }

        service.invoke(Request(Method.PUT, "/file$uploadFilePath").body(updatedFileContents).withAuthentication())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
            }

        service.invoke(Request(Method.GET, "/file$uploadFilePath").withAuthentication())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
                assertThat(response.contentType).isEqualTo(OCTET_STREAM.value)
                assertThat(response.bodyString()).isEqualTo(updatedFileContents)
            }

        service.invoke(Request(Method.DELETE, "/file$uploadFilePath").withAuthentication())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
            }

        service.invoke(Request(Method.GET, "/file$uploadFilePath").withAuthentication())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.NOT_FOUND)
            }
    }

    private fun Response.handleResponse(body: (Response) -> Unit) = also(body)

    private val Response.contentType: String
        get () = header(HttpHeaders.CONTENT_TYPE) ?: fail("no content type")

}