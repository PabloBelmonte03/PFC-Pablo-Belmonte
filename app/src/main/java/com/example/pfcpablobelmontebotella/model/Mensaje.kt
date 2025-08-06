package com.example.pfcpablobelmontebotella.model

data class Mensaje(
    val emisor_id: String = "",
    val receptor_id: String = "",
    val contenido: String = "",
    val fecha: String = "",
    val comunidad_id: String = ""
)
