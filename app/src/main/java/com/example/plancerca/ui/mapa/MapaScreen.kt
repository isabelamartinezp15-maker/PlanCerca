package com.example.plancerca.ui.mapa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MapaScreen(onLugarClick: (String) -> Unit) {
    val context = LocalContext.current
    val estadoUbicacion = rememberUbicacion()
    val miUbicacion = estadoUbicacion.ubicacion

    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(4.6486, -74.0628), 12f)
    }

    // Mueve la cámara a tu ubicación
    LaunchedEffect(miUbicacion) {
        if (miUbicacion != null) {
            camara.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(miUbicacion.latitude, miUbicacion.longitude), 14f
                )
            )
        }
    }

    // Busca los lugares reales cercanos
    LaunchedEffect(miUbicacion) {
        if (miUbicacion != null) {
            LugaresRepositorio.cargarCercanos(context, miUbicacion.latitude, miUbicacion.longitude)
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
            LugaresRepositorio.lugares.forEach { lugar ->
                key(lugar.id) {
                    Marker(
                        state = rememberMarkerState(
                            position = LatLng(lugar.latitud, lugar.longitud)
                        ),
                        title = lugar.nombre,
                        snippet = "${lugar.categoria} · toca para ver más",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        onInfoWindowClick = { onLugarClick(lugar.id) }
                    )
                }
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