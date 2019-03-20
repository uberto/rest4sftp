package com.ubertob.rest4sftp.http

import com.ubertob.rest4sftp.http.CustomHeaders.HOST_HEADER
import com.ubertob.rest4sftp.http.CustomHeaders.PORT_HEADER
import com.ubertob.rest4sftp.http.CustomHeaders.PWD_HEADER
import com.ubertob.rest4sftp.http.CustomHeaders.USER_HEADER
import com.ubertob.rest4sftp.model.RemoteHost
import org.apache.http.HttpHeaders.AUTHORIZATION
import org.http4k.base64Decoded
import org.http4k.core.Credentials
import org.http4k.core.Request

fun Request.toRemoteHost(): RemoteHost = basicAuthenticationCredentials()
        ?.let {
            RemoteHost(
                    host = requiredHeader(HOST_HEADER),
                    port = requiredHeader(PORT_HEADER).toInt(),
                    userName = it.user,
                    password = it.password
            )
        }
        ?: RemoteHost(
                host = requiredHeader(HOST_HEADER),
                port = requiredHeader(PORT_HEADER).toInt(),
                userName = requiredHeader(USER_HEADER),
                password = requiredHeader(PWD_HEADER)
        )

object CustomHeaders {
    val HOST_HEADER = "FTP-Host"
     val PORT_HEADER = "FTP-Port"
     val USER_HEADER = "FTP-User"
     val PWD_HEADER = "FTP-Password"
}

private fun Request.requiredHeader(name: String) =
        header(name) ?: throw UnauthorisedException(message = "$name not configured in headers")

private fun Request.basicAuthenticationCredentials(): Credentials? =
        header(AUTHORIZATION)?.replace("Basic ", "")?.toCredentials()

private fun String.toCredentials(): Credentials =
        base64Decoded().split(":").let { Credentials(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }) }

class UnauthorisedException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
