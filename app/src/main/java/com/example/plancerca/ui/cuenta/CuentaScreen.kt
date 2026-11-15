package com.example.plancerca.ui.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.plancerca.data.AuthRepositorio
import com.example.plancerca.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

@Composable
fun CuentaScreen() {
    val estado by supabase.auth.sessionStatus.collectAsState()
    val scope = rememberCoroutineScope()

    var modoRegistro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (estado) {
            is SessionStatus.Initializing -> CircularProgressIndicator()

            is SessionStatus.Authenticated -> {
                Text("Tu cuenta", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                val nombreActual = AuthRepositorio.nombreActual()
                if (!nombreActual.isNullOrBlank()) {
                    Text("Hola, $nombreActual")
                }
                Text("Sesión iniciada como ${AuthRepositorio.correoActual() ?: ""}")
                Spacer(Modifier.height(8.dp))
                Text("Ya puedes guardar favoritos y publicar reseñas con fotos.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = { scope.launch { AuthRepositorio.cerrarSesion() } }) {
                    Text("Cerrar sesión")
                }
            }

            else -> {
                Text(
                    if (modoRegistro) "Crear cuenta" else "Iniciar sesión",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(8.dp))
                Text("Con tu cuenta puedes guardar favoritos y publicar reseñas con fotos.")
                Spacer(Modifier.height(16.dp))

                if (modoRegistro) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña (mínimo 6 caracteres)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                mensajeError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = !cargando &&
                            correo.isNotBlank() &&
                            contrasena.isNotBlank() &&
                            (!modoRegistro || nombre.isNotBlank()),
                    onClick = {
                        scope.launch {
                            cargando = true
                            mensajeError = null
                            try {
                                if (modoRegistro) {
                                    AuthRepositorio.registrar(nombre.trim(), correo.trim(), contrasena)
                                } else {
                                    AuthRepositorio.iniciarSesion(correo.trim(), contrasena)
                                }
                            } catch (e: Exception) {
                                mensajeError = traducirError(e)
                            }
                            cargando = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            cargando -> "Un momento..."
                            modoRegistro -> "Registrarme"
                            else -> "Entrar"
                        }
                    )
                }

                TextButton(onClick = {
                    modoRegistro = !modoRegistro
                    mensajeError = null
                }) {
                    Text(
                        if (modoRegistro) "¿Ya tienes cuenta? Inicia sesión"
                        else "¿No tienes cuenta? Regístrate"
                    )
                }
            }
        }
    }
}

private fun traducirError(e: Exception): String {
    val texto = e.message.orEmpty()
    return when {
        texto.contains("Invalid login credentials", ignoreCase = true) ->
            "Correo o contraseña incorrectos."
        texto.contains("already registered", ignoreCase = true) ->
            "Ese correo ya está registrado. Prueba iniciando sesión."
        texto.contains("at least", ignoreCase = true) ->
            "La contraseña debe tener al menos 6 caracteres."
        texto.contains("valid email", ignoreCase = true) ||
                texto.contains("invalid format", ignoreCase = true) ->
            "Escribe un correo válido."
        texto.contains("Unable to resolve host", ignoreCase = true) ||
                texto.contains("timeout", ignoreCase = true) ->
            "No hay conexión a internet. Inténtalo de nuevo."
        else -> "No se pudo completar. Inténtalo de nuevo."
    }
}