package com.example.plancerca.ui.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plancerca.data.FavoritosRepositorio
import com.example.plancerca.data.Lugar
import com.example.plancerca.data.LugaresRepositorio
import com.example.plancerca.ui.ubicacion.distanciaMetros
import com.example.plancerca.ui.ubicacion.formatearDistancia
import com.example.plancerca.ui.ubicacion.rememberUbicacion

// Botones de categoría. "tipo" es el tipo con el que se buscó el lugar en Google;
// "texto" sirve de respaldo para los lugares de ejemplo, que no tienen tipo.
private class Filtro(
    val etiqueta: String,
    val tipo: String?,
    val texto: (String) -> Boolean
) {
    fun coincide(lugar: Lugar): Boolean = when {
        tipo == null -> true
        lugar.tipo.isNotBlank() -> lugar.tipo == tipo
        else -> texto(lugar.categoria)
    }
}

private val FILTROS = listOf(
    Filtro("Todos", null) { true },
    Filtro("Restaurantes", "restaurant") { it.contains("restaur", ignoreCase = true) },
    Filtro("Cafeterías", "cafe") { it.contains("caf", ignoreCase = true) },
    Filtro("Bares", "bar") { it.equals("bar", ignoreCase = true) || it.contains("pub", ignoreCase = true) },
    Filtro("Parques", "park") {
        it.contains("parque", ignoreCase = true) || it.contains("park", ignoreCase = true)
    },
    Filtro("Atracciones", "tourist_attraction") {
        it.contains("atracci", ignoreCase = true) || it.contains("turíst", ignoreCase = true) ||
                it.contains("turist", ignoreCase = true)
    }
)

@Composable
fun ExplorarScreen(onLugarClick: (String) -> Unit) {
    val context = LocalContext.current
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtroSeleccionado by rememberSaveable { mutableStateOf(FILTROS.first().etiqueta) }

    val estadoUbicacion = rememberUbicacion()
    val miUbicacion = estadoUbicacion.ubicacion

    // Cuando llega el GPS, buscamos los lugares reales cercanos
    LaunchedEffect(miUbicacion) {
        if (miUbicacion != null) {
            LugaresRepositorio.cargarCercanos(context, miUbicacion.latitude, miUbicacion.longitude)
        }
    }

    val filtroActual = FILTROS.firstOrNull { it.etiqueta == filtroSeleccionado } ?: FILTROS.first()

    val lugaresFiltrados = LugaresRepositorio.lugares.filter { lugar ->
        filtroActual.coincide(lugar) &&
                lugar.nombre.contains(busqueda.trim(), ignoreCase = true)
    }

    val lugaresOrdenados = if (miUbicacion != null) {
        lugaresFiltrados.sortedBy { distanciaMetros(miUbicacion, it) }
    } else {
        lugaresFiltrados
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Descubre",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lugares cerca de ti",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        if (!estadoUbicacion.tienePermiso) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Activa tu ubicación para ver los lugares más cercanos",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(onClick = estadoUbicacion.solicitarPermiso) {
                            Text("Activar")
                        }
                    }
                }
            }
        } else if (miUbicacion == null) {
            item {
                Text(
                    text = "Buscando tu ubicación… revisa que el GPS esté activado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        if (LugaresRepositorio.cargando) {
            item {
                Text(
                    text = "Buscando lugares cerca de ti…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        val aviso = LugaresRepositorio.mensaje
        if (aviso != null) {
            item {
                Text(
                    text = aviso,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        item {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FILTROS) { filtro ->
                    FilterChip(
                        selected = filtro.etiqueta == filtroSeleccionado,
                        onClick = { filtroSeleccionado = filtro.etiqueta },
                        label = { Text(filtro.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        if (lugaresOrdenados.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No encontramos lugares con ese nombre o categoría",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            items(lugaresOrdenados, key = { it.id }) { lugar ->
                val distancia = miUbicacion?.let { formatearDistancia(distanciaMetros(it, lugar)) }
                LugarCard(
                    lugar = lugar,
                    distancia = distancia,
                    onClick = { onLugarClick(lugar.id) }
                )
            }
        }
    }
}

@Composable
fun LugarCard(
    lugar: Lugar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    distancia: String? = null
) {
    val context = LocalContext.current
    val esFavorito = FavoritosRepositorio.esFavorito(lugar.id)

    // La foto se pide solo cuando la tarjeta aparece en pantalla
    var foto by remember(lugar.id) { mutableStateOf(lugar.imagenUrl) }
    LaunchedEffect(lugar.id) {
        if (foto.isBlank()) {
            foto = LugaresRepositorio.fotoDe(context, lugar.id)
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(260.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
        ) {
            AsyncImage(
                model = foto.ifBlank { null },
                contentDescription = lugar.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )
            IconButton(
                onClick = { FavoritosRepositorio.alternar(lugar.copy(imagenUrl = foto)) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (esFavorito) "Quitar de favoritos" else "Guardar en favoritos",
                    tint = if (esFavorito) MaterialTheme.colorScheme.primary else Color.White
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    if (distancia != null) {
                        Text(
                            text = "a $distancia",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = lugar.nombre,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = lugar.direccion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}