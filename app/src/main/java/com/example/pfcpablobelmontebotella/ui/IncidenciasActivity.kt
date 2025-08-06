package com.example.pfcpablobelmontebotella.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pfcpablobelmontebotella.R
import com.example.pfcpablobelmontebotella.model.Incidencia
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class IncidenciasActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var listaIncidencias: ArrayList<Incidencia>
    private lateinit var adapter: IncidenciaAdapter
    private lateinit var prefs: SharedPreferences
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pag_incidencias)

        // === BOTÓN “VOLVER” (ícono) ===
        val btnVolver = findViewById<ImageButton>(R.id.btn_volver_incidencias)
        btnVolver.setOnClickListener {
            // Al pulsar: vuelve a la pantalla de login
            val intent = Intent(this, LoginUserMain::class.java)
            startActivity(intent)
            finish()
        }
        // =============================

        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        recyclerView = findViewById(R.id.recyclerViewIncidencias)
        recyclerView.layoutManager = LinearLayoutManager(this)
        listaIncidencias = ArrayList()

        // Adapter provisional; se reemplazará en cargarIncidencias()
        adapter = IncidenciaAdapter(
            listaIncidencias,
            isAdmin = false
        ) { _, _ -> }
        recyclerView.adapter = adapter

        findViewById<View>(R.id.btnAgregarIncidencia).setOnClickListener {
            mostrarDialogoNuevaIncidencia()
        }
    }

    override fun onStart() {
        super.onStart()
        comprobarRolYcargarIncidencias()
    }

    private fun comprobarRolYcargarIncidencias() {
        val userId = prefs.getString("USER_ID", null)
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                val isAdmin = doc.getString("tipo_usuario") == "Admin"
                cargarIncidencias(isAdmin, doc.getString("id_comunidad"))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar tipo de usuario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarIncidencias(isAdmin: Boolean, userComunidadId: String?) {
        val comunidadSeleccionadaId = prefs.getString("COMUNIDAD_ACTIVA_ID", null)

        val comunidadId = when {
            isAdmin && !comunidadSeleccionadaId.isNullOrBlank() -> comunidadSeleccionadaId
            isAdmin && comunidadSeleccionadaId.isNullOrBlank() -> {
                Toast.makeText(this, "Selecciona una comunidad para administrarla", Toast.LENGTH_LONG).show()
                listaIncidencias.clear()
                adapter.notifyDataSetChanged()
                return
            }
            else -> userComunidadId
        }

        if (comunidadId.isNullOrBlank()) {
            Toast.makeText(this, "No estás en ninguna comunidad", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("incidencias")
            .whereEqualTo("comunidad_id", comunidadId)
            .get()
            .addOnSuccessListener { result ->
                listaIncidencias.clear()
                for (d in result.documents) {
                    val incidencia = d.toObject(Incidencia::class.java)
                    if (incidencia != null) {
                        incidencia.firestoreId = d.id
                        listaIncidencias.add(incidencia)
                    }
                }

                adapter = IncidenciaAdapter(
                    listaIncidencias,
                    isAdmin
                ) { position, firestoreId ->
                    if (isAdmin) confirmarEliminarIncidencia(firestoreId, position)
                }
                recyclerView.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar incidencias", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmarEliminarIncidencia(firestoreId: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar incidencia")
            .setMessage("¿Estás seguro de que quieres eliminar esta incidencia?")
            .setPositiveButton("Sí") { _, _ ->
                eliminarIncidencia(firestoreId, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarIncidencia(firestoreId: String, position: Int) {
        Log.d("BORRADO", "Firestore ID recibido: '$firestoreId'")
        db.collection("incidencias").document(firestoreId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Incidencia eliminada", Toast.LENGTH_SHORT).show()
                listaIncidencias.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
            .addOnFailureListener {
                Log.e("BORRADO", "Fallo al borrar: ${it.message}")
                Toast.makeText(this, "Error al eliminar incidencia", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarDialogoNuevaIncidencia() {
        val userId = prefs.getString("USER_ID", null)
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtenemos el documento del usuario para saber si es Admin o no
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { docU ->
                val isAdmin = docU.getString("tipo_usuario") == "Admin"
                val comunidadUsuario: String? = if (isAdmin) {
                    // Si es Admin, exigimos que exista PREF
                    val comunidadSeleccionadaId = prefs.getString("COMUNIDAD_ACTIVA_ID", null)
                    if (comunidadSeleccionadaId.isNullOrBlank()) {
                        Toast.makeText(
                            this,
                            "Selecciona primero una comunidad para administrarla",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnSuccessListener
                    }
                    comunidadSeleccionadaId
                } else {
                    // Si no es Admin, usamos el id_comunidad que recibe desde Firestore
                    docU.getString("id_comunidad")
                }

                if (comunidadUsuario.isNullOrBlank()) {
                    Toast.makeText(this, "No estás en ninguna comunidad", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Inflamos layout del diálogo
                val view = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_nueva_incidencia, null)

                AlertDialog.Builder(this)
                    .setTitle("Nueva Incidencia")
                    .setView(view)
                    .setPositiveButton("Guardar") { _, _ ->
                        val titulo = view.findViewById<EditText>(R.id.editTituloIncidencia)
                            .text.toString().trim()
                        val descripcion =
                            view.findViewById<EditText>(R.id.editDescripcionIncidencia).text
                                .toString().trim()
                        if (titulo.isBlank() || descripcion.isBlank()) {
                            Toast.makeText(this, "Datos vacíos", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        // Obtenemos nombre de usuario para guardarlo en la incidencia
                        val nombreUsuario = docU.getString("nom_usuario") ?: "Desconocido"

                        val newDocId = UUID.randomUUID().toString()
                        val newDoc = hashMapOf<String, Any>(
                            "id" to newDocId,
                            "titulo" to titulo,
                            "descripcion" to descripcion,
                            "usuario_id" to userId,
                            "nombre_usuario" to nombreUsuario,
                            "comunidad_id" to comunidadUsuario,
                            "estado" to "",
                            "fecha" to ""
                        )

                        db.collection("incidencias")
                            .document(newDocId)
                            .set(newDoc)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Incidencia registrada", Toast.LENGTH_SHORT)
                                    .show()
                                comprobarRolYcargarIncidencias()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Error al registrar incidencia",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener datos de usuario", Toast.LENGTH_SHORT).show()
            }
    }
}
