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
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.shared.HexonPrimaryButton
import eric.bitria.hexon.viewmodel.auth.VerifyStatus
import eric.bitria.hexon.viewmodel.auth.VerifyViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VerifyScreen(
    email: String,
    verifyViewModel: VerifyViewModel = koinViewModel { parametersOf(email) },
    onVerifySuccess: () -> Unit
) {
    HexonTheme {
        LaunchedEffect(verifyViewModel.verifyStatus) {
            if (verifyViewModel.verifyStatus == VerifyStatus.SUCCESS) onVerifySuccess()
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
                    "Verify Email",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                        fontSize = (paddingScale * 0.08f).value.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(paddingScale * 0.02f))

                Text(
                    "We sent a 6-digit code to\n$email",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = (paddingScale * 0.035f).value.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(paddingScale * 0.06f))

                Column(
                    modifier = Modifier.fillMaxWidth(contentWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoginInputField(
                        value = verifyViewModel.code,
                        onValueChange = { verifyViewModel.onCodeChange(it) },
                        placeholder = "Verification Code",
                        error = verifyViewModel.errorMessage,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        paddingScale = paddingScale
                    )

                    Spacer(Modifier.height(paddingScale * 0.06f))

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
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = (paddingScale * 0.045f).value.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(paddingScale * 0.04f))

                    TextButton(
                        onClick = { verifyViewModel.resendCode() },
                        contentPadding = PaddingValues(paddingScale * 0.02f)
                    ) {
                        Text(
                            "Didn't receive a code? Resend",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = (paddingScale * 0.035f).value.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
