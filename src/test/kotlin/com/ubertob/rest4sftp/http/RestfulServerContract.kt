package com.ubertob.rest4sftp.http

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.fail
import com.ubertob.rest4sftp.model.CommandHandler
import com.ubertob.rest4sftp.model.SimpleFtpClientFactory
import org.apache.http.HttpHeaders
import org.http4k.base64Encode
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

    private fun Request.withAuthentication(password: String = ftpPassword) =
        headers(listOf(
            "FTP-Host" to ftpHost,
            "FTP-Port" to ftpPort.toString(),
            "FTP-User" to ftpUser,
            "FTP-Password" to password
        ))

    private fun Request.withAuthorisation(password: String = ftpPassword) =
            headers(listOf(
                    "FTP-Host" to ftpHost,
                    "FTP-Port" to ftpPort.toString(),
                    "Authorization" to "Basic ${"$ftpUser:$password".base64Encode()}"
            ))

    private val handler: RestfulServer = RestfulServer(CommandHandler { ftpClientFactory(it) })

    @Test
    fun `returns unauthorised if authorisation headers not present`() {
        val response = handler(Request(Method.GET, "/folder/upload"))
        assertThat(response.status).isEqualTo(Status.UNAUTHORIZED)
    }

    @Test
    fun `returns unauthorised if invalid credentials supplied`() {
        val request = Request(Method.GET, "/folder/upload").withAuthentication("bad password")
        val response = handler(request)
        assertThat(response.status).isEqualTo(Status.UNAUTHORIZED)
    }

    @Test
    fun `returns unauthorised if invalid credentials supplied with basic auth`() {
        val request = Request(Method.GET, "/folder/upload").withAuthorisation("bad password")
        val response = handler(request)
        assertThat(response.status).isEqualTo(Status.UNAUTHORIZED)
    }

    @Test
    fun `returns file not found for invalid path`() {
        val request = Request(Method.GET, "/file/upload/unknown_file.txt").withAuthorisation()
        val response = handler(request)
        assertThat(response.status).isEqualTo(Status.NOT_FOUND)
    }

    @Test
    fun `returns file not found if given a folder path`() {
        val request = Request(Method.GET, "/file/upload").withAuthorisation()
        val response = handler(request)
        assertThat(response.status).isEqualTo(Status.NOT_FOUND)
    }

    @Test
    fun `returns folder not found if folder does not exist on server`() {
        val request = Request(Method.GET, "/folder/upload/unknown_subfolder").withAuthorisation()
        val response = handler(request)
        assertThat(response.status).isEqualTo(Status.NOT_FOUND)
    }

    @Test
    fun `returns valid folder`() {
        val request = Request(Method.GET, "/folder/upload").withAuthorisation()
        val response = handler(request)
        assertThat(response.status).isEqualTo(Status.OK)
        assertThat(response.contentType).isEqualTo(ContentType.APPLICATION_JSON.toHeaderValue())
    }

    @Test
    fun `can upload a new file, retrieve it, update it, and delete it`() {

        val uploadFilePath = "/upload/uploaded-file-${LocalDateTime.now()}.txt"
        val testFileContents = "this is an uploaded file"
        val updatedFileContents = "this is a revised uploaded file"

        handler(Request(Method.GET, "/file$uploadFilePath").withAuthorisation())
            .handleResponse {
                assertThat(it.status).isEqualTo(Status.NOT_FOUND)
            }

        handler(Request(Method.PUT, "/file$uploadFilePath").body(testFileContents).withAuthorisation()).handleResponse {
            assertThat(it.status).isEqualTo(Status.OK)
        }

        handler(Request(Method.GET, "/file$uploadFilePath").withAuthorisation())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
                assertThat(response.contentType).isEqualTo(OCTET_STREAM.value)
                assertThat(response.bodyString()).isEqualTo(testFileContents)
            }

        handler(Request(Method.PUT, "/file$uploadFilePath").body(updatedFileContents).withAuthorisation())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
            }

        handler(Request(Method.GET, "/file$uploadFilePath").withAuthorisation())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
                assertThat(response.contentType).isEqualTo(OCTET_STREAM.value)
                assertThat(response.bodyString()).isEqualTo(updatedFileContents)
            }

        handler(Request(Method.DELETE, "/file$uploadFilePath").withAuthorisation())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.OK)
            }

        handler(Request(Method.GET, "/file$uploadFilePath").withAuthorisation())
            .handleResponse { response ->
                assertThat(response.status).isEqualTo(Status.NOT_FOUND)
            }
    }

    private fun Response.handleResponse(body: (Response) -> Unit) = also(body)

    private val Response.contentType: String
        get () = header(HttpHeaders.CONTENT_TYPE) ?: fail("no content type")

}