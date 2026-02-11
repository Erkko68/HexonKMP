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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.ui.components.shared.HexonPrimaryButton
import eric.bitria.hexon.ui.screens.auth.LoginInputField
import eric.bitria.hexon.viewmodel.account.DeleteAccountViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    HexonTheme {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val paddingScale = dimensions.paddingScale

        var showConfirmDialog by remember { mutableStateOf(false) }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Delete Account") },
                text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.confirmDelete()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
                    "Delete Account",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(spacing.small))

                Text(
                    "Deleting your account is permanent. Please verify your identity.",
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
                    if (viewModel.codeSent) {
                        LoginInputField(
                            value = viewModel.code,
                            onValueChange = { viewModel.onCodeChange(it) },
                            placeholder = "6-digit Verification Code",
                            error = viewModel.codeError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(Modifier.height(spacing.small))

                        LoginInputField(
                            value = viewModel.password,
                            onValueChange = { viewModel.onPasswordChange(it) },
                            placeholder = "Password",
                            error = viewModel.passwordError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }

                    Spacer(Modifier.height(spacing.mediumLarge))

                    HexonPrimaryButton(
                        text = if (viewModel.codeSent) "Delete Account" else "Send Verification Code",
                        onClick = {
                            if (viewModel.codeSent) {
                                showConfirmDialog = true
                            } else {
                                viewModel.initiateDelete()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.state !is ApiResult.Loading,
                        paddingScale = paddingScale
                    ) {
                        if (viewModel.state is ApiResult.Loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(paddingScale * 0.05f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (viewModel.codeSent) "Delete Account" else "Send Verification Code",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (viewModel.codeSent) {
                        Spacer(Modifier.height(spacing.mediumSmall))
                        TextButton(onClick = { viewModel.resendCode() }) {
                            Text(
                                "Resend Code",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(spacing.mediumSmall))

                    when (val state = viewModel.state) {
                        is ApiResult.Error -> {
                            Text(
                                text = state.message ?: "Unknown error occurred",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(spacing.mediumSmall))
                        }
                        is ApiResult.NetworkError -> {
                            Text(
                                text = "Network error. Please check your connection.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(spacing.mediumSmall))
                        }
                        else -> {}
                    }

                    TextButton(onClick = onNavigateBack) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
