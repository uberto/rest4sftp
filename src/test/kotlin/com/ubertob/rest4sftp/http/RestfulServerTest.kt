
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.ubertob.rest4sftp.http.CustomHeaders
import com.ubertob.rest4sftp.http.RestfulServer
import com.ubertob.rest4sftp.model.CommandHandler
import com.ubertob.rest4sftp.model.FileInfo
import com.ubertob.rest4sftp.model.FolderInfo
import com.ubertob.rest4sftp.model.toFolderResponse
import com.ubertob.rest4sftp.testing.SpySimpleRemoteClient
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Instant


class RestfulServerTest {

    private val ROOT_FOLDER = "folder1"

    private val file1 = FileInfo("file1", Instant.ofEpochSecond(0), 123, ROOT_FOLDER)
    private val file2 = FileInfo("file2", Instant.ofEpochSecond(0), 123, ROOT_FOLDER)
    private val dir1 = FolderInfo("subFolder", Instant.ofEpochSecond(0), ROOT_FOLDER)
    private val files = mutableMapOf(ROOT_FOLDER to mutableListOf(file1, file2, dir1))

    private lateinit var fakeFtpClient: SpySimpleRemoteClient

    private val contents: MutableMap<String, ByteArray> = mutableMapOf("folder1/file1" to "<xml/>".toByteArray())
    private val handler = RestfulServer(CommandHandler {
        fakeFtpClient = SpySimpleRemoteClient(it, files, contents)
        fakeFtpClient
    })

    private val connectionHeaders = listOf(
        CustomHeaders.HOST_HEADER to "server",
        CustomHeaders.PORT_HEADER to "22",
        CustomHeaders.USER_HEADER to "user",
        CustomHeaders.PWD_HEADER to "pwd"
    )

    @Test
    fun `map files to folder response`() {
        val expectedJson = ObjectMapper().writeValueAsString(files[ROOT_FOLDER]?.toFolderResponse())

        assertThat(expectedJson).isEqualTo("""{"folders":[{"name":"subFolder","date":{"epochSecond":0,"nano":0},"fullFolderPath":"folder1"}],"files":[{"name":"file1","date":{"epochSecond":0,"nano":0},"size":123,"folderPath":"folder1"},{"name":"file2","date":{"epochSecond":0,"nano":0},"size":123,"folderPath":"folder1"}]}""".trimIndent())
    }


    @Test
    fun `retrieve list of all files in dir`() {
        val expectedJson = ObjectMapper().writeValueAsString(files[ROOT_FOLDER]?.toFolderResponse())
        val req = Request(Method.GET, "/folder/folder1").headers(connectionHeaders)

        val response = handler(req)
        assertThat(response).all {
            hasStatus(Status.OK)
            hasBody(expectedJson)
        }

        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `non existent folder returns 404`() {
        val req = Request(Method.GET, "/folder/folder3").headers(connectionHeaders)

        val response = handler(req)
        assertThat(response).all {
            hasStatus(Status.NOT_FOUND)
        }
        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `can create a directory`() {
        val folderPath = "folder2"
        val createDirectory = Request(Method.PUT, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(Status.OK)
            hasBody("created folder: $folderPath")
        }

        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `can't create a directory that exists`() {
        val folderPath = ROOT_FOLDER
        val createDirectory = Request(Method.PUT, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(Status.NOT_FOUND)
            hasBody("impossible to create folder: $folderPath")
        }
        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `delete a directory`() {
        val folderPath = ROOT_FOLDER
        val createDirectory = Request(Method.DELETE, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(Status.OK)
            hasBody("deleted folder: $folderPath")
        }
        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `can't delete a directory that doesn't exist`() {
        val folderPath = "folder2"
        val createDirectory = Request(Method.DELETE, "/folder/$folderPath").headers(connectionHeaders)

        assertThat(handler(createDirectory)).all {
            hasStatus(Status.NOT_FOUND)
            hasBody("impossible to delete folder: $folderPath")
        }
        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `retrieve the content of a file`() {
        val req = Request(Method.GET, "/file/folder1/file1").headers(connectionHeaders)
        val response = handler(req)

        assertThat(response).all {
            hasStatus(Status.OK)
            hasBody("<xml/>")
        }
        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `delete a file`() {
        val filePath = "folder1/file1"
        val delete = Request(Method.DELETE, "/file/$filePath").headers(connectionHeaders)

        assertThat(handler(delete)).all {
            hasStatus(Status.OK)
            hasBody("deleted: $filePath")
        }
        assertFalse(fakeFtpClient.isConnected())
    }


    @Test
    fun `can't delete a file that doesn't exist`() {
        val filePath = "folder1/file5"
        val delete = Request(Method.DELETE, "/file/$filePath").headers(connectionHeaders)

        assertThat(handler(delete)).all {
            hasStatus(Status.NOT_FOUND)
            hasBody("impossible to delete: $filePath")
        }

        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `upload a file`() {
        val filePath = "folder1/file1"
        val upload = Request(Method.PUT, "/file/$filePath").headers(connectionHeaders).body(Body("test".byteInputStream()))

        assertThat(handler(upload)).all {
            hasStatus(Status.OK)
            hasBody("uploaded: $filePath")
        }
        assertFalse(fakeFtpClient.isConnected())
    }

    @Test
    fun `can't upload a file to a directory that doesn't exist`() {
        val filePath = "folder10/file1"
        val upload = Request(Method.PUT, "/file/$filePath").headers(connectionHeaders).body(Body("test".byteInputStream()))

        assertThat(handler(upload)).all {
            hasStatus(Status.BAD_REQUEST)
            hasBody("could not upload: $filePath")
        }
        assertFalse(fakeFtpClient.isConnected())
    }

    fun Assert<Response>.hasStatus(expected: Status) {
        transform { assertThat(it.status).isEqualTo(expected) }
    }

    fun Assert<Response>.hasBody(expected: String) {
        transform { assertThat(it.bodyString()).isEqualTo(expected) }
    }
}

