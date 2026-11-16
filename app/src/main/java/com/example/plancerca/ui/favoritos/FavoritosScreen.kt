package com.example.plancerca.ui.favoritos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.plancerca.data.FavoritosRepositorio
import com.example.plancerca.data.supabase
import com.example.plancerca.ui.explorar.LugarCard
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun FavoritosScreen(
    onLugarClick: (String) -> Unit,
    onIrACuenta: () -> Unit = {},
    onExplorar: () -> Unit = {} // NUEVO: lleva a la pestaña Explorar
) {
    // ¿Hay sesión iniciada?
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
            Encabezado(cantidad = if (haySesion) favoritos.size else 0)
        }

        when {
            cargandoSesion -> Unit
            !haySesion -> item { SinSesion(onIrACuenta) }
            favoritos.isEmpty() -> item { SinFavoritos(onExplorar) }
            else -> items(favoritos, key = { it.id }) { lugar ->
                LugarCard(lugar = lugar, onClick = { onLugarClick(lugar.id) })
            }
        }
    }
}

// Título, subtítulo (con el contador) y un corazón decorativo
@Composable
private fun Encabezado(cantidad: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tus favoritos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = when {
                    cantidad == 1 -> "1 lugar guardado"
                    cantidad > 1 -> "$cantidad lugares guardados"
                    else -> "Lugares que guardaste para visitar"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Cuando no hay sesión iniciada
@Composable
private fun SinSesion(onIrACuenta: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Ilustracion(Icons.Default.Favorite)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Guarda los lugares que te encantan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Inicia sesión para crear tu lista de favoritos y tenerla siempre a la mano.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Beneficio(Icons.Default.Favorite, "Guarda restaurantes, cafés, bares y más")
                Beneficio(Icons.Default.Place, "Encuentra tus planes en un solo lugar")
                Beneficio(Icons.Default.RateReview, "Comparte reseñas con fotos")
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onIrACuenta,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Iniciar sesión o registrarme")
            }
        }
    }
}

// Cuando hay sesión pero todavía no hay favoritos
@Composable
private fun SinFavoritos(onExplorar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Ilustracion(Icons.Default.FavoriteBorder)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aún no tienes favoritos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Toca el corazón en cualquier lugar y aparecerá aquí.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onExplorar,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Default.Explore, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Explorar lugares")
        }
    }
}

// Ícono grande dentro de dos círculos suaves
@Composable
private fun Ilustracion(icono: ImageVector) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

// Una línea con un ícono en un círculo y un texto
@Composable
private fun Beneficio(icono: ImageVector, texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}