package com.example.plancerca.ui.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.plancerca.R
import com.example.plancerca.data.Lugar
import com.example.plancerca.data.LugaresRepositorio
import com.example.plancerca.ui.ubicacion.rememberUbicacion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch

// Botones de categoría del mapa. "tipo" es el tipo con el que se buscó el lugar en Google;
// "texto" sirve de respaldo cuando el lugar no trae tipo. "matiz" es el color del marcador.
private class Filtro(
    val etiqueta: String,
    val tipo: String?,
    val matiz: Float,
    val texto: (String) -> Boolean
) {
    fun coincide(lugar: Lugar): Boolean = when {
        tipo == null -> true
        lugar.tipo.isNotBlank() -> lugar.tipo == tipo
        else -> texto(lugar.categoria)
    }
}

private val FILTROS = listOf(
    Filtro("Todos", null, BitmapDescriptorFactory.HUE_AZURE) { true },
    Filtro("Restaurantes", "restaurant", BitmapDescriptorFactory.HUE_ORANGE) {
        it.contains("restaur", ignoreCase = true)
    },
    Filtro("Cafeterías", "cafe", BitmapDescriptorFactory.HUE_YELLOW) {
        it.contains("caf", ignoreCase = true)
    },
    Filtro("Bares", "bar", BitmapDescriptorFactory.HUE_VIOLET) {
        it.equals("bar", ignoreCase = true) || it.contains("pub", ignoreCase = true)
    },
    Filtro("Parques", "park", BitmapDescriptorFactory.HUE_GREEN) {
        it.contains("parque", ignoreCase = true) || it.contains("park", ignoreCase = true)
    },
    Filtro("Atracciones", "tourist_attraction", BitmapDescriptorFactory.HUE_ROSE) {
        it.contains("atracci", ignoreCase = true) || it.contains("turíst", ignoreCase = true) ||
                it.contains("turist", ignoreCase = true)
    }
)

// Color del marcador según el tipo de lugar
private fun matizDe(lugar: Lugar): Float =
    FILTROS.drop(1).firstOrNull { it.coincide(lugar) }?.matiz ?: BitmapDescriptorFactory.HUE_AZURE

@Composable
fun MapaScreen(onLugarClick: (String) -> Unit) {
    val context = LocalContext.current
    val densidad = LocalDensity.current
    val scope = rememberCoroutineScope()
    val estadoUbicacion = rememberUbicacion()
    val miUbicacion = estadoUbicacion.ubicacion

    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(4.6486, -74.0628), 12f)
    }

    var zonaNueva by remember { mutableStateOf(false) }
    var filtroSeleccionado by rememberSaveable { mutableStateOf(FILTROS.first().etiqueta) }

    // Alto del panel de arriba, para que los controles del mapa no queden tapados
    var alturaPanel by remember { mutableStateOf(0.dp) }

    val filtroActual = FILTROS.firstOrNull { it.etiqueta == filtroSeleccionado } ?: FILTROS.first()
    val lugaresVisibles = LugaresRepositorio.lugaresMapa.filter { filtroActual.coincide(it) }

    // Cuando llega tu ubicación: centra el mapa y carga los lugares de la zona
    LaunchedEffect(miUbicacion) {
        if (miUbicacion != null) {
            val posicion = LatLng(miUbicacion.latitude, miUbicacion.longitude)
            camara.animate(CameraUpdateFactory.newLatLngZoom(posicion, 14f))
            LugaresRepositorio.cargarMapa(context, posicion.latitude, posicion.longitude)
            zonaNueva = false
        }
    }

    // Muestra "Buscar en esta zona" solo cuando el usuario mueve el mapa con el dedo
    LaunchedEffect(camara.isMoving) {
        if (camara.isMoving && camara.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            zonaNueva = true
        }
    }

    val estilo = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.mapa_estilo) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camara,
            contentPadding = PaddingValues(top = alturaPanel),
            properties = MapProperties(
                isMyLocationEnabled = estadoUbicacion.tienePermiso,
                mapStyleOptions = estilo
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = estadoUbicacion.tienePermiso
            )
        ) {
            lugaresVisibles.forEach { lugar ->
                key(lugar.id) {
                    Marker(
                        state = rememberUpdatedMarkerState(
                            position = LatLng(lugar.latitud, lugar.longitud)
                        ),
                        title = lugar.nombre,
                        snippet = "${lugar.categoria} · toca para ver más",
                        icon = BitmapDescriptorFactory.defaultMarker(matizDe(lugar)),
                        onInfoWindowClick = { onLugarClick(lugar.id) }
                    )
                }
            }
        }

        // Panel de arriba: botones de filtro y contador de lugares
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onGloballyPositioned { alturaPanel = with(densidad) { it.size.height.toDp() } }
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FILTROS) { filtro ->
                        FilterChip(
                            selected = filtro.etiqueta == filtroSeleccionado,
                            onClick = { filtroSeleccionado = filtro.etiqueta },
                            label = { Text(filtro.etiqueta) },
                            leadingIcon = if (filtro.tipo != null) {
                                { PuntoDeColor(filtro.matiz) }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Text(
                    text = when {
                        LugaresRepositorio.cargandoMapa -> "Buscando lugares…"
                        lugaresVisibles.isEmpty() ->
                            "Sin lugares de este tipo aquí. Mueve el mapa y toca Buscar en esta zona."
                        lugaresVisibles.size == 1 -> "1 lugar en esta zona"
                        else -> "${lugaresVisibles.size} lugares en esta zona"
                    },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (zonaNueva || LugaresRepositorio.cargandoMapa) {
            Button(
                onClick = {
                    scope.launch {
                        val centro = camara.position.target
                        LugaresRepositorio.cargarMapa(context, centro.latitude, centro.longitude)
                        zonaNueva = false
                    }
                },
                enabled = !LugaresRepositorio.cargandoMapa,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = alturaPanel + 8.dp)
            ) {
                Text(if (LugaresRepositorio.cargandoMapa) "Buscando lugares…" else "Buscar en esta zona")
            }
        }

        if (!estadoUbicacion.tienePermiso) {
            Button(
                onClick = estadoUbicacion.solicitarPermiso,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Text("Activar mi ubicación")
            }
        }
    }
}

// Puntico del mismo color que los marcadores de esa categoría
@Composable
private fun PuntoDeColor(matiz: Float) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(Color.hsv(matiz, 0.85f, 0.95f), CircleShape)
    )
}