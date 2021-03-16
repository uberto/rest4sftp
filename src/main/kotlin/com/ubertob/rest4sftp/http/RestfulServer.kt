package com.ubertob.rest4sftp.http

import com.ubertob.rest4sftp.model.*
import org.http4k.core.HttpHandler
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer


class RestfulServer(private val commandHandler: CommandHandler) : HttpHandler {

    override fun invoke(request: Request): Response =
        routes(
            "/folder/{path:.*}" bind GET to { RetrieveFolder(it.path(), Filter(it.query("name"))).process(it) },
            "/folder/{path:.*}" bind PUT to { CreateFolder(it.path()).process(it) },
            "/folder/{path:.*}" bind DELETE to { DeleteFolder(it.path()).process(it) },
            "/file/{path:.*}" bind GET to { RetrieveFile(it.directoryName(), it.fileName()).process(it) },
            "/file/{path:.*}" bind PUT to { UploadFile(it.directoryName(), it.fileName(), it.body.stream).process(it) },
            "/file/{path:.*}" bind DELETE to { DeleteFile(it.directoryName(), it.fileName()).process(it) }
        ).invoke(request)


    private fun Command.process(req: Request): Response =
            try {
                commandHandler.handle(req.toRemoteHost(), this).toResponse()
            } catch (e: UnauthorisedException) {
                Response(Status.UNAUTHORIZED).body(e.message.orEmpty())
            }

    private fun HttpResult.toResponse() = when (val body = this.responseBody) {
        is StringResponseBody -> Response(this.status).body(body.asString).header("Content-Type", "text/plain")
        is InputStreamResponseBody -> Response(this.status).body(body.asInputStream).header("Content-Type", "application/octet-stream")
        is JsonResponseBody -> Response(this.status).body(body.asJson).header("Content-Type", "application/json; charset=utf-8")
    }

    private fun Request.path() = path("path").orEmpty()

    private fun Request.parts() = path().split("/")

    private fun Request.directoryName(): String = parts().dropLast(1).joinToString("/")

    private fun Request.fileName(): String = parts().last()

    fun start(port: Int) = this.asServer(Jetty(port)).start()
}