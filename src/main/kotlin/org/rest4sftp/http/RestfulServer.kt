package org.rest4sftp.http

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
import org.rest4sftp.ftp.ApacheCommonsFtpClient
import org.rest4sftp.model.Command
import org.rest4sftp.model.CommandHandler
import org.rest4sftp.model.CreateFolder
import org.rest4sftp.model.DeleteFile
import org.rest4sftp.model.DeleteFolder
import org.rest4sftp.model.HttpResult
import org.rest4sftp.model.InputStreamResponseBody
import org.rest4sftp.model.JsonResponseBody
import org.rest4sftp.model.RemoteHost
import org.rest4sftp.model.RetrieveFile
import org.rest4sftp.model.RetrieveFolder
import org.rest4sftp.model.StringResponseBody
import org.rest4sftp.model.UploadFile
import java.time.Duration


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
                    host = it["host"].orEmpty(),
                    port = it["port"]?.toIntOrNull() ?: 21,
                    userName = it["user"].orEmpty(),
                    password = it["password"].orEmpty()
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

fun main() {

    //get from args if FTP or SFTP and the port and timeout
    //or a config file?

    RestfulServer(CommandHandler { ApacheCommonsFtpClient(it, Duration.ofSeconds(5)) }).start(7070)
}