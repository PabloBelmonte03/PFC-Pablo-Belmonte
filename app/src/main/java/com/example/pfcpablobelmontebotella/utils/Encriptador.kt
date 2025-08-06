package com.example.pfcpablobelmontebotella.utils

import java.security.MessageDigest

object Encriptador{

    fun cifrarPass(password: String): String {
        val algoHash = MessageDigest.getInstance("SHA-256") //Crea un algoritmo de hash con SHA-256.
        val convertPass = algoHash.digest(password.toByteArray(Charsets.UTF_8)) //Convierte la contraseña a bytes UTF-8 y la cifra con SHA-256.
        return convertPass.joinToString("") { "%02x".format(it) }
    }
}