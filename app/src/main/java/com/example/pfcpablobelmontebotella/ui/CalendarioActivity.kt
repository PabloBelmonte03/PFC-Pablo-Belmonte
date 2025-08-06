package com.example.pfcpablobelmontebotella.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pfcpablobelmontebotella.R
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import java.text.SimpleDateFormat
import java.util.*

class CalendarioActivity : AppCompatActivity() {

    private lateinit var compactCalendarView: CompactCalendarView

    // Mapa: fecha en milisegundos UTC → texto de la anotación
    private val anotaciones = HashMap<Long, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario)

        compactCalendarView = findViewById(R.id.compactCalendarView)

        // Mostramos mes actual en la ActionBar
        supportActionBar?.title =
            "Calendario (${ getMonthName(compactCalendarView.firstDayOfCurrentMonth) })"

        compactCalendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(dateClicked: Date) {
                abrirDialogoAnotacion(dateClicked)
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date) {
                supportActionBar?.title =
                    "Calendario (${ getMonthName(firstDayOfNewMonth) })"
            }
        })

        // Si quieres que, al iniciar la actividad, muestre los días con anotaciones:
        cargarEventosPrevios()

        val btnVolver = findViewById<ImageButton>(R.id.btn_volver_comunidades)
        // 2) Al pulsar, vuelve a tu LoginUserMainActivity
        btnVolver.setOnClickListener {
            startActivity(Intent(this, LoginUserMain::class.java))
            finish()
        }

    }

    private fun cargarEventosPrevios() {
        // Borra cualquier event viejo y regenera desde el mapa `anotaciones`
        compactCalendarView.removeAllEvents()
        for ((diaMillis, texto) in anotaciones) {
            // Elegimos un color sólido; aquí, el mismo morado que usabas para el círculo.
            val colorPunto = 0xFF6200EE.toInt()
            compactCalendarView.addEvent(Event(colorPunto, diaMillis, texto))
        }
    }

    private fun abrirDialogoAnotacion(date: Date) {
        val diaMillis = getStartOfDayUtc(date)
        val dialogView = layoutInflater.inflate(R.layout.dialogo_anotacion, null)
        val editText = dialogView.findViewById<android.widget.EditText>(R.id.editTextNota)

        // Si ya existía anotación, precargamos el texto
        anotaciones[diaMillis]?.let { editText.setText(it) }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Anotar para ${ formatDate(date) }")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val texto = editText.text.toString().trim()
                // Quitamos todos los eventos de ese día (si hubiera)
                compactCalendarView.getEvents(diaMillis).forEach { ev ->
                    compactCalendarView.removeEvent(ev)
                }
                if (texto.isNotEmpty()) {
                    // Guardamos en el mapa
                    anotaciones[diaMillis] = texto
                    // Añadimos un Event de color morado
                    val colorPunto = 0xFF6200EE.toInt()
                    compactCalendarView.addEvent(Event(colorPunto, diaMillis, texto))
                    Toast.makeText(this, "Anotación guardada", Toast.LENGTH_SHORT).show()
                } else {
                    // Si quedó vacío, borramos del mapa
                    anotaciones.remove(diaMillis)
                    Toast.makeText(this, "Anotación eliminada", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Obtiene la fecha a medianoche UTC para CompactCalendarView
    private fun getStartOfDayUtc(date: Date): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Formatea Date a "dd/MM/yyyy"
    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
        return sdf.format(date)
    }

    // Formatea Date a "MMMM yyyy" para el título (e.g. "junio 2025")
    private fun getMonthName(date: Date): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        return sdf.format(date)
    }
}
