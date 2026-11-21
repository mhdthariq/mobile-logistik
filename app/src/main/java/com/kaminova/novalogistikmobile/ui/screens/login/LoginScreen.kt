package com.kaminova.novalogistikmobile.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaminova.novalogistikmobile.R
import com.kaminova.novalogistikmobile.ui.components.ErrorBanner
import com.kaminova.novalogistikmobile.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val canSubmit = state.username.isNotBlank() && state.password.isNotBlank() && !state.isLoading

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoginSuccess()
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(Modifier.widthIn(max = 432.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 36.dp, bottomEnd = 12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = MaterialShapes.Cookie6Sided.toShape(),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(34.dp))
                        }
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Masuk", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                TextField(
                    value = state.corpId, onValueChange = viewModel::onCorpIdChange,
                    label = { Text("Corporate ID") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                    keyboardActions = next, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = state.username, onValueChange = viewModel::onUsernameChange,
                    label = { Text("Nama pengguna") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = next, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = state.password, onValueChange = viewModel::onPasswordChange,
                    label = { Text("Kata sandi") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::togglePasswordVisibility, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                if (state.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (state.passwordVisible) "Sembunyikan kata sandi" else "Tampilkan kata sandi"
                            )
                        }
                    },
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canSubmit) {
                            focus.clearFocus()
                            viewModel.login()
                        }
                    }),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                state.error?.let { ErrorBanner(message = it) }
                PrimaryButton(
                    label = "Masuk", loading = state.isLoading, enabled = canSubmit,
                    onClick = { focus.clearFocus(); viewModel.login() },
                    modifier = Modifier.heightIn(min = 58.dp)
                )
            }
        }
    }
}
