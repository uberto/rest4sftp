package com.ubertob.rest4sftp.model

import org.http4k.core.Status
import java.io.InputStream

sealed class ResponseBody
class StringResponseBody(val asString: String): ResponseBody()
class JsonResponseBody(val asJson: String): ResponseBody()
class InputStreamResponseBody(val asInputStream: InputStream): ResponseBody()

data class HttpResult(val status: Status, val responseBody: ResponseBody)


