package com.example.aureum1.controller.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.aureum1.controller.fragments.AccountsFragment
import com.example.aureum1.controller.fragments.DebtsFragment
import com.example.aureum1.controller.fragments.ProfileFragment
import com.example.aureum1.R
import com.example.aureum1.controller.fragments.RecordFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val REQUEST_CHECK_SETTINGS = 200
    private lateinit var fused: com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var highAccReq: com.google.android.gms.location.LocationRequest
    private lateinit var settingsRequest: com.google.android.gms.location.LocationSettingsRequest
    private var locationCallback: com.google.android.gms.location.LocationCallback? = null
    private var locationOverlay: android.view.View? = null
    private var inicioCargado = false
    private var startTab: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startTab = intent?.getStringExtra("START_TAB")

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish(); return
        }

        Toast.makeText(
            this,
            "Hola, ${user.displayName ?: user.email ?: "Usuario"}",
            Toast.LENGTH_LONG
        ).show()

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    openFragment(AccountsFragment(), "ACCOUNTS")
                    true
                }
                else -> false
            }
        }

        //Home como ImageView
        findViewById<ImageView>(R.id.btnHome).setOnClickListener {
            openFragment(AccountsFragment(), "ACCOUNTS")

            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.nav_home
        }

        // Avatar → cerrar sesión
        findViewById<ImageView>(R.id.imgAvatar).setOnClickListener {
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { openFragment(AccountsFragment(), "ACCOUNTS"); true }
                R.id.nav_record -> { openFragment(RecordFragment(), "RECORD"); true }
                R.id.nav_debts -> { openFragment(DebtsFragment(), "DEBTS"); true }
                R.id.nav_profile -> { openFragment(ProfileFragment(), "PROFILE"); true }
                else -> false
            }
        }
        // Deshabilitar navegación hasta obtener ubicación
        bottomNav.isEnabled = false

        // No abrir contenido hasta que la ubicación esté lista
        if (savedInstanceState == null) {
            // Crear overlay bloqueante
            val overlay = android.widget.FrameLayout(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#80FFFFFF"))
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                val progress = android.widget.ProgressBar(context).apply {
                    isIndeterminate = true
                }
                val tv = android.widget.TextView(context).apply {
                    text = "Detectando ubicación…"
                    setTextColor(android.graphics.Color.BLACK)
                    textSize = 16f
                    setPadding(0, 24, 0, 0)
                    gravity = android.view.Gravity.CENTER
                }
                val container = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    addView(progress)
                    addView(tv)
                }
                addView(container, android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }
            addContentView(overlay, overlay.layoutParams)
            locationOverlay = overlay
        }

        // NUEVO BLOQUE: obtener país y cambiar bandera
        fused = LocationServices.getFusedLocationProviderClient(this)

        val container = findViewById<android.widget.FrameLayout>(R.id.fragmentContainer)
        val bottom = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottom.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            container.setPadding(container.paddingLeft, container.paddingTop, container.paddingRight, bottom.height)
        }
        highAccReq = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()
        settingsRequest = LocationSettingsRequest.Builder().addLocationRequest(highAccReq).setAlwaysShow(true).build()
        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
        } else {
            // Verificar que el GPS esté encendido y solicitar resolución si no lo está
            iniciarDeteccionUbicacion(settingsClient)
        }
    }

    // Manejo del permiso de ubicación
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permiso concedido: iniciar detección de ubicación
            val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
            iniciarDeteccionUbicacion(settingsClient)
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            // Tras activar GPS, intenta obtener ubicación nuevamente
            if (resultCode == RESULT_OK) {
                obtenerUbicacion()
            } else {
                Toast.makeText(this, "El GPS no se activó", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarDeteccionUbicacion(settingsClient: SettingsClient) {
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                // GPS habilitado: proceder a obtener ubicación
                obtenerUbicacion()
            }
            .addOnFailureListener { ex ->
                if (ex is ResolvableApiException) {
                    try {
                        ex.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Activa el GPS manualmente", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun obtenerUbicacion() {
        fused.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                procesarUbicacion(location.latitude, location.longitude)
            } else {
                // Solicita una actualización activa si lastLocation es nulo
                locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null) {
                            procesarUbicacion(loc.latitude, loc.longitude)
                            // Detener más actualizaciones para evitar consumo innecesario
                            fused.removeLocationUpdates(this)
                            locationCallback = null
                        }
                    }
                }
                fused.requestLocationUpdates(highAccReq, locationCallback as com.google.android.gms.location.LocationCallback, mainLooper)
            }
        }
    }

    // Procesar coordenadas → país → bandera
    private fun procesarUbicacion(lat: Double, lon: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val lista = geocoder.getFromLocation(lat, lon, 1)
            val pais = lista?.firstOrNull()?.countryName ?: "Desconocido"

            val imgPais = findViewById<ImageView>(R.id.imgPais)
            when (pais) {
                "Peru", "Perú" -> imgPais.setImageResource(R.drawable.flag_peru)
                "Mexico", "México" -> imgPais.setImageResource(R.drawable.flag_mexico)
                "Argentina" -> imgPais.setImageResource(R.drawable.flag_argentina)
                "United States","Estados Unidos" -> imgPais.setImageResource(R.drawable.flag_estadosunidos)
                else -> imgPais.setImageResource(R.drawable.flag_global)
            }
            imgPais.invalidate()

            // Guardar moneda preferida según país detectado
            val moneda = when (pais) {
                "Peru", "Perú" -> "PEN"
                "Mexico", "México" -> "MXN"
                "Argentina" -> "ARS"
                "United States", "Estados Unidos" -> "USD"
                else -> "PEN"
            }
            getSharedPreferences("aureum_prefs", MODE_PRIVATE)
                .edit()
                .putString("preferred_currency", moneda)
                .apply()

            Log.d("Geo", "Ubicación detectada: $pais")
            Toast.makeText(this, "Ubicación: $pais", Toast.LENGTH_SHORT).show()

            // Habilitar navegación y abrir inicio si aún no se ha cargado
            findViewById<BottomNavigationView>(R.id.bottomNav).isEnabled = true
            locationOverlay?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
            locationOverlay = null
            abrirInicioSiNecesario()
        } catch (e: Exception) {
            Log.e("Geo", "Error geocoder: ${e.message}")
            // En caso de error, permitir navegación con configuración por defecto
            findViewById<BottomNavigationView>(R.id.bottomNav).isEnabled = true
            locationOverlay?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
            locationOverlay = null
            abrirInicioSiNecesario()
        }
    }

    private fun openFragment(fragment: androidx.fragment.app.Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    private fun abrirInicioSiNecesario() {
        if (!inicioCargado) {
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
            if (startTab == "DEBTS") {
                openFragment(DebtsFragment(), "DEBTS")
                bottomNav.selectedItemId = R.id.nav_debts
            } else {
                openFragment(AccountsFragment(), "ACCOUNTS")
                bottomNav.selectedItemId = R.id.nav_home
            }
            inicioCargado = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }
}
