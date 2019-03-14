package com.ubertob.rest4sftp.http

import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import com.ubertob.rest4sftp.model.Command
import com.ubertob.rest4sftp.model.CommandHandler
import com.ubertob.rest4sftp.model.CreateFolder
import com.ubertob.rest4sftp.model.DeleteFile
import com.ubertob.rest4sftp.model.DeleteFolder
import com.ubertob.rest4sftp.model.HttpResult
import com.ubertob.rest4sftp.model.InputStreamResponseBody
import com.ubertob.rest4sftp.model.JsonResponseBody
import com.ubertob.rest4sftp.model.RemoteHost
import com.ubertob.rest4sftp.model.RetrieveFile
import com.ubertob.rest4sftp.model.RetrieveFolder
import com.ubertob.rest4sftp.model.StringResponseBody
import com.ubertob.rest4sftp.model.UploadFile


class RestfulServer(private val commandHandler: CommandHandler) : HttpHandler {

    override fun invoke(request: Request): Response =
        routes(
            "/folder/{path:.*}" bind GET to { RetrieveFolder(it.path()).process(it) },
            "/folder/{path:.*}" bind PUT to { CreateFolder(it.path()).process(it) },
            "/folder/{path:.*}" bind DELETE to { DeleteFolder(it.path()).process(it) },
            "/file/{path:.*}" bind GET to { RetrieveFile(it.directoryName(), it.fileName()).process(it) },
            "/file/{path:.*}" bind PUT to { UploadFile(it.directoryName(), it.fileName(), it.body.stream).process(it) },
            "/file/{path:.*}" bind DELETE to { DeleteFile(it.directoryName(), it.fileName()).process(it) }
        ).invoke(request)


    private fun Request.toFtpHost(): RemoteHost =
        headers.toMap().let {
            RemoteHost(
                    host = it["FTP-Host"].orEmpty(),
                    port = it["FTP-Port"]?.toIntOrNull() ?: 21,
                    userName = it["FTP-User"].orEmpty(),
                    password = it["FTP-Password"].orEmpty()
            )
        }

    private fun Command.process(req: Request): Response = commandHandler.handle(req.toFtpHost(), this).toResponse()

    private fun HttpResult.toResponse() = when (val body = this.responseBody) {
        is StringResponseBody -> Response(this.status).body(body.asString).header("Content-Type", "text/plain")
        is InputStreamResponseBody -> Response(this.status).body(body.asInputStream).header("Content-Type", "application/octet-stream")
        is JsonResponseBody -> Response(this.status).body(body.asJson).header("Content-Type", "application/json")
    }

    private fun Request.path() = path("path").orEmpty()

    private fun Request.parts() = path().split("/")

    private fun Request.directoryName(): String = parts().dropLast(1).joinToString("/")

    private fun Request.fileName(): String = parts().last()

    fun start(port: Int) = this.asServer(Jetty(port)).start()
}
