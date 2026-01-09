package eric.bitria.hexon.utils

import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import io.ktor.http.HttpStatusCode as HTTPStatusCode

/**
 * This methods map a Services Results to a [HTTPStatusCode].
 */

fun RegisterResult.toHttpStatus() = when (this) {
    RegisterResult.SUCCESS -> HTTPStatusCode.OK
    RegisterResult.USERNAME_ALREADY_EXISTS,
    RegisterResult.EMAIL_ALREADY_EXISTS -> HTTPStatusCode.Conflict
    RegisterResult.INVALID_USERNAME,
    RegisterResult.INVALID_EMAIL,
    RegisterResult.INVALID_PASSWORD -> HTTPStatusCode.BadRequest
    else -> HTTPStatusCode.InternalServerError
}

fun VerifyEmailResult.toHttpStatus() = when (this) {
    VerifyEmailResult.SUCCESS -> HTTPStatusCode.OK
    VerifyEmailResult.INVALID_CODE -> HTTPStatusCode.BadRequest
    VerifyEmailResult.USER_NOT_FOUND -> HTTPStatusCode.NotFound
    VerifyEmailResult.ALREADY_VERIFIED -> HTTPStatusCode.Conflict
    VerifyEmailResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun RefreshResult.toHttpStatus() = when (this) {
    RefreshResult.SUCCESS -> HTTPStatusCode.OK
    RefreshResult.USER_NOT_FOUND -> HTTPStatusCode.NotFound
    RefreshResult.TOKEN_MISMATCH,
    RefreshResult.INVALID_TOKEN -> HTTPStatusCode.Unauthorized
    RefreshResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun LoginResult.toHttpStatus() = when (this) {
    LoginResult.SUCCESS -> HTTPStatusCode.OK
    LoginResult.INVALID_CREDENTIALS,
    LoginResult.NOT_VERIFIED -> HTTPStatusCode.Unauthorized
    LoginResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun ChangePasswordResult.toHttpStatus() = when (this) {
    ChangePasswordResult.SUCCESS -> HTTPStatusCode.OK
    ChangePasswordResult.INVALID_PASSWORD_FORMAT -> TODO()
    ChangePasswordResult.INVALID_PASSWORD -> TODO()
    ChangePasswordResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun ResetPasswordResult.toHttpStatus() = when (this) {
    ResetPasswordResult.SUCCESS -> HTTPStatusCode.OK
    ResetPasswordResult.UNKNOWN_EMAIL -> TODO()
    ResetPasswordResult.INVALID_EMAIL_FORMAT -> TODO()
    ResetPasswordResult.INVALID_CODE_FORMAT -> TODO()
    ResetPasswordResult.INVALID_PASSWORD_FORMAT -> TODO()
    ResetPasswordResult.INVALID_CODE -> TODO()
    ResetPasswordResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}