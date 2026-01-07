package eric.bitria.hexon.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.shared.HexonPrimaryButton
import eric.bitria.hexon.viewmodel.auth.ResetPasswordStatus
import eric.bitria.hexon.viewmodel.auth.ResetPasswordViewModel
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
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val paddingScale = dimensions.paddingScale

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
            val isPortrait = maxWidth < maxHeight
            val contentWidth = if (isPortrait) 0.85f else 0.4f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.screenVertical)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (isResetMode) "Reset Password" else "Change Password",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(spacing.small))

                val subText = if (isResetMode)
                    "Enter the code sent to your email and your new password."
                else "Enter your current password and your new password."

                Text(
                    subText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(contentWidth)
                )

                Spacer(Modifier.height(spacing.mediumLarge))

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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    } else {
                        LoginInputField(
                            value = viewModel.oldPassword,
                            onValueChange = { viewModel.onOldPasswordChange(it) },
                            placeholder = "Current Password",
                            error = viewModel.oldPasswordError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }

                    Spacer(Modifier.height(spacing.extraSmall))

                    LoginInputField(
                        value = viewModel.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder = "New Password",
                        error = viewModel.passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(Modifier.height(spacing.extraSmall))

                    LoginInputField(
                        value = viewModel.confirmPassword,
                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                        placeholder = "Confirm New Password",
                        error = viewModel.confirmPasswordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(Modifier.height(spacing.mediumLarge))

                    HexonPrimaryButton(
                        text = if (isResetMode) "Reset Password" else "Update Password",
                        onClick = { viewModel.changePassword() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.state != ResetPasswordStatus.LOADING,
                        paddingScale = paddingScale
                    ) {
                        if (viewModel.state == ResetPasswordStatus.LOADING) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(paddingScale * 0.05f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isResetMode) "Reset Password" else "Update Password",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(Modifier.height(spacing.mediumSmall))

                    TextButton(onClick = onNavigateBack) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    if (viewModel.state == ResetPasswordStatus.ERROR) {
                        Spacer(Modifier.height(spacing.mediumSmall))
                        Text(
                            text = viewModel.errorMessage ?: "Unknown error occurred",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
