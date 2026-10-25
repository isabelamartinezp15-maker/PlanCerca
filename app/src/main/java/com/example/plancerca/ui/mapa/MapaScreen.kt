package com.example.plancerca.ui.mapa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.plancerca.R
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

@Composable
fun MapaScreen(onLugarClick: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val estadoUbicacion = rememberUbicacion()
    val miUbicacion = estadoUbicacion.ubicacion

    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(4.6486, -74.0628), 12f)
    }

    var zonaNueva by remember { mutableStateOf(false) }

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
            properties = MapProperties(
                isMyLocationEnabled = estadoUbicacion.tienePermiso,
                mapStyleOptions = estilo
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = estadoUbicacion.tienePermiso
            )
        ) {
            LugaresRepositorio.lugaresMapa.forEach { lugar ->
                key(lugar.id) {
                    Marker(
                        state = rememberUpdatedMarkerState(
                            position = LatLng(lugar.latitud, lugar.longitud)
                        ),
                        title = lugar.nombre,
                        snippet = "${lugar.categoria} · toca para ver más",
                        icon = BitmapDescriptorFactory.defaultMarker(colorMarcador(lugar.categoria)),
                        onInfoWindowClick = { onLugarClick(lugar.id) }
                    )
                }
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
                    .statusBarsPadding()
                    .padding(16.dp)
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

// Color del marcador según el tipo de lugar
private fun colorMarcador(categoria: String): Float = when {
    categoria.contains("caf", ignoreCase = true) -> BitmapDescriptorFactory.HUE_YELLOW
    categoria.contains("bar", ignoreCase = true) -> BitmapDescriptorFactory.HUE_VIOLET
    categoria.contains("restaur", ignoreCase = true) -> BitmapDescriptorFactory.HUE_ORANGE
    else -> BitmapDescriptorFactory.HUE_AZURE
}