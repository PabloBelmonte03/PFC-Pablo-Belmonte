package com.example.pfcpablobelmontebotella.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*

/**
 * CompactCalendarView que, al dibujar:
 * 1) Intercepta el Paint del día seleccionado y lo vuelve transparente antes de llamar a super.onDraw(),
 *    de modo que el círculo “grande” de la librería no se vea.
 * 2) Restablece el Paint a su color original y dibuja un círculo más pequeño (radio 5px) centrado.
 */
class SmallCircleCompactCalendarView : CompactCalendarView {

    // Referencias por reflexión (las inicializamos en initReflection())
    private var fieldCurrentDay: Field? = null
    private var methodGetTopbarHeight: Method? = null
    private var fieldSelectedPaint: Field? = null

    constructor(context: Context) : super(context) {
        initReflection()
    }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initReflection()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initReflection()
    }

    private fun initReflection() {
        try {
            fieldCurrentDay = CompactCalendarView::class.java.getDeclaredField("currentDay")
            fieldCurrentDay?.isAccessible = true

            methodGetTopbarHeight = CompactCalendarView::class.java.getDeclaredMethod("getTopbarHeight")
            methodGetTopbarHeight?.isAccessible = true

            fieldSelectedPaint = CompactCalendarView::class.java.getDeclaredField("mCurrentSelectedDayBackgroundPaint")
            fieldSelectedPaint?.isAccessible = true
        } catch (e: Exception) {
            // Si algo falla, dejamos esas referencias en null y no dibujamos el círculo extra.
            fieldCurrentDay = null
            methodGetTopbarHeight = null
            fieldSelectedPaint = null
            e.printStackTrace()
        }
    }

    override fun onDraw(canvas: Canvas) {
        // 1) Antes de llamar a super.onDraw, hacemos transparente el Paint original
        val paintSelected = fieldSelectedPaint?.get(this) as? Paint
        var originalColor = 0

        if (paintSelected != null) {
            originalColor = paintSelected.color         // guardamos el color actual (p. ej. "#6200EE")
            paintSelected.color = Color.TRANSPARENT      // lo volvemos transparente
        }

        // 2) Ahora le pedimos a la clase padre que dibuje todo (incluido su círculo grande, pero ya transparente)
        super.onDraw(canvas)

        // 3) Restauramos el Paint a su color original (morado, o el que hayas definido en XML)
        if (paintSelected != null) {
            paintSelected.color = originalColor
        }

        // 4) Dibujamos nuestro círculo pequeño
        drawSmallSelectedCircle(canvas, paintSelected)
    }

    /**
     * Dibuja un círculo más pequeño (radio 5px) usando el Paint original
     * que acabamos de restaurar (color morado).
     */
    private fun drawSmallSelectedCircle(canvas: Canvas, paint: Paint?) {
        if (paint == null) return

        try {
            // 1) Obtenemos la fecha seleccionada (private Date currentDay)
            val currentDayValue = fieldCurrentDay?.get(this) as? Date ?: return

            // 2) Obtenemos la altura del encabezado (mes/año)
            val headerHeightPx = (methodGetTopbarHeight?.invoke(this) as? Int)?.toFloat() ?: return

            // --- A) Dimensiones totales y área de “días” ---
            val totalW = width.toFloat()
            val totalH = height.toFloat()
            val daysAreaH = totalH - headerHeightPx

            // Fijamos 6 filas y 7 columnas (para simplificar)
            val filas = 6f
            val cols = 7f
            val cellW = totalW / cols
            val cellH = daysAreaH / filas

            // --- B) Calculamos fila/columna donde cae currentDayValue ---
            val cal = Calendar.getInstance().apply { time = currentDayValue }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

            // Offset de la primera semana del mes (0=domingo, 1=lunes,..)
            val firstOfMonth = Calendar.getInstance().apply {
                time = currentDayValue
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val startWeekOffset = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 1 // 0..6

            // Fila aproximada (0..5):
            val row = ((dayOfMonth + startWeekOffset - 1) / 7).toFloat()
            // Columna (0..6):
            val col = (cal.get(Calendar.DAY_OF_WEEK) - 1).toFloat()

            // --- C) Centro de la celda en px ---
            val centerX = col * cellW + cellW / 2f
            val centerY = headerHeightPx + row * cellH + cellH / 2f

            // --- D) Radio fijo PEQUEÑO (5px) ---
            val radiusPx = 5f

            // --- E) Dibujamos el círculo pequeño con el Paint morado restaurado ---
            canvas.drawCircle(centerX, centerY, radiusPx, paint)
        } catch (t: Throwable) {
            // Si algo falla, no dibujamos nada extra
            t.printStackTrace()
        }
    }
}
