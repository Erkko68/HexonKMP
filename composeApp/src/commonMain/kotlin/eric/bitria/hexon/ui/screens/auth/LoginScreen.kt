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
import androidx.compose.ui.unit.dp
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
        val dimensions = HexonTheme.dimensions
        val spacing = dimensions.spacing
        val shapes = dimensions.shapes
        val paddingScale = dimensions.paddingScale

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
                    text = "Hexon",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(spacing.medium))

                // Toggle Login / Register
                Row(
                    modifier = Modifier
                        .height(dimensions.listItemHeight)
                        .fillMaxWidth(contentWidth)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = shapes.medium
                        )
                        .padding(spacing.extraSmall),
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
                                    shape = shapes.small
                                )
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = shapes.small
                                )
                                .clickable { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tab,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(spacing.mediumLarge))

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
                                error = loginViewModel.nameError
                            )
                            Spacer(Modifier.height(spacing.small))
                        }
                    }

                    // --- Email ---
                    LoginInputField(
                        value = loginViewModel.email,
                        onValueChange = { loginViewModel.onEmailChange(it) },
                        placeholder = "Email",
                        error = loginViewModel.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(Modifier.height(spacing.small))

                    // --- Password ---
                    LoginInputField(
                        value = loginViewModel.password,
                        onValueChange = { loginViewModel.onPasswordChange(it) },
                        placeholder = "Password",
                        error = loginViewModel.passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(Modifier.height(spacing.small))

                    // --- Confirm Password (Register only) ---
                    AnimatedVisibility(visible = selectedTab == "Register") {

                        LoginInputField(
                            value = loginViewModel.confirmPassword,
                            onValueChange = { loginViewModel.onConfirmPasswordChange(it) },
                            placeholder = "Confirm Password",
                            error = loginViewModel.confirmPasswordError,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }

                    Spacer(Modifier.height(spacing.mediumLarge))

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
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(spacing.large))

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
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(horizontal = spacing.small)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    Spacer(Modifier.height(spacing.mediumSmall))

                    OutlinedButton(
                        onClick = { loginViewModel.continueWithGoogle() },
                        modifier = Modifier.fillMaxWidth().height(dimensions.listItemHeight),
                        shape = shapes.large,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        contentPadding = PaddingValues(vertical = spacing.small)
                    ) {
                        Text(
                            "Google", 
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // --- Forgot Password Button ---
                    AnimatedVisibility(visible = selectedTab == "Login") {

                        Spacer(Modifier.height(spacing.large))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextButton(onClick = onNavigateToForgotPassword) {
                                Text(
                                    "Forgot Password?",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    if (loginViewModel.loginState == LoginStatus.ERROR || loginViewModel.loginState == LoginStatus.TIMEOUT) {
                        Spacer(Modifier.height(spacing.mediumSmall))
                        Text(
                            text = loginViewModel.errorMessage ?: "Unknown error occurred",
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

@Composable
fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    val dimensions = HexonTheme.dimensions
    val shapes = dimensions.shapes

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = shapes.large
            ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = shapes.large,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        isError = error != null,
        supportingText = error?.let { 
            { 
                Text(
                    it, 
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
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
