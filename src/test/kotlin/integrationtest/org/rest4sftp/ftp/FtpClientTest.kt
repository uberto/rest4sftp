package integrationtest.org.rest4sftp.ftp

import org.rest4sftp.ftp.ApacheCommonsFtpClient
import org.rest4sftp.model.FtpHost
import org.rest4sftp.model.SimpleFtpClient
import assertk.assertThat
import assertk.assertions.*


import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FtpClientTest {

    @Test
    fun `retrieve list of all files in dir`() {

        val files = createFtpConnection().use { it.listFiles("/ftproot") }

        val names = files.map { it.name }

        assertThat(files.size).isEqualTo(2)
        assertThat(names).contains("test-xml.xml")
        assertThat(names).contains("test-text.txt")

    }

    @Test
    fun `retrieve empty list from non existent dir`() {

        val files = createFtpConnection().use { it.listFiles("/ftproot1") }

        assertThat(files).isEmpty()
    }

    @Test
    fun `delete folder`() {

        val deleteFolder = File("ftp-content-root/delete-folder")

        deleteFolder.mkdir()

        val deleteSuccess = createFtpConnection().use {
            it.deleteFolder("/ftproot/delete-folder")
        }

        assertTrue(deleteSuccess)
        assertFalse(deleteFolder.exists())

        val deleteFailure = createFtpConnection().use {
            it.deleteFolder("/ftproot/delete-folder")
        }

        assertFalse(deleteFailure)
        assertFalse(deleteFolder.exists())

    }


    @Test
    fun `create folder`() {

        val folderToBeCreated = File("ftp-content-root/new-folder")
        folderToBeCreated.delete()

        assertFalse(folderToBeCreated.exists())

        val createSuccess = createFtpConnection().use {
            it.createFolder("/ftproot/new-folder")
        }

        assertTrue(createSuccess)
        assertTrue(folderToBeCreated.exists())

        val createFailure = createFtpConnection().use {
            it.createFolder("/ftproot/new-folder")
        }

        assertFalse(createFailure)
        folderToBeCreated.delete()

        assertFalse(folderToBeCreated.exists())


    }

    @Test
    fun `retrieve the content of a file`() {

        val fileContents: ByteArray = createFtpConnection().use {
            it.retrieveFile("/ftproot", "test-xml.xml")
        }

        assertThat(fileContents.size).isGreaterThan(100)

        val expectedContent = File("ftp-content-root/test-xml.xml").readText()

        assertThat(String(fileContents)).isEqualTo(expectedContent)

    }

    @Test
    fun `retrieve empty content from file in non existent folder`() {

        val fileContents: ByteArray = createFtpConnection().use {
            it.retrieveFile("/ftproot1", "test-xml.xml")
        }

        assertThat(fileContents).hasSize(0)
    }

    @Test
    fun `delete file`() {

        val deleteFile = File("ftp-content-root/test-delete.xml")

        deleteFile.writeText("test")

        val deleteSuccess = createFtpConnection().use {
            it.deleteFile("/ftproot", "test-delete.xml")
        }

        assertTrue(deleteSuccess)
        assertFalse(deleteFile.exists())

        val deleteFailure = createFtpConnection().use {
            it.deleteFile("/ftproot", "test-delete.xml")
        }

        assertFalse(deleteFailure)
        assertFalse(deleteFile.exists())

    }

    @Test
    fun `upload file`() {

        val uploadFile = File("ftp-content-root/test-upload.xml")

        uploadFile.delete()
        assertFalse(uploadFile.exists())

        val uploadSuccess = createFtpConnection().use {
            it.uploadFile("/ftproot", "test-upload.xml", "test".byteInputStream())
        }

        assertTrue(uploadSuccess)
        assertTrue(uploadFile.exists())

        uploadFile.delete()

    }

    @Test
    fun `upload file to non existent directory`() {

        val uploadFile = File("ftp-content-root-123/test-upload.xml")

        assertFalse(uploadFile.exists())

        val uploadFailure = createFtpConnection().use {
            it.uploadFile("/ftproot123", "test-upload.xml", "test".byteInputStream())
        }

        assertFalse(uploadFailure)
        assertFalse(uploadFile.exists())

        uploadFile.delete()
    }

    val ftpHost = FtpHost(host = "127.0.0.1", port = 21, userName = "bob", password = "12345")

    private fun createFtpConnection(): SimpleFtpClient = ApacheCommonsFtpClient(ftpHost).withDebug().connect()

}