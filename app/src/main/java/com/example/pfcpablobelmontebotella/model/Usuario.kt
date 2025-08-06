package com.example.pfcpablobelmontebotella.model

data class Usuario (
    val DNI_USU: String = "",
    val Nom_usuario: String = "",
    val email: String = "",
    val password: String = "",
    val comunidad_id : String = "",
    val tipo_usuario : String = "vecino"
)


