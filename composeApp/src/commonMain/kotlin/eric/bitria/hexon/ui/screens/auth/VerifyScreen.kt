package eric.bitria.hexon.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.shared.HexonPrimaryButton
import eric.bitria.hexon.viewmodel.auth.VerifyStatus
import eric.bitria.hexon.viewmodel.auth.VerifyViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VerifyScreen(
    email: String,
    verifyViewModel: VerifyViewModel = koinViewModel(),
    onVerifySuccess: () -> Unit
) {
    HexonTheme {
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val paddingScale = dimensions.paddingScale

        LaunchedEffect(email) {
            verifyViewModel.updateEmail(email)
        }

        LaunchedEffect(verifyViewModel.verifyStatus) {
            if (verifyViewModel.verifyStatus == VerifyStatus.SUCCESS) onVerifySuccess()
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
                    "Verify Email",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(spacing.small))

                Text(
                    "We sent a 6-digit code to\n${verifyViewModel.email}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(spacing.large))

                Column(
                    modifier = Modifier.fillMaxWidth(contentWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoginInputField(
                        value = verifyViewModel.code,
                        onValueChange = { verifyViewModel.onCodeChange(it) },
                        placeholder = "Verification Code",
                        error = verifyViewModel.errorMessage,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(Modifier.height(spacing.large))

                    HexonPrimaryButton(
                        text = "Verify",
                        onClick = { verifyViewModel.verify() },
                        modifier = Modifier.fillMaxWidth(),
                        paddingScale = paddingScale
                    ) {
                        if (verifyViewModel.verifyStatus == VerifyStatus.LOADING) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(paddingScale * 0.05f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Verify",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(Modifier.height(spacing.medium))

                    TextButton(
                        onClick = { verifyViewModel.resendCode() },
                        contentPadding = PaddingValues(spacing.small)
                    ) {
                        Text(
                            "Didn't receive a code? Resend",
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
