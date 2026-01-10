package eric.bitria.hexon.utils

import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
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

fun ResendVerificationCodeResult.toHttpStatus() = when (this) {
    ResendVerificationCodeResult.SUCCESS -> HTTPStatusCode.OK
    ResendVerificationCodeResult.ALREADY_VERIFIED -> HTTPStatusCode.Conflict
    ResendVerificationCodeResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun ChangePasswordResult.toHttpStatus() = when (this) {
    ChangePasswordResult.SUCCESS -> HTTPStatusCode.OK
    ChangePasswordResult.WRONG_PASSWORD -> HTTPStatusCode.Unauthorized
    ChangePasswordResult.INVALID_PASSWORD -> HTTPStatusCode.BadRequest
    ChangePasswordResult.USER_NOT_FOUND -> HTTPStatusCode.NotFound
    ChangePasswordResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun ForgotPasswordResult.toHttpStatus() = when (this) {
    ForgotPasswordResult.SUCCESS -> HTTPStatusCode.OK
    ForgotPasswordResult.INVALID_EMAIL -> HTTPStatusCode.BadRequest
    ForgotPasswordResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun ResetPasswordResult.toHttpStatus() = when (this) {
    ResetPasswordResult.SUCCESS -> HTTPStatusCode.OK
    ResetPasswordResult.INVALID_CODE,
    ResetPasswordResult.INVALID_EMAIL,
    ResetPasswordResult.INVALID_PASSWORD -> HTTPStatusCode.BadRequest
    ResetPasswordResult.USER_NOT_FOUND -> HTTPStatusCode.NotFound
    ResetPasswordResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun DeleteAccountResult.toHttpStatus() = when (this) {
    DeleteAccountResult.SUCCESS -> HTTPStatusCode.OK
    DeleteAccountResult.WRONG_PASSWORD -> HTTPStatusCode.Unauthorized
    DeleteAccountResult.INVALID_CODE -> HTTPStatusCode.BadRequest
    DeleteAccountResult.USER_NOT_FOUND -> HTTPStatusCode.NotFound
    DeleteAccountResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}