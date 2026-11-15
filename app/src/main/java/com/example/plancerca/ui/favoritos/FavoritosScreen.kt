package com.example.plancerca.ui.favoritos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button // NUEVO
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // NUEVO
import androidx.compose.runtime.getValue // NUEVO
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.plancerca.data.FavoritosRepositorio
import com.example.plancerca.data.supabase // NUEVO
import com.example.plancerca.ui.explorar.LugarCard
import io.github.jan.supabase.auth.auth // NUEVO
import io.github.jan.supabase.auth.status.SessionStatus // NUEVO

@Composable
fun FavoritosScreen(
    onLugarClick: (String) -> Unit,
    onIrACuenta: () -> Unit = {} // NUEVO
) {
    // NUEVO: ¿hay sesión iniciada?
    val estadoSesion by supabase.auth.sessionStatus.collectAsState()
    val haySesion = estadoSesion is SessionStatus.Authenticated
    val cargandoSesion = estadoSesion is SessionStatus.Initializing

    val favoritos = FavoritosRepositorio.favoritos

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Tus favoritos",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lugares que guardaste para visitar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        if (!haySesion && !cargandoSesion) {
            // NUEVO: sin sesión se pide iniciar sesión
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Inicia sesión para ver y guardar tus favoritos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onIrACuenta) {
                        Text("Ir a Cuenta")
                    }
                }
            }
        } else if (haySesion && favoritos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no tienes favoritos.\nToca el corazón en un lugar para guardarlo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (haySesion) {
            items(favoritos, key = { it.id }) { lugar ->
                LugarCard(lugar = lugar, onClick = { onLugarClick(lugar.id) })
            }
        }
    }
}