package eric.bitria.hexon.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.ui.components.shared.HexonPrimaryButton
import eric.bitria.hexon.viewmodel.auth.LoginStatus
import eric.bitria.hexon.viewmodel.auth.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToVerify: (String) -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    HexonTheme {
        var selectedTab by remember { mutableStateOf("Login") }

        LaunchedEffect(loginViewModel.loginState) {
            when (loginViewModel.loginState) {
                LoginStatus.SUCCESS -> onLoginSuccess()
                LoginStatus.VERIFICATION_SENT -> {
                    onNavigateToVerify(loginViewModel.email)
                    loginViewModel.resetState()
                }
                else -> {}
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
                    "Hexon",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp,
                        fontSize = (paddingScale * 0.08f).value.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(paddingScale * 0.04f))

                // Toggle Login / Register
                Row(
                    modifier = Modifier
                        .height(paddingScale * 0.12f)
                        .fillMaxWidth(contentWidth)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(paddingScale * 0.02f)
                        )
                        .padding(paddingScale * 0.01f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Login", "Register").forEach { tab ->
                        val selected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(
                                    elevation = if (selected) 4.dp else 0.dp,
                                    shape = RoundedCornerShape(paddingScale * 0.015f)
                                )
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(paddingScale * 0.015f)
                                )
                                .clickable { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tab,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = (paddingScale * 0.04f).value.sp
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(paddingScale * 0.05f))

                Column(
                    modifier = Modifier.fillMaxWidth(contentWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Name (Register only) ---
                    AnimatedVisibility(visible = selectedTab == "Register") {
                        Column {
                            LoginInputField(
                                value = loginViewModel.name,
                                onValueChange = { loginViewModel.onNameChange(it) },
                                placeholder = "Name",
                                error = loginViewModel.nameError,
                                paddingScale = paddingScale
                            )
                            Spacer(Modifier.height(paddingScale * 0.025f))
                        }
                    }

                    // --- Email ---
                    LoginInputField(
                        value = loginViewModel.email,
                        onValueChange = { loginViewModel.onEmailChange(it) },
                        placeholder = "Email",
                        error = loginViewModel.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        paddingScale = paddingScale
                    )

                    Spacer(Modifier.height(paddingScale * 0.025f))

                    // --- Password ---
                    LoginInputField(
                        value = loginViewModel.password,
                        onValueChange = { loginViewModel.onPasswordChange(it) },
                        placeholder = "Password",
                        error = loginViewModel.passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        paddingScale = paddingScale
                    )

                    Spacer(Modifier.height(paddingScale * 0.025f))

                    // --- Confirm Password (Register only) ---
                    AnimatedVisibility(visible = selectedTab == "Register") {

                        LoginInputField(
                            value = loginViewModel.confirmPassword,
                            onValueChange = { loginViewModel.onConfirmPasswordChange(it) },
                            placeholder = "Confirm Password",
                            error = loginViewModel.confirmPasswordError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            paddingScale = paddingScale
                        )
                    }

                    Spacer(Modifier.height(paddingScale * 0.05f))

                    // --- Action Button ---
                    HexonPrimaryButton(
                        text = selectedTab,
                        onClick = {
                            if (selectedTab == "Login") loginViewModel.loginWithEmail()
                            else loginViewModel.registerWithEmail()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        paddingScale = paddingScale
                    ) {
                        if (loginViewModel.loginState == LoginStatus.LOADING || loginViewModel.loginState == LoginStatus.SUCCESS) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(paddingScale * 0.05f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "ButtonTextAnimation"
                            ) { tab ->
                                Text(
                                    tab,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = (paddingScale * 0.045f).value.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(paddingScale * 0.07f))

                    // Divider + OAuth buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "Or continue with",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = (paddingScale * 0.03f).value.sp
                            ),
                            modifier = Modifier.padding(horizontal = paddingScale * 0.02f)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    Spacer(Modifier.height(paddingScale * 0.035f))

                    OutlinedButton(
                        onClick = { loginViewModel.continueWithGoogle() },
                        modifier = Modifier.fillMaxWidth().height(paddingScale * 0.12f),
                        shape = RoundedCornerShape(paddingScale * 0.03f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        contentPadding = PaddingValues(vertical = paddingScale * 0.02f)
                    ) {
                        Text(
                            "Google", 
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = (paddingScale * 0.04f).value.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // --- Forgot Password Button ---
                    AnimatedVisibility(visible = selectedTab == "Login") {

                        Spacer(Modifier.height(paddingScale * 0.07f))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextButton(onClick = onNavigateToForgotPassword) {
                                Text(
                                    "Forgot Password?",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = (paddingScale * 0.035f).value.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    if (loginViewModel.loginState == LoginStatus.ERROR || loginViewModel.loginState == LoginStatus.TIMEOUT) {
                        Spacer(Modifier.height(paddingScale * 0.035f))
                        Text(
                            text = loginViewModel.errorMessage ?: "Unknown error occurred",
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

@Composable
fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    paddingScale: Dp,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (paddingScale * 0.04f).value.sp
                )
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(paddingScale * 0.03f)
            ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (paddingScale * 0.04f).value.sp
        ),
        shape = RoundedCornerShape(paddingScale * 0.03f),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        isError = error != null,
        supportingText = error?.let { 
            { 
                Text(
                    it, 
                    color = MaterialTheme.colorScheme.error,
                    fontSize = (paddingScale * 0.03f).value.sp
                ) 
            } 
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            errorBorderColor = MaterialTheme.colorScheme.error
        )
    )
}
