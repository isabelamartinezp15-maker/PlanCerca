package com.example.plancerca.ui.detalle

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plancerca.data.FavoritosRepositorio
import com.example.plancerca.data.Lugar
import com.example.plancerca.data.LugaresRepositorio

@Composable
fun DetalleScreen(lugarId: String, onBack: () -> Unit) {
    // Busca el lugar en Explorar, en el Mapa y, si no está, en los favoritos guardados
    val lugar = LugaresRepositorio.lugares.firstOrNull { it.id == lugarId }
        ?: LugaresRepositorio.lugaresMapa.firstOrNull { it.id == lugarId }
        ?: FavoritosRepositorio.favoritos.firstOrNull { it.id == lugarId }
    val context = LocalContext.current

    if (lugar == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No encontramos este lugar", color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val esFavorito = FavoritosRepositorio.esFavorito(lugar.id)

        // Los lugares del mapa no traen foto: se pide al abrir el detalle
        var foto by remember(lugar.id) { mutableStateOf(lugar.imagenUrl) }
        LaunchedEffect(lugar.id) {
            if (foto.isBlank()) {
                foto = LugaresRepositorio.fotoDe(context, lugar.id)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
            ) {
                AsyncImage(
                    model = foto.ifBlank { null },
                    contentDescription = lugar.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { FavoritosRepositorio.alternar(lugar.copy(imagenUrl = foto)) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (esFavorito) "Quitar de favoritos" else "Guardar en favoritos",
                        tint = if (esFavorito) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
            Column(modifier = Modifier.padding(24.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = lugar.categoria,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = lugar.nombre,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lugar.direccion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Button(
                    onClick = { abrirIndicaciones(context, lugar) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cómo llegar")
                }
            }
        }
    }
}

private fun abrirIndicaciones(context: Context, lugar: Lugar) {
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=${lugar.latitud},${lugar.longitud}"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        // Sin app de mapas ni navegador: no hacemos nada
    }
}