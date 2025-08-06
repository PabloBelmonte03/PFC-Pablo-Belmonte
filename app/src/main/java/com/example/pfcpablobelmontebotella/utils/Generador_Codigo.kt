package com.example.pfcpablobelmontebotella.utils

object Generador_Codigo {

    fun generarCodigoUnico(): String {
        val letras = ('A'..'Z').shuffled().take(4).joinToString("")
        val numeros = (1000..9999).random()
        return "COM-$letras$numeros"
    }

}
