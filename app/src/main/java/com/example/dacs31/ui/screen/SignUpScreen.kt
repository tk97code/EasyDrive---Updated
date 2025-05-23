package com.example.dacs31.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dacs31.R
import com.example.dacs31.data.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    authRepository: AuthRepository,
    viewModel: SignUpViewModel = viewModel(factory = SignUpViewModelFactory(authRepository))
) {
    // Observe LiveData from ViewModel
    val name by viewModel.name.observeAsState(TextFieldValue())
    val email by viewModel.email.observeAsState(TextFieldValue())
    val password by viewModel.password.observeAsState(TextFieldValue())
    val passwordVisible by viewModel.passwordVisible.observeAsState(false)
    val gender by viewModel.gender.observeAsState("")
    val selectedRole by viewModel.selectedRole.observeAsState("Customer")
    val isTermsAccepted by viewModel.isTermsAccepted.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState(null)
    val isLoading by viewModel.isLoading.observeAsState(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Sign up",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading,
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        var expandedGender by remember { mutableStateOf(false) }
        val genderOptions = listOf("Male", "Female", "Other")
        ExposedDropdownMenuBox(
            expanded = expandedGender,
            onExpandedChange = { expandedGender = !expandedGender }
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            )
            ExposedDropdownMenu(
                expanded = expandedGender,
                onDismissRequest = { expandedGender = false }
            ) {
                genderOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.updateGender(option)
                            expandedGender = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var expandedRole by remember { mutableStateOf(false) }
        val roleOptions = listOf("Customer", "Driver")
        ExposedDropdownMenuBox(
            expanded = expandedRole,
            onExpandedChange = { expandedRole = !expandedRole }
        ) {
            OutlinedTextField(
                value = selectedRole,
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRole)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            )
            ExposedDropdownMenu(
                expanded = expandedRole,
                onDismissRequest = { expandedRole = false }
            ) {
                roleOptions.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role) },
                        onClick = {
                            viewModel.updateRole(role)
                            expandedRole = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTermsAccepted,
                onCheckedChange = { viewModel.updateTermsAccepted(it) },
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "By signing up, you agree to the Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.signUp { result ->
                    if (result.isSuccess) {
                        navController.navigate("signin")
                    }
                }
            },
            enabled = isTermsAccepted && name.text.isNotBlank() && email.text.isNotBlank() && password.text.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign Up", modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { /* TODO: Google login */ }, enabled = !isLoading) {
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = "Sign up with Google",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { /* TODO: Facebook login */ }, enabled = !isLoading) {
                Icon(
                    painter = painterResource(R.drawable.ic_facebook),
                    contentDescription = "Sign up with Facebook",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { /* TODO: Apple login */ }, enabled = !isLoading) {
                Icon(
                    painter = painterResource(R.drawable.ic_apple),
                    contentDescription = "Sign up with Apple",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { navController.navigate("signin") }, enabled = !isLoading) {
                Text(
                    text = "Sign in",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}