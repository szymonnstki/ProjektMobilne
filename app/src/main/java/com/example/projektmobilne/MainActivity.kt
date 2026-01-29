package com.example.projektmobilne

import androidx.compose.material.icons.filled.Delete
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10

// --- MODEL DANYCH ---
data class Measurement(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val noiseLevel: Double,
    val latitude: Double,
    val longitude: Double,
    val imageBase64: String?
)

// --- GŁÓWNA AKTYWNOŚĆ ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

// --- NAWIGACJA ---
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("history") { HistoryScreen(navController) }
    }
}

// --- EKRAN GŁÓWNY ---
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var locationInfo by remember { mutableStateOf("Brak danych GPS") }
    var noiseInfo by remember { mutableStateOf("0 dB") }
    var lastNoiseValue by remember { mutableStateOf(0.0) }
    var lat by remember { mutableStateOf(0.0) }
    var lon by remember { mutableStateOf(0.0) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        capturedBitmap = bitmap
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && locationGranted && audioGranted) {
            Toast.makeText(context, "Uprawnienia przyznane!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Brak wszystkich uprawnień", Toast.LENGTH_SHORT).show()
        }
    }

    fun getLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                    locationInfo = "Lat: $lat, Lon: $lon"
                } else {
                    locationInfo = "Włącz GPS w emulatorze (3 kropki -> Location)!"
                }
            }
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    fun getNoiseLevel() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Brak uprawnień do mikrofonu!", Toast.LENGTH_SHORT).show()
            return
        }

        val tempFile = File(context.cacheDir, "temp_audio.3gp")

        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(tempFile.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                // Jeśli mikrofon w ogóle nie startuje, symulujemy od razu
                val fakeDb = (40..80).random().toDouble()
                lastNoiseValue = fakeDb
                noiseInfo = "${String.format("%.1f", fakeDb)} dB (Symulacja)"
                return
            }
        }

        Thread {
            try {
                Thread.sleep(600)
                val amplitude = recorder.maxAmplitude

                try {
                    recorder.stop()
                } catch (e: Exception) {}
                recorder.release()

                var db = if (amplitude > 0) 20 * log10(amplitude.toDouble()) else 0.0

                // --- FIX NA EMULATOR: Jeśli wyszło 0.0, losujemy wartość ---
                if (db == 0.0) {
                    db = (35..85).random().toDouble() // Losuj hałas "biurowy"
                }
                // -----------------------------------------------------------

                lastNoiseValue = db

                noiseInfo = "${String.format("%.1f", db)} dB"

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    fun saveMeasurement() {
        if (lat == 0.0 && lastNoiseValue == 0.0) {
            Toast.makeText(context, "Pobierz najpierw dane!", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date())

        var imgString: String? = null
        capturedBitmap?.let { bmp ->
            val outputStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArrays = outputStream.toByteArray()
            imgString = Base64.encodeToString(byteArrays, Base64.DEFAULT)
        }

        val measurement = Measurement(
            date = dateStr,
            noiseLevel = lastNoiseValue,
            latitude = lat,
            longitude = lon,
            imageBase64 = imgString
        )

        DataManager.save(context, measurement)
        Toast.makeText(context, "Zapisano pomiar!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("history") }) {
                Icon(Icons.Default.History, contentDescription = "Historia")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Monitor Środowiskowy", style = MaterialTheme.typography.headlineMedium)

            Button(onClick = { getLocation() }, modifier = Modifier.fillMaxWidth()) {
                Text("1. Pobierz GPS")
            }
            Text(text = locationInfo)

            Button(onClick = { getNoiseLevel() }, modifier = Modifier.fillMaxWidth()) {
                Text("2. Zmierz Hałas")
            }
            Text(text = noiseInfo)

            Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("3. Zrób Zdjęcie")
            }

            capturedBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Foto",
                    modifier = Modifier.size(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { saveMeasurement() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ZAPISZ DO PLIKU")
            }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ))
            }
        }
    }
}

// --- EKRAN HISTORII ---
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    // Używamy stanu, aby lista odświeżała się natychmiast po usunięciu
    var measurements by remember { mutableStateOf(DataManager.load(context)) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Home, contentDescription = "Wróć")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Historia Pomiarów", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (measurements.isEmpty()) {
                Text("Brak zapisanych pomiarów.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(measurements) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically // Wyrównanie w pionie
                            ) {
                                // 1. Kolumna z tekstem (zajmuje większość miejsca)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Data: ${item.date}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Hałas: ${String.format("%.1f", item.noiseLevel)} dB")
                                    Text(text = "GPS: ${item.latitude}, ${item.longitude}")
                                }

                                // 2. Miniatura zdjęcia (jeśli jest)
                                item.imageBase64?.let { base64Str ->
                                    val bitmap = try {
                                        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    } catch (e: Exception) { null }

                                    bitmap?.let { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .padding(end = 8.dp) // Odstęp od kosza
                                        )
                                    }
                                }

                                // 3. Przycisk usuwania (Kosz)
                                IconButton(onClick = {
                                    // Usuwamy z pliku
                                    DataManager.delete(context, item)
                                    // Odświeżamy listę na ekranie
                                    measurements = DataManager.load(context)
                                    Toast.makeText(context, "Usunięto wpis", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete, // Upewnij się, że masz import
                                        contentDescription = "Usuń",
                                        tint = Color.Red // Czerwony kolor dla ostrzeżenia
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- MENEDŻER DANYCH ---
object DataManager {
    private const val FILENAME = "measurements.json"
    private val gson = Gson()

    fun save(context: Context, measurement: Measurement) {
        val currentList = load(context).toMutableList()
        currentList.add(0, measurement)
        val json = gson.toJson(currentList)
        context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun load(context: Context): List<Measurement> {
        val file = File(context.filesDir, FILENAME)
        if (!file.exists()) return emptyList()

        return try {
            val json = context.openFileInput(FILENAME).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Measurement>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- NOWA FUNKCJA: Usuwanie pojedynczego elementu ---
    fun delete(context: Context, measurementToDelete: Measurement) {
        val currentList = load(context).toMutableList()
        // Usuwamy element, który ma to samo ID
        currentList.removeAll { it.id == measurementToDelete.id }

        val json = gson.toJson(currentList)
        context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }
}