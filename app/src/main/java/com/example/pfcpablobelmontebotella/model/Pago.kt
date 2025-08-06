package com.example.pfcpablobelmontebotella.model

data class Pago(
    val concepto: String = "",
    val cantidad: Double = 0.0,
    val fecha: String = "",
    val usuario_id: String = "",
    val comunidad_id: String = ""
)
