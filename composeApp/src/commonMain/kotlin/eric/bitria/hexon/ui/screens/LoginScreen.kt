package eric.bitria.hexon.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.viewmodel.LoginState
import eric.bitria.hexon.viewmodel.LoginViewModel

data class AuthColors(
    val primary: Color = Color(0xFF1193D4),
    val inputBg: Color = Color(0x802C3E44),
    val textPlaceholder: Color = Color(0xFF9DB0B9),
    val divider: Color = Color(0x80999999),
    val buttonGradient: Brush = Brush.linearGradient(
        listOf(Color(0xFF13A0E8), Color(0xFF0F87C1))
    ),
    val backgroundGradient: Brush = Brush.verticalGradient(
        listOf(Color(0xFF111618), Color(0xFF0D1214), Color(0xFF050708))
    )
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel { LoginViewModel() },
    onLoginSuccess: () -> Unit,
    colors: AuthColors = AuthColors()
) {
    val email = loginViewModel.email
    val name = loginViewModel.name
    val password = loginViewModel.password
    val confirmPassword = loginViewModel.confirmPassword
    val loginState = loginViewModel.loginState

    var selectedTab by remember { mutableStateOf("Login") }

    // Handle login success
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundGradient)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Hexon",
                style = typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Toggle Login / Register
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth(0.8f)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Login", "Register").forEach { tab ->
                    val selected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (selected) colors.primary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            style = typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else colors.textPlaceholder
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Name (Register only) ---
            AnimatedVisibility(visible = selectedTab == "Register") {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { loginViewModel.onNameChange(it) },
                        placeholder = { Text("Name", color = colors.textPlaceholder) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(colors.inputBg, RoundedCornerShape(12.dp)),
                        singleLine = true,
                        textStyle = typography.bodyLarge.copy(color = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // --- Email ---
            OutlinedTextField(
                value = email,
                onValueChange = { loginViewModel.onEmailChange(it) },
                placeholder = { Text("Email", color = colors.textPlaceholder) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .background(colors.inputBg, RoundedCornerShape(12.dp)),
                singleLine = true,
                textStyle = typography.bodyLarge.copy(color = Color.White),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(12.dp))

            // --- Password ---
            OutlinedTextField(
                value = password,
                onValueChange = { loginViewModel.onPasswordChange(it) },
                placeholder = { Text("Password", color = colors.textPlaceholder) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .background(colors.inputBg, RoundedCornerShape(12.dp)),
                singleLine = true,
                textStyle = typography.bodyLarge.copy(color = Color.White),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(12.dp))

            // --- Confirm Password (Register only) ---
            AnimatedVisibility(visible = selectedTab == "Register") {
                Column {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { loginViewModel.onConfirmPasswordChange(it) },
                        placeholder = { Text("Confirm Password", color = colors.textPlaceholder) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(colors.inputBg, RoundedCornerShape(12.dp)),
                        singleLine = true,
                        textStyle = typography.bodyLarge.copy(color = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // --- Action Button ---
            Button(
                onClick = {
                    if (selectedTab == "Login") loginViewModel.loginWithEmail()
                    else { loginViewModel.registerWithEmail() }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.buttonGradient, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (loginState == LoginState.Loading || loginState == LoginState.Success) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) },
                            label = "ButtonTextAnimation"
                        ) { tab ->
                            Text(
                                tab,
                                style = typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Divider + OAuth buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = DividerDefaults.Thickness,
                    color = colors.divider
                )
                Text(
                    "Or continue with",
                    style = typography.bodySmall.copy(color = colors.textPlaceholder),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = DividerDefaults.Thickness,
                    color = colors.divider
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                OutlinedButton(
                    onClick = { loginViewModel.continueWithGoogle() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text("Google", fontWeight = FontWeight.Bold)
                }
            }

            if (loginState is LoginState.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = loginState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = typography.bodyMedium
                )
            }
        }
    }
}
