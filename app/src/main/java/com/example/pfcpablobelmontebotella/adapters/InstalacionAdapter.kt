package com.example.pfcpablobelmontebotella.adapters

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.pfcpablobelmontebotella.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class InstalacionAdapter(
    private val instalaciones: List<Map<String, Any>>,
    private val isAdmin: Boolean,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<InstalacionAdapter.InstalacionViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val userId: String by lazy {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstalacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_instalacion, parent, false)
        return InstalacionViewHolder(view)
    }

    override fun getItemCount(): Int = instalaciones.size

    override fun onBindViewHolder(holder: InstalacionViewHolder, position: Int) {
        val instalacion = instalaciones[position]
        val context = holder.itemView.context

        val id = instalacion["id"] as? String ?: return
        val nombre = instalacion["nombre"] as? String ?: "Sin nombre"
        val horario = instalacion["horario_disponible"] as? String ?: "-"
        val capacidad = (instalacion["capacidad"] as? Long)?.toInt() ?: 0
        val requiereReserva = instalacion["requiere_reserva"] == true
        val imagenNombre = instalacion["imagen"] as? String ?: "logo"

        // Cargar imagen desde drawable según el nombre
        val resId = context.resources.getIdentifier(imagenNombre, "drawable", context.packageName)
        holder.ivFondoInstalacion.setImageResource(resId)

        // Mostrar información
        holder.txtNombre.text = "🏷️ $nombre"
        holder.txtHorario.text = "🕒 $horario"
        holder.txtCapacidad.text = "👥 Máx. $capacidad"
        holder.txtReserva.text = if (requiereReserva) "🔐 Requiere reserva" else "🔓 Sin reserva"
        holder.txtPlazas.text = ""

        // Botón eliminar (solo para admin)
        if (isAdmin) {
            holder.btnEliminar.visibility = View.VISIBLE
            holder.btnEliminar.setOnClickListener {
                onDelete(id)
            }
        } else {
            holder.btnEliminar.visibility = View.GONE
        }

        // Colorear tarjeta si ya ha reservado
        val fechaHoy = DateFormat.format("yyyy-MM-dd", Date()).toString()
        db.collection("reservas")
            .whereEqualTo("instalacion_id", id)
            .whereEqualTo("fecha", fechaHoy)
            .get()
            .addOnSuccessListener { docs ->
                val totalHoy = docs.size()
                val yaReservo = docs.any { it.getString("usuario_id") == userId }

                holder.txtPlazas.text = "$totalHoy/$capacidad plazas"
                holder.card.setCardBackgroundColor(
                    if (yaReservo) Color.parseColor("#D8F6DC") else Color.WHITE
                )

                if (!isAdmin && requiereReserva) {
                    holder.itemView.setOnClickListener {
                        if (yaReservo) {
                            mostrarDialogoCancelar(context, id)
                        } else {
                            mostrarDialogoReservar(context, id)
                        }
                    }
                } else {
                    holder.itemView.setOnClickListener(null)
                }
            }
            .addOnFailureListener {
                holder.txtPlazas.text = "-/$capacidad plazas"
                holder.card.setCardBackgroundColor(Color.WHITE)
                holder.itemView.setOnClickListener(null)
            }
    }

    private fun mostrarDialogoReservar(context: Context, instalacionId: String) {
        val ahora = Calendar.getInstance()
        val horaInit = ahora.get(Calendar.HOUR_OF_DAY)
        val minutoInit = ahora.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val horaInicio = String.format("%02d:%02d", selectedHour, selectedMinute)
                val finCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    add(Calendar.HOUR_OF_DAY, 1)
                }
                val horaFin = String.format("%02d:%02d", finCalendar.get(Calendar.HOUR_OF_DAY), finCalendar.get(Calendar.MINUTE))

                AlertDialog.Builder(context)
                    .setTitle("Confirmar reserva")
                    .setMessage("Reservar de $horaInicio a $horaFin para hoy?")
                    .setPositiveButton("Sí") { _, _ ->
                        guardarReservaEnFirestore(context, instalacionId, horaInicio, horaFin)
                    }
                    .setNegativeButton("No", null)
                    .show()
            },
            horaInit,
            minutoInit,
            true
        ).show()
    }

    private fun guardarReservaEnFirestore(context: Context, instalacionId: String, horaInicio: String, horaFin: String) {
        val fechaHoy = DateFormat.format("yyyy-MM-dd", Date()).toString()
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val comunidadId = prefs.getString("COMUNIDAD_ACTIVA_ID", "") ?: ""

        val reserva = hashMapOf(
            "instalacion_id" to instalacionId,
            "usuario_id" to userId,
            "fecha" to fechaHoy,
            "hora_inicio" to horaInicio,
            "hora_fin" to horaFin,
            "comunidad_id" to comunidadId
        )

        db.collection("reservas")
            .add(reserva)
            .addOnSuccessListener {
                Toast.makeText(context, "Reserva guardada: $horaInicio - $horaFin", Toast.LENGTH_SHORT).show()
                notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al reservar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DEBUG_RESERVA", "Error al guardar reserva: ${e.message}")
            }
    }

    private fun mostrarDialogoCancelar(context: Context, instalacionId: String) {
        AlertDialog.Builder(context)
            .setTitle("¿Cancelar reserva?")
            .setMessage("Ya tienes una reserva aquí. ¿Quieres cancelarla?")
            .setPositiveButton("Sí") { _, _ ->
                val hoy = DateFormat.format("yyyy-MM-dd", Date()).toString()
                db.collection("reservas")
                    .whereEqualTo("instalacion_id", instalacionId)
                    .whereEqualTo("fecha", hoy)
                    .whereEqualTo("usuario_id", userId)
                    .get()
                    .addOnSuccessListener { docs ->
                        if (!docs.isEmpty) {
                            val idReserva = docs.documents.first().id
                            db.collection("reservas").document(idReserva).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Reserva cancelada", Toast.LENGTH_SHORT).show()
                                    notifyDataSetChanged()
                                }
                        } else {
                            Toast.makeText(context, "No se encontró la reserva", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    inner class InstalacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFondoInstalacion: ImageView = view.findViewById(R.id.ivFondoInstalacion)
        val txtNombre: TextView = view.findViewById(R.id.txtNombre)
        val txtHorario: TextView = view.findViewById(R.id.txtHorario)
        val txtCapacidad: TextView = view.findViewById(R.id.txtCapacidad)
        val txtReserva: TextView = view.findViewById(R.id.txtReserva)
        val txtPlazas: TextView = view.findViewById(R.id.txtPlazas)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
        val card: CardView = view as CardView
    }
}
