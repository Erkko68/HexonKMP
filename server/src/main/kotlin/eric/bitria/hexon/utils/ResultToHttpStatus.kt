package eric.bitria.hexon.utils

import eric.bitria.hexon.dtos.account.ChangePasswordResult
import eric.bitria.hexon.dtos.account.DeleteAccountResult
import eric.bitria.hexon.dtos.account.ForgotPasswordResult
import eric.bitria.hexon.dtos.account.ResetPasswordResult
import eric.bitria.hexon.dtos.auth.LoginResult
import eric.bitria.hexon.dtos.auth.LogoutResult
import eric.bitria.hexon.dtos.auth.RefreshResult
import eric.bitria.hexon.dtos.auth.RegisterResult
import eric.bitria.hexon.dtos.auth.ResendVerificationCodeResult
import eric.bitria.hexon.dtos.auth.VerifyEmailResult
import eric.bitria.hexon.dtos.matchmaking.CreateLobbyResult
import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import eric.bitria.hexon.dtos.social.AddFriendResult
import eric.bitria.hexon.dtos.social.GetFriendRequestsResult
import eric.bitria.hexon.dtos.social.GetFriendsResult
import eric.bitria.hexon.dtos.social.RespondFriendResult
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

fun GetFriendsResult.toHttpStatus() = when (this) {
    GetFriendsResult.SUCCESS -> HTTPStatusCode.OK
    GetFriendsResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}


fun AddFriendResult.toHttpStatus() = when (this) {
    AddFriendResult.SUCCESS -> HTTPStatusCode.OK // or Created (201)
    AddFriendResult.USER_NOT_FOUND -> HTTPStatusCode.NotFound
    AddFriendResult.ALREADY_FRIENDS -> HTTPStatusCode.Conflict // 409 Conflict fits well
    AddFriendResult.REQUEST_ALREADY_SENT -> HTTPStatusCode.Conflict
    AddFriendResult.CANNOT_ADD_SELF -> HTTPStatusCode.BadRequest
    AddFriendResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}


fun RespondFriendResult.toHttpStatus() = when (this) {
    RespondFriendResult.SUCCESS -> HTTPStatusCode.OK
    RespondFriendResult.REQUEST_NOT_FOUND -> HTTPStatusCode.NotFound
    RespondFriendResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun GetFriendRequestsResult.toHttpStatus() = when (this) {
    GetFriendRequestsResult.SUCCESS -> HTTPStatusCode.OK
    GetFriendRequestsResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun JoinGameResult.toHttpStatus() = when (this) {
    JoinGameResult.SUCCESS -> HTTPStatusCode.OK
    JoinGameResult.INVALID_MODE -> HTTPStatusCode.BadRequest
    JoinGameResult.SESSION_FULL -> HTTPStatusCode.Conflict
    JoinGameResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun CreateLobbyResult.toHttpStatus() = when (this) {
    CreateLobbyResult.SUCCESS -> HTTPStatusCode.OK
    CreateLobbyResult.INVALID_MODE,
    CreateLobbyResult.INVALID_MAX_PLAYERS -> HTTPStatusCode.BadRequest
    CreateLobbyResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}

fun LogoutResult.toHttpStatus() = when (this) {
    LogoutResult.SUCCESS -> HTTPStatusCode.OK
    LogoutResult.INVALID_TOKEN -> HTTPStatusCode.Unauthorized
    LogoutResult.UNKNOWN_ERROR -> HTTPStatusCode.InternalServerError
}