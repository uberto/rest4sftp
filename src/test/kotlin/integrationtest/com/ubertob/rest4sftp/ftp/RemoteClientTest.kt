package integrationtest.com.ubertob.rest4sftp.ftp

import assertk.assertThat
import assertk.assertions.*
import assertk.fail
import com.ubertob.rest4sftp.model.SimpleRemoteClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

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

        Assertions.assertTrue(deleteSuccess)
        Assertions.assertFalse(deleteFolder.exists())

        val deleteFailure = createConnection().use {
            it.deleteFolder("/upload/delete-folder")
        }

        Assertions.assertFalse(deleteFailure)
        Assertions.assertFalse(deleteFolder.exists())

    }


    @Test
    fun `create folder`() {

        val folderToBeCreated = File("${baseDir}new-folder")
        folderToBeCreated.delete()

        Assertions.assertFalse(folderToBeCreated.exists())

        val createSuccess = createConnection().use {
            it.createFolder("/upload/new-folder")
        }

        Assertions.assertTrue(createSuccess)
        Assertions.assertTrue(folderToBeCreated.exists())

        val createFailure = createConnection().use {
            it.createFolder("/upload/new-folder")
        }

        Assertions.assertFalse(createFailure)
        folderToBeCreated.delete()

        Assertions.assertFalse(folderToBeCreated.exists())


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

        Assertions.assertTrue(deleteSuccess)
        Assertions.assertFalse(deleteFile.exists())

        val deleteFailure = createConnection().use {
            it.deleteFile("/upload", "test-delete.xml")
        }

        Assertions.assertFalse(deleteFailure)
        Assertions.assertFalse(deleteFile.exists())

    }


    @Test
    fun `rename file`() {

        val toBeRenamedFile = File("${baseDir}test-rename.xml")
        toBeRenamedFile.writeText("test")

        val renameSuccess = createConnection().use {
            it.renameFile("/upload", "test-rename.xml", "test-renamed.xml")
        }
        Assertions.assertTrue(renameSuccess)

        val renamedFile = File("${baseDir}test-renamed.xml")

        Assertions.assertTrue(renamedFile.exists())
        Assertions.assertFalse(toBeRenamedFile.exists())

        createConnection().deleteFile("/upload", "test-renamed.xml")

    }


    @Test
    fun `upload file and rename when finish`() {

        val uploadFile = File("${baseDir}test-upload.xml")
        val uploadTempFile = File("${baseDir}test-upload.xml.io")

        uploadFile.delete()
        uploadTempFile.delete()
        Assertions.assertFalse(uploadFile.exists())


        val createdTemp = GlobalScope.async {
            while (true){
                delay(1)
                if (uploadTempFile.exists())
                    break
            }
            true
        }

        val uploadSuccess = createConnection().use {
            it.uploadFile("/upload", "test-upload.xml", "test".byteInputStream())
        }

        Assertions.assertTrue(uploadSuccess)
        Assertions.assertTrue(uploadFile.exists())
        Assertions.assertTrue(createdTemp.getCompleted())

        uploadFile.delete()

    }


    @Test
    fun `upload file`() {

        val uploadFile = File("${baseDir}test-upload.xml")
        val uploadTempFile = File("${baseDir}test-upload.xml.io")

        uploadFile.delete()
        uploadTempFile.delete()

        val uploadSuccess = createConnection().use {
            it.uploadFile("/upload", "test-upload.xml", "test".byteInputStream())
        }

        Assertions.assertTrue(uploadSuccess)
        Assertions.assertTrue(uploadFile.exists())

        uploadFile.delete()

    }

    @Test
    fun `upload file to non existent directory`() {

        val uploadFile = File("${baseDir}-123/test-upload.xml")

        Assertions.assertFalse(uploadFile.exists())

        val uploadFailure = createConnection().use {
            it.uploadFile("/upload123", "test-upload.xml", "test".byteInputStream())
        }

        Assertions.assertFalse(uploadFailure)
        Assertions.assertFalse(uploadFile.exists())

        uploadFile.delete()
    }

}
