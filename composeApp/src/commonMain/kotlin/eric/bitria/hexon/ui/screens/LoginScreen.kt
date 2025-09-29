package eric.bitria.hexon.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.theme.HexonTheme
import eric.bitria.hexon.viewmodel.LoginState
import eric.bitria.hexon.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel { LoginViewModel() },
    onLoginSuccess: () -> Unit
) {
    HexonTheme {
        val email = loginViewModel.email
        val name = loginViewModel.name
        val password = loginViewModel.password
        val confirmPassword = loginViewModel.confirmPassword
        val loginState = loginViewModel.loginState

        var selectedTab by remember { mutableStateOf("Login") }

        LaunchedEffect(loginState) {
            if (loginState is LoginState.Success) onLoginSuccess()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // Toggle Login / Register
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(0.8f)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Login", "Register").forEach { tab ->
                        val selected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(
                                    elevation = if (selected) 6.dp else 0.dp,
                                    shape = if (selected) RoundedCornerShape(6.dp) else RoundedCornerShape(0.dp)
                                )
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tab,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
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
                            placeholder = { Text("Name", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
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
                    placeholder = { Text(
                        text = "Email",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyLarge
                    ) },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(12.dp))

                // --- Password ---
                OutlinedTextField(
                    value = password,
                    onValueChange = { loginViewModel.onPasswordChange(it) },
                    placeholder = { Text("Password", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
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
                            placeholder = { Text("Confirm Password", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
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
                    contentPadding = PaddingValues(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(12.dp)
                            ),
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
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "ButtonTextAnimation"
                            ) { tab ->
                                Text(
                                    tab,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
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
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Or continue with",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.onBackground
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
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Text("Google", fontWeight = FontWeight.Bold)
                    }
                }

                if (loginState is LoginState.Error) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = loginState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
