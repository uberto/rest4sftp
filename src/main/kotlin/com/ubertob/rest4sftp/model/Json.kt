package com.ubertob.rest4sftp.model

import com.ubertob.kondor.json.*

object JFolder : JAny<FolderInfo>() {
    val name by str(FolderInfo::name)
    val date by str(FolderInfo::date)
    val fullFolderPath by str(FolderInfo::fullFolderPath)

    override fun JsonNodeObject.deserializeOrThrow() =
        FolderInfo(
            name = +name,
            date = +date,
            fullFolderPath = +fullFolderPath
        )
}


object JFileInfo : JAny<FileInfo>() {
    val name by str(FileInfo::name)
    val date by str(FileInfo::date)
    val size by num(FileInfo::size)
    val folderPath by str(FileInfo::folderPath)

    override fun JsonNodeObject.deserializeOrThrow() =
        FileInfo(
            name = +name,
            date = +date,
            size = +size,
            folderPath = +folderPath
        )

}

object JFolderResponse : JAny<FolderResponse>() {

    val folders by array(JFolder, FolderResponse::folders)
    val files by array(JFileInfo, FolderResponse::files)

    override fun JsonNodeObject.deserializeOrThrow() =
        FolderResponse(
            folders = +folders,
            files = +files
        )

}