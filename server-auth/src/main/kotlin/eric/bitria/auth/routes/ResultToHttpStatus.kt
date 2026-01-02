package eric.bitria.auth.routes

import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import io.ktor.http.HttpStatusCode as HTTPStatusCode

/**
 * This methods map a Services Results to a [HTTPStatusCode].
 */

fun RegisterResult.toHttpStatus() = when (this) {
    RegisterResult.VERIFICATION_SENT -> HTTPStatusCode.OK
    RegisterResult.SUCCESS -> HTTPStatusCode.Created
    RegisterResult.USERNAME_EXISTS,
    RegisterResult.EMAIL_EXISTS -> HTTPStatusCode.Conflict
    RegisterResult.INVALID_USERNAME,
    RegisterResult.INVALID_EMAIL,
    RegisterResult.INVALID_PASSWORD -> HTTPStatusCode.BadRequest
    else -> HTTPStatusCode.InternalServerError
}

fun VerifyEmailResult.toHttpStatus() = when (this) {
    VerifyEmailResult.SUCCESS -> HTTPStatusCode.OK
    VerifyEmailResult.INVALID_EMAIL,
    VerifyEmailResult.INVALID_VERIFICATION_CODE -> HTTPStatusCode.BadRequest
    VerifyEmailResult.ACCOUNT_ALREADY_VERIFIED -> HTTPStatusCode.Conflict
    else -> HTTPStatusCode.InternalServerError
}

fun ResendVerificationCodeResult.toHttpStatus() = when (this) {
    ResendVerificationCodeResult.SUCCESS -> HTTPStatusCode.OK
    ResendVerificationCodeResult.INVALID_EMAIL -> HTTPStatusCode.BadRequest
    ResendVerificationCodeResult.EMAIL_NOT_REGISTERED,
    ResendVerificationCodeResult.EMAIL_ALREADY_VERIFIED -> HTTPStatusCode.Conflict
    else -> HTTPStatusCode.InternalServerError
}