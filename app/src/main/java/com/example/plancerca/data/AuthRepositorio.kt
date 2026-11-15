package com.example.plancerca.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// Registro, inicio y cierre de sesión con Supabase
object AuthRepositorio {

    suspend fun registrar(nombre: String, correo: String, contrasena: String) {
        supabase.auth.signUpWith(Email) {
            email = correo
            password = contrasena
            data = buildJsonObject { put("nombre", nombre) }
        }
    }

    suspend fun iniciarSesion(correo: String, contrasena: String) {
        supabase.auth.signInWith(Email) {
            email = correo
            password = contrasena
        }
    }

    suspend fun cerrarSesion() {
        supabase.auth.signOut()
    }

    fun correoActual(): String? = supabase.auth.currentUserOrNull()?.email

    fun nombreActual(): String? =
        supabase.auth.currentUserOrNull()
            ?.userMetadata?.get("nombre")?.jsonPrimitive?.contentOrNull
}
