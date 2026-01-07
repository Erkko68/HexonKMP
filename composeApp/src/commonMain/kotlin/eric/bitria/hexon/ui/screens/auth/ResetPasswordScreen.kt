package eric.bitria.hexon.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.screens.auth.LoginInputField
import eric.bitria.hexon.viewmodel.ResetPasswordStatus
import eric.bitria.hexon.viewmodel.ResetPasswordViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResetPasswordScreen(
    email: String,
    isResetMode: Boolean = true,
    viewModel: ResetPasswordViewModel = koinViewModel(),
    onResetSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    HexonTheme {
        LaunchedEffect(email, isResetMode) {
            viewModel.init(email, isResetMode)
        }

        LaunchedEffect(viewModel.state) {
            if (viewModel.state == ResetPasswordStatus.SUCCESS) {
                onResetSuccess()
                viewModel.resetState()
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val paddingScale = minOf(maxWidth, maxHeight)
            val isPortrait = maxWidth < maxHeight
            val contentWidth = if (isPortrait) 0.85f else 0.4f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = paddingScale * 0.04f, vertical = paddingScale * 0.02f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (isResetMode) "Reset Password" else "Change Password",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = (paddingScale * 0.06f).value.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(paddingScale * 0.02f))

                val subText = if (isResetMode)
                    "Enter the code sent to your email and your new password."
                else "Enter your current password and your new password."

                Text(
                    subText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = (paddingScale * 0.035f).value.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(contentWidth)
                )

                Spacer(Modifier.height(paddingScale * 0.05f))

                Column(
                    modifier = Modifier.fillMaxWidth(contentWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isResetMode) {
                        LoginInputField(
                            value = viewModel.resetCode,
                            onValueChange = { viewModel.onResetCodeChange(it) },
                            placeholder = "6-digit Code",
                            error = viewModel.resetCodeError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            paddingScale = paddingScale
                        )
                    } else {
                        LoginInputField(
                            value = viewModel.oldPassword,
                            onValueChange = { viewModel.onOldPasswordChange(it) },
                            placeholder = "Current Password",
                            error = viewModel.oldPasswordError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            paddingScale = paddingScale
                        )
                    }

                    Spacer(Modifier.height(paddingScale * 0.025f))

                    LoginInputField(
                        value = viewModel.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder = "New Password",
                        error = viewModel.passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        paddingScale = paddingScale
                    )

                    Spacer(Modifier.height(paddingScale * 0.025f))

                    LoginInputField(
                        value = viewModel.confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                        placeholder = "Confirm New Password",
                        error = viewModel.confirmPasswordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        paddingScale = paddingScale
                    )

                    Spacer(Modifier.height(paddingScale * 0.05f))

                    Button(
                        onClick = { viewModel.changePassword() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(paddingScale * 0.12f)
                            .shadow(6.dp, RoundedCornerShape(paddingScale * 0.03f)),
                        shape = RoundedCornerShape(paddingScale * 0.03f),
                        enabled = viewModel.state != ResetPasswordStatus.LOADING
                    ) {
                        if (viewModel.state == ResetPasswordStatus.LOADING) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(paddingScale * 0.05f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isResetMode) "Reset Password" else "Update Password",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (paddingScale * 0.04f).value.sp
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(paddingScale * 0.03f))

                    TextButton(onClick = onNavigateBack) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (paddingScale * 0.035f).value.sp
                        )
                    }

                    if (viewModel.state == ResetPasswordStatus.ERROR) {
                        Spacer(Modifier.height(paddingScale * 0.035f))
                        Text(
                            text = viewModel.errorMessage ?: "Unknown error occurred",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (paddingScale * 0.035f).value.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}