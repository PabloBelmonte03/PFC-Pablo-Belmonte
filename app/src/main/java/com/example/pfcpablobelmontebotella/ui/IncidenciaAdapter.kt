package com.example.pfcpablobelmontebotella.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pfcpablobelmontebotella.R
import com.example.pfcpablobelmontebotella.model.Incidencia

// Adaptador para un RecyclerView que muestra una lista de incidencias
class IncidenciaAdapter(
    private val lista: List<Incidencia>,                // Lista con los datos de incidencias
    private val isAdmin: Boolean,                        // Indica si el usuario es admin (para mostrar botón eliminar)
    private val onEliminarClick: (position: Int, incidenciaId: String) -> Unit // Función que se llama al eliminar
) : RecyclerView.Adapter<IncidenciaAdapter.ViewHolder>() {

    // Se llama para crear una nueva "fila" o vista para el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflamos el layout XML para cada item (fila)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incidencia, parent, false)
        return ViewHolder(view) // Devolvemos el ViewHolder con la vista creada
    }

    // Devuelve la cantidad de elementos que hay en la lista
    override fun getItemCount(): Int = lista.size

    // Asigna los datos a cada elemento (fila) de la lista cuando se muestra
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val incidencia = lista[position] // Obtenemos la incidencia en la posición actual

        // Asignamos los valores de la incidencia a los TextView correspondientes
        holder.titulo.text       = incidencia.titulo
        holder.descripcion.text  = incidencia.descripcion
        holder.estado.text       = incidencia.estado ?: "" // Si estado es null, ponemos cadena vacía
        holder.usuario.text      = incidencia.nombre_usuario ?: "Desconocido" // Si no hay nombre, "Desconocido"

        // Si el usuario es admin, mostramos el botón eliminar, si no, lo ocultamos
        holder.eliminar.visibility = if (isAdmin) View.VISIBLE else View.GONE

        // Al pulsar el botón eliminar, llamamos a la función pasada desde fuera con la posición y id de incidencia
        holder.eliminar.setOnClickListener {
            onEliminarClick(position, incidencia.firestoreId)
        }
    }

    // Clase interna que guarda las referencias a los elementos de la vista para cada item
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView       = view.findViewById(R.id.tituloIncidencia)
        val descripcion: TextView  = view.findViewById(R.id.descripcionIncidencia)
        val estado: TextView       = view.findViewById(R.id.textEstado)
        val usuario: TextView      = view.findViewById(R.id.usuarioIncidencia)
        val eliminar: ImageButton  = view.findViewById(R.id.btnEliminarIncidencia)
    }
}
