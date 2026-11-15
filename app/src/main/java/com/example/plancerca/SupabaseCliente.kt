
package com.example.plancerca.data

import com.example.plancerca.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

// Cliente único de Supabase para toda la app.
// La URL y la clave salen de local.properties (no se suben a GitHub).
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_KEY
) {
    install(Auth)       // registro e inicio de sesión
    install(Postgrest)  // tablas: reseñas y favoritos
    install(Storage)    // fotos de las reseñas
}