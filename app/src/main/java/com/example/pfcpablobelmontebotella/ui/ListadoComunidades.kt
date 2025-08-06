package com.example.pfcpablobelmontebotella.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pfcpablobelmontebotella.R
import com.google.firebase.firestore.FirebaseFirestore

class ListadoComunidades : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.listado_comunidades)

        // === BOTÓN “VOLVER” (ícono) ===
        val btnVolver = findViewById<ImageButton>(R.id.btn_volver_comunidades)
        btnVolver.setOnClickListener {
            // Al pulsar: vuelve a la pantalla de login
            val intent = Intent(this, LoginUserMain::class.java)
            startActivity(intent)
            finish()
        }
        // =============================

        // Obtenemos el USER_ID de SharedPreferences
        val prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", null)

        if (userId != null) {
            db.collection("usuarios").document(userId).get().addOnSuccessListener { userDoc ->
                val nomUsuario = userDoc.getString("nom_usuario") ?: return@addOnSuccessListener

                db.collection("comunidades")
                    .whereEqualTo("nombre_admin", nomUsuario)
                    .get()
                    .addOnSuccessListener { docs ->
                        val inflater = LayoutInflater.from(this)
                        val container = findViewById<LinearLayout>(R.id.container_listado)

                        // Obtenemos la comunidad activa para marcarla visualmente
                        val comunidadActivaId = prefs.getString("COMUNIDAD_ACTIVA_ID", null)

                        docs.forEach { doc ->
                            val comunidadId = doc.id
                            val nombre = doc.getString("calle") ?: "Sin nombre"
                            val codUnic = doc.getString("cod_unic") ?: "Sin código"

                            val view = inflater.inflate(R.layout.item_comunidad, container, false)
                            val txtCalle = view.findViewById<TextView>(R.id.txtCalle)
                            val txtCodigo = view.findViewById<TextView>(R.id.txtCodigo)

                            txtCalle.text = "🏠 $nombre"
                            txtCodigo.text = "🔑 Código: $codUnic"

                            // Marcar visual si está activa
                            if (comunidadId == comunidadActivaId) {
                                view.setBackgroundColor(Color.parseColor("#E0E7FF"))
                            }

                            view.setOnClickListener {
                                val editor = prefs.edit()
                                val comunidadSeleccionada = prefs.getString("COMUNIDAD_ACTIVA_ID", null)

                                if (comunidadSeleccionada == comunidadId) {
                                    // Deseleccionar
                                    editor.remove("COMUNIDAD_ACTIVA_ID")
                                    editor.remove("COMUNIDAD_ACTIVA_NOMBRE")
                                    Toast.makeText(
                                        this,
                                        "Has dejado de administrar '$nombre'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // Seleccionar nueva
                                    editor.putString("COMUNIDAD_ACTIVA_ID", comunidadId)
                                    editor.putString("COMUNIDAD_ACTIVA_NOMBRE", nombre)
                                    Toast.makeText(
                                        this,
                                        "Ahora estás administrando '$nombre'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                editor.apply()
                                recreate()
                            }

                            container.addView(
                                view,
                                LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT
                                )
                            )
                        }
                    }
            }
        }
    }
}
