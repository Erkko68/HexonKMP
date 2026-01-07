package eric.bitria.hexon.ui.screens.auth

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.viewmodel.ForgotPasswordStatus
import eric.bitria.hexon.viewmodel.ForgotPasswordViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = koinViewModel(),
    onNavigateToReset: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    HexonTheme {
        LaunchedEffect(viewModel.state) {
            if (viewModel.state == ForgotPasswordStatus.SUCCESS) {
                onNavigateToReset(viewModel.email)
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
                    "Forgot Password",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = (paddingScale * 0.06f).value.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(paddingScale * 0.02f))

                Text(
                    "Enter your email to receive a password reset code.",
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
                    LoginInputField(
                        value = viewModel.email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        placeholder = "Email",
                        error = viewModel.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        paddingScale = paddingScale
                    )

                    Spacer(Modifier.height(paddingScale * 0.05f))

                    Button(
                        onClick = { viewModel.forgotPassword() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(paddingScale * 0.12f)
                            .shadow(6.dp, RoundedCornerShape(paddingScale * 0.03f)),
                        shape = RoundedCornerShape(paddingScale * 0.03f),
                        enabled = viewModel.state != ForgotPasswordStatus.LOADING
                    ) {
                        if (viewModel.state == ForgotPasswordStatus.LOADING) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(paddingScale * 0.05f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Send Reset Code",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (paddingScale * 0.04f).value.sp
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(paddingScale * 0.03f))

                    TextButton(onClick = onNavigateBack) {
                        Text(
                            "Back to Login",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = (paddingScale * 0.035f).value.sp
                        )
                    }

                    if (viewModel.state == ForgotPasswordStatus.ERROR) {
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
