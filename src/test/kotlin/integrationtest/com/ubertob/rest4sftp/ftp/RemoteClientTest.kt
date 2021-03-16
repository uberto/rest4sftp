package integrationtest.com.ubertob.rest4sftp.ftp

import assertk.assertThat
import assertk.assertions.*
import assertk.fail
import com.ubertob.rest4sftp.model.Filter
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

@TestInstance(PER_CLASS)
abstract class RemoteClientTest {


    val baseDir = "local-integration-test/ftp-content-root/"

    abstract fun createConnection(): SimpleRemoteClient

    @Test
    fun `retrieve list of all files in dir`() {

        val files = createConnection().use { it.listFiles("/upload") }

        val names = files?.map { it.name } ?: fail("retrieve failed")

        assertThat(files.size).isGreaterThanOrEqualTo(2)
        assertThat(names).contains("test-xml.xml")
        assertThat(names).contains("test-text.txt")

    }

    @ParameterizedTest
    @MethodSource("filterPatterns")
    fun `retrieve list of all files based on pattern`(pattern: String, expectedFiles: List<String>) {
        val files = createConnection().use { it.listFiles("/upload", Filter(pattern)) }

        val names = files?.map { it.name } ?: fail("retrieve failed")

        assertThat(names).isEqualTo(expectedFiles)
    }

    @Suppress("unused")
    private fun filterPatterns() = setOf(
        Arguments.of("*.xml", listOf("test-xml.xml")),
        Arguments.of("test.xml.xml", emptyList<String>()),
        Arguments.of("test-xml.xm?", listOf("test-xml.xml")),
        Arguments.of("test\\-xml.xml", emptyList<String>()),
        Arguments.of("test-+xml.xml", emptyList<String>()),
        Arguments.of("test-(x)ml.xml", emptyList<String>()),
        Arguments.of("test-[x]ml.xml", emptyList<String>()),
        Arguments.of("test-x{1}ml.xml", emptyList<String>()),
        Arguments.of("^test-xml.xml", emptyList<String>()),
        Arguments.of("test-xml.xml$", emptyList<String>()),
        Arguments.of("something|test-xml.xml", emptyList<String>()),
    )

    @Test
    fun `retrieves nothing from non existent dir`() {

        val files = createConnection().use { it.listFiles("/upload123") }

        assertThat(files).isNull()
    }

    @Test
    fun `delete folder`() {

        val deleteFolder = File("${baseDir}delete-folder")

        deleteFolder.mkdir()

        val deleteSuccess = createConnection().use {
            it.deleteFolder("/upload/delete-folder")
        }

        assertTrue(deleteSuccess)
        assertFalse(deleteFolder.exists())

        val deleteFailure = createConnection().use {
            it.deleteFolder("/upload/delete-folder")
        }

        assertFalse(deleteFailure)
        assertFalse(deleteFolder.exists())

    }


    @Test
    fun `create folder`() {

        val folderToBeCreated = File("${baseDir}new-folder")
        folderToBeCreated.delete()

        assertFalse(folderToBeCreated.exists())

        val createSuccess = createConnection().use {
            it.createFolder("/upload/new-folder")
        }

        assertTrue(createSuccess)
        assertTrue(folderToBeCreated.exists())

        val createFailure = createConnection().use {
            it.createFolder("/upload/new-folder")
        }

        assertFalse(createFailure)
        folderToBeCreated.delete()

        assertFalse(folderToBeCreated.exists())


    }

    @Test
    fun `retrieve the content of a text file`() {

        val fileContents = createConnection().use {
            it.retrieveFile("/upload", "test-xml.xml")
        }

        assertThat(fileContents?.size).isNotNull().isGreaterThan(100)

        val expectedContent = File("${baseDir}test-xml.xml").readText()

        assertThat(String(fileContents!!)).isEqualTo(expectedContent)
    }

    @Test
    fun `retrieve the content of a binary file`() {

        val fileContents = createConnection().use {
            it.retrieveFile("/upload", "test.zip")
        }

        assertThat(fileContents?.size).isNotNull().isGreaterThan(100)

        val expectedContent = File("${baseDir}test.zip").readBytes()

        assertThat(fileContents!!).isEqualTo(expectedContent)
    }

    @Test
    fun `retrieve nothing for file in non existent folder`() {

        val fileContents = createConnection().use {
            it.retrieveFile("/upload1", "test-xml.xml")
        }

        assertThat(fileContents).isNull()
    }

    @Test
    fun `delete file`() {

        val deleteFile = File("${baseDir}test-delete.xml")

        deleteFile.writeText("test")

        val deleteSuccess = createConnection().use {
            it.deleteFile("/upload", "test-delete.xml")
        }

        assertTrue(deleteSuccess)
        assertFalse(deleteFile.exists())

        val deleteFailure = createConnection().use {
            it.deleteFile("/upload", "test-delete.xml")
        }

        assertFalse(deleteFailure)
        assertFalse(deleteFile.exists())

    }


    @Test
    fun `rename file`() {

        val toBeRenamedFile = File("${baseDir}test-rename.xml")
        toBeRenamedFile.writeText("test")

        val renameSuccess = createConnection().use {
            it.renameFile("/upload", "test-rename.xml", "test-renamed.xml")
        }
        assertTrue(renameSuccess)

        val renamedFile = File("${baseDir}test-renamed.xml")

        assertTrue(renamedFile.exists())
        assertFalse(toBeRenamedFile.exists())

        createConnection().deleteFile("/upload", "test-renamed.xml")

    }


    @Test
    fun `upload file and rename when finish`() {

        val uploadFile = File("${baseDir}test-upload.xml")
        val uploadTempFile = File("${baseDir}test-upload.xml.io")

        uploadFile.delete()
        uploadTempFile.delete()
        assertFalse(uploadFile.exists())
        assertFalse(uploadTempFile.exists())

        var createdTemp = false

        Thread {
            while (true) {
                if (uploadTempFile.exists()) break
            }
            createdTemp = true
        }.start()

        val uploadSuccess = createConnection().use {
            it.uploadFile("/upload", "test-upload.xml", "test".byteInputStream())
        }

        assertTrue(uploadSuccess)
        assertTrue(uploadFile.exists())
        assertTrue(createdTemp)

        uploadFile.delete()

    }


    @Test
    fun `upload new file`() {

        val uploadFile = File("${baseDir}test-upload.xml")
        val uploadTempFile = File("${baseDir}test-upload.xml.io")

        uploadFile.delete()
        uploadTempFile.delete()

        val uploadSuccess = createConnection().use {
            it.uploadFile("/upload", "test-upload.xml", "test".byteInputStream())
        }

        assertTrue(uploadSuccess)
        assertTrue(uploadFile.exists())

        uploadFile.delete()

    }

    @Test
    fun `upload updated existing file`() {

        val uploadFile = File("${baseDir}test-upload.xml")
        val uploadTempFile = File("${baseDir}test-upload.xml.io")

        uploadFile.delete()
        uploadTempFile.delete()

        assertTrue(
                createConnection().use {
                    it.uploadFile("/upload", "test-upload.xml", "test".byteInputStream())
                }
        )

        assertTrue(uploadFile.exists())

        assertTrue(
                createConnection().use {
                    it.uploadFile("/upload", "test-upload.xml", "updated".byteInputStream())
                }
        )

        assertThat(uploadFile.readText()).isEqualTo("updated")

        uploadFile.delete()
    }

    @Test
    fun `upload file to non existent directory`() {

        val uploadFile = File("${baseDir}-123/test-upload.xml")

        assertFalse(uploadFile.exists())

        val uploadFailure = createConnection().use {
            it.uploadFile("/upload123", "test-upload.xml", "test".byteInputStream())
        }

        assertFalse(uploadFailure)
        assertFalse(uploadFile.exists())

        uploadFile.delete()
    }

}