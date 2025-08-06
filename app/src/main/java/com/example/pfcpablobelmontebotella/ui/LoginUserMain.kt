package com.example.pfcpablobelmontebotella.ui

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.pfcpablobelmontebotella.R
import com.example.pfcpablobelmontebotella.firebase.CerrarSesion
import com.example.pfcpablobelmontebotella.firebase.PerfilUsuario
import com.example.pfcpablobelmontebotella.utils.Generador_Codigo
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore

class LoginUserMain : AppCompatActivity() {

    private var drawerLayout: DrawerLayout?     = null
    private var navigationView: NavigationView? = null
    private lateinit var prefs: SharedPreferences
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LOGIN_USER_MAIN", "Entrando a LoginUserMain")
        setContentView(R.layout.activity_login_user_main)

        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", null)
        if (userId == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Referencias
        drawerLayout   = findViewById(R.id.activity_login_user_main)
        navigationView = findViewById(R.id.nav_view)

        // Eliminar scrim gris
        drawerLayout?.setScrimColor(Color.TRANSPARENT)

        // “Hamburger” abre drawer a la derecha
        findViewById<ImageButton>(R.id.btnDes)?.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.END)
        }

        // Botones del dashboard
        findViewById<ImageButton>(R.id.btn_Home)?.setOnClickListener {
            startActivity(Intent(this, Instalaciones::class.java))
        }
        findViewById<ImageButton>(R.id.btn_Incidencias)?.setOnClickListener {
            startActivity(Intent(this, IncidenciasActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btn_Calendario)?.setOnClickListener {
            startActivity(Intent(this, CalendarioActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btn_Chat)?.setOnClickListener {
            // …
        }
        findViewById<ImageButton>(R.id.btn_Gastos)?.setOnClickListener {
            // …
        }
        findViewById<ImageButton>(R.id.btn_user)?.setOnClickListener {
            startActivity(Intent(this, PerfilUsuario::class.java))
        }

        // Listener del menú lateral (cierra a la derecha)
        navigationView?.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.add_comunidad      -> showAgregarComunidadDialog()
                R.id.join_comunidad     -> showUnirseComunidadDialog()
                R.id.list_comunidad     -> startActivity(Intent(this, ListadoComunidades::class.java))
                R.id.nav_logout         -> CerrarSesion.cerrarSesion(this)
            }
            drawerLayout?.closeDrawer(GravityCompat.END)
            true
        }

        // Carga usuario y ajusta visibilidad
        db.collection("usuarios").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val tipo   = doc.getString("tipo_usuario") ?: "Vecino"
                    val nombre = doc.getString("nom_usuario")  ?: "Usuario"
                    navigationView?.menu?.apply {
                        findItem(R.id.add_comunidad).isVisible  = tipo == "Admin"
                        findItem(R.id.list_comunidad).isVisible = tipo == "Admin"
                        findItem(R.id.join_comunidad).isVisible = tipo != "Admin"
                    }
                    findViewById<TextView>(R.id.mensaje_bienvenida)
                        ?.text = "Hola, $nombre"
                }
            }
    }

    private fun showAgregarComunidadDialog() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_agregar_comunidad, null)
        AlertDialog.Builder(this)
            .setTitle("Agregar Comunidad")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val calle     = view.findViewById<EditText>(R.id.editCalle).text.toString()
                val ciudad    = view.findViewById<EditText>(R.id.editCiudad).text.toString()
                val escaleras = view.findViewById<EditText>(R.id.editNumeroEscaleras)
                    .text.toString().toIntOrNull() ?: 1
                val codUnic = Generador_Codigo.generarCodigoUnico()
                val uid     = prefs.getString("USER_ID", null) ?: return@setPositiveButton
                db.collection("usuarios").document(uid)
                    .get().addOnSuccessListener { doc ->
                        val admin = doc.getString("nom_usuario").orEmpty()
                        if (admin.isEmpty()) {
                            Toast.makeText(this, "Nombre no configurado.", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }
                        val comunidad = hashMapOf(
                            "calle"        to calle,
                            "ciudad"       to ciudad,
                            "n_escaleras"  to escaleras,
                            "nombre_admin" to admin,
                            "cod_unic"     to codUnic
                        )
                        db.collection("comunidades")
                            .add(comunidad)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Comunidad creada.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al crear comunidad.", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showUnirseComunidadDialog() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_unirse_comunidad, null)
        AlertDialog.Builder(this)
            .setTitle("Unirse a Comunidad")
            .setView(view)
            .setPositiveButton("Unirse") { _, _ ->
                val codigo = view.findViewById<EditText>(R.id.editCodigoComunidad)
                    .text.toString()
                val uid = prefs.getString("USER_ID", null) ?: return@setPositiveButton
                db.collection("comunidades")
                    .whereEqualTo("cod_unic", codigo)
                    .get().addOnSuccessListener { docs ->
                        if (!docs.isEmpty) {
                            val comunidadId = docs.documents[0].id
                            db.collection("usuarios").document(uid)
                                .update("id_comunidad", comunidadId)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Unido a la comunidad.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al unirte.", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Código inválido.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
