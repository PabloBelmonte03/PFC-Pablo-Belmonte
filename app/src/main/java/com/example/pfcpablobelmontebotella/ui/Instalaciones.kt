package com.example.pfcpablobelmontebotella.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pfcpablobelmontebotella.R
import com.example.pfcpablobelmontebotella.adapters.InstalacionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Actividad que muestra las instalaciones y permite gestionarlas
class Instalaciones : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "MyPrefs" // Nombre para SharedPreferences
    }

    private lateinit var db: FirebaseFirestore          // Base de datos Firestore
    private lateinit var prefs: SharedPreferences       // Preferencias para guardar datos locales

    private lateinit var recyclerView: RecyclerView     // Vista para lista de instalaciones
    private lateinit var adapter: InstalacionAdapter    // Adaptador para mostrar las instalaciones
    private val listaInstalaciones = mutableListOf<Map<String, Any>>() // Lista que guarda los datos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.instalaciones) // Carga el layout XML

        db = FirebaseFirestore.getInstance()   // Inicializamos la base de datos Firestore
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // Preferencias privadas

        // Configuramos el RecyclerView con un LayoutManager vertical
        recyclerView = findViewById(R.id.recyclerViewInstalaciones)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Creamos el adaptador con la lista y una función para eliminar instalaciones
        adapter = InstalacionAdapter(listaInstalaciones, isAdmin = false) { idEliminar ->
            eliminarInstalacion(idEliminar)
        }
        recyclerView.adapter = adapter

        // Botón para volver a la pantalla principal
        findViewById<ImageButton>(R.id.btn_volver_instalaciones).setOnClickListener {
            startActivity(Intent(this, LoginUserMain::class.java))
            finish() // Cerramos esta actividad para que no quede en el historial
        }

        // Botón para mostrar el diálogo para agregar una instalación nueva
        findViewById<Button>(R.id.btnAgregarInstalacion).setOnClickListener {
            mostrarDialogoAgregar()
        }

        // Cargamos las instalaciones desde la base de datos
        cargarInstalaciones()
    }

    // Función que carga las instalaciones desde Firestore
    private fun cargarInstalaciones() {
        listaInstalaciones.clear() // Limpiamos la lista para no duplicar

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return // Obtenemos id del usuario actual

        // Obtenemos datos del usuario para saber si es admin y la comunidad
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val tipoUsuario = userDoc.getString("tipo_usuario") ?: ""  // Tipo de usuario (Admin o no)
                val isAdmin = tipoUsuario == "Admin"                        // Comprobamos si es admin

                // Obtenemos el id de la comunidad activa según si es admin o no
                val comunidadId = if (isAdmin) {
                    prefs.getString("COMUNIDAD_ACTIVA_ID", null)
                } else {
                    userDoc.getString("id_comunidad")
                }

                // Mostramos o escondemos el botón para agregar instalación según permisos
                val btnAgregar = findViewById<Button>(R.id.btnAgregarInstalacion)
                btnAgregar.visibility = if (isAdmin && !comunidadId.isNullOrEmpty()) View.VISIBLE else View.GONE

                // Si no hay comunidad seleccionada, mostramos mensaje y paramos
                if (comunidadId.isNullOrEmpty()) {
                    Toast.makeText(this, "No hay comunidad seleccionada", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Actualizamos el adaptador con los permisos correctos
                adapter = InstalacionAdapter(listaInstalaciones, isAdmin) { idEliminar ->
                    eliminarInstalacion(idEliminar)
                }
                recyclerView.adapter = adapter

                // Consultamos las instalaciones de la comunidad en Firestore
                db.collection("instalaciones")
                    .whereEqualTo("comunidad_id", comunidadId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        // Añadimos cada documento a la lista como mapa con su id incluido
                        listaInstalaciones.addAll(querySnapshot.documents.map { doc ->
                            mapOf("id" to doc.id) + (doc.data ?: emptyMap())
                        })
                        adapter.notifyDataSetChanged() // Avisamos al adaptador que la lista cambió
                    }
            }
    }

    // Muestra un diálogo para agregar una instalación nueva
    private fun mostrarDialogoAgregar() {
        // Obtenemos la comunidad activa, si no hay mostramos mensaje y salimos
        val comunidadId = prefs.getString("COMUNIDAD_ACTIVA_ID", null) ?: run {
            Toast.makeText(this, "Selecciona una comunidad primero", Toast.LENGTH_SHORT).show()
            return
        }

        // Inflamos el layout del diálogo personalizado
        val dialogView = LayoutInflater.from(this).inflate(R.layout.nueva_instalacion, null)
        val spinnerImagenes = dialogView.findViewById<Spinner>(R.id.spinnerImagenes) // Selector de imágenes
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivImagenSeleccionada) // Vista previa de imagen

        // Lista con nombres de imágenes para el spinner
        val nombresImagenes = arrayOf("ic_piscina", "ic_gym", "ic_tenis")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresImagenes)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerImagenes.adapter = adapterSpinner

        // Cambia la imagen de vista previa según la opción seleccionada en el spinner
        spinnerImagenes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val resId = resources.getIdentifier(nombresImagenes[position], "drawable", packageName)
                ivPreview.setImageResource(resId) // Cambiamos imagen
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Construimos el diálogo para crear nueva instalación
        AlertDialog.Builder(this)
            .setTitle("Nueva Instalación")
            .setView(dialogView)
            .setPositiveButton("Crear") { dialog, _ ->
                // Obtenemos los datos del formulario
                val nombre = dialogView.findViewById<EditText>(R.id.editNombre).text.toString().trim()
                val descripcion = dialogView.findViewById<EditText>(R.id.editDescripcion).text.toString().trim()
                val ubicacion = dialogView.findViewById<EditText>(R.id.editUbicacion).text.toString().trim()
                val horario = dialogView.findViewById<EditText>(R.id.editHorario).text.toString().trim()
                val capacidad = dialogView.findViewById<EditText>(R.id.editCapacidad).text.toString().toIntOrNull() ?: 0
                val requiereReserva = dialogView.findViewById<CheckBox>(R.id.checkRequiereReserva).isChecked
                val imagenSeleccionada = spinnerImagenes.selectedItem.toString()

                // Comprobamos que los campos obligatorios no estén vacíos
                if (nombre.isEmpty() || descripcion.isEmpty() || ubicacion.isEmpty() || horario.isEmpty()) {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Creamos un mapa con los datos para guardar en Firestore
                val docRef = db.collection("instalaciones").document()
                val datosInstalacion = hashMapOf(
                    "nombre" to nombre,
                    "descripcion" to descripcion,
                    "ubicacion" to ubicacion,
                    "horario_disponible" to horario,
                    "capacidad" to capacidad,
                    "requiere_reserva" to requiereReserva,
                    "comunidad_id" to comunidadId,
                    "imagen" to imagenSeleccionada
                )

                // Guardamos los datos en Firestore
                docRef.set(datosInstalacion)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Instalación creada", Toast.LENGTH_SHORT).show()
                        cargarInstalaciones() // Recargamos la lista para ver la nueva instalación
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }

                dialog.dismiss() // Cerramos el diálogo
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() } // Botón para cancelar
            .show()
    }

    // Función para eliminar una instalación por su id
    private fun eliminarInstalacion(instalacionId: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Seguro que quieres eliminar la instalación?")
            .setPositiveButton("Sí") { dialog, _ ->
                db.collection("instalaciones").document(instalacionId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Instalación eliminada", Toast.LENGTH_SHORT).show()
                        cargarInstalaciones() // Recargamos la lista tras eliminar
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Solo cerramos el diálogo si el usuario cancela
            }
            .show()
    }

}
