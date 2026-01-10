package eric.bitria.hexon.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.shared.HexonPrimaryButton
import eric.bitria.hexon.ui.screens.auth.LoginInputField
import eric.bitria.hexon.viewmodel.account.ResetPasswordStatus
import eric.bitria.hexon.viewmodel.account.ResetPasswordViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResetPasswordScreen(
    email: String,
    viewModel: ResetPasswordViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    HexonTheme {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val paddingScale = dimensions.paddingScale

        LaunchedEffect(email) {
            viewModel.init(email)
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
                    "Reset Password",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(spacing.small))

                Text(
                    "Enter the code sent to your email and your new password.",
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
                    LoginInputField(
                        value = viewModel.resetCode,
                        onValueChange = { viewModel.onResetCodeChange(it) },
                        placeholder = "6-digit Code",
                        error = viewModel.resetCodeError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(Modifier.height(spacing.small))

                    LoginInputField(
                        value = viewModel.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder = "New Password",
                        error = viewModel.passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(Modifier.height(spacing.small))

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
                        text = "Reset Password",
                        onClick = { viewModel.resetPassword() },
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
                                "Reset Password",
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
