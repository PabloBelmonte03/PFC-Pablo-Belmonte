package com.example.pfcpablobelmontebotella.model

data class Incidencia(
    val titulo: String = "",
    val descripcion: String = "",
    val estado: String = "",
    val fecha: String = "",
    val usuario_id: String = "",
    val nombre_usuario: String = "",
    val comunidad_id: String = "",
    val id: String = "",
    var firestoreId: String = ""
)
