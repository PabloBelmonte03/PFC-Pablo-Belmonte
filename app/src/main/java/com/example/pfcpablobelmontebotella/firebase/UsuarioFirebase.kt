package com.example.pfcpablobelmontebotella.firebase

import com.example.pfcpablobelmontebotella.model.Usuario
import com.example.pfcpablobelmontebotella.utils.Encriptador
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UsuarioFirebase {

    fun registrar(usuario: Usuario, password: String, callback: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(usuario.email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val datos = hashMapOf(
                        "dni_USU" to usuario.DNI_USU,
                        "nom_usuario" to usuario.Nom_usuario,
                        "email" to usuario.email,
                        "tipo_usuario" to usuario.tipo_usuario,
                        "comunidad_id" to usuario.comunidad_id,
                        "password" to Encriptador.cifrarPass(password)  // Guardamos la contraseña cifrada
                    )

                    // Guardar datos en Firestore
                    db.collection("usuarios").document(uid).set(datos)
                        .addOnSuccessListener {
                            callback("OK")
                        }
                        .addOnFailureListener {
                            callback("FIRESTORE_ERROR")
                        }
                } else {
                    callback("UID_NULL")
                }
            }
            .addOnFailureListener { exception ->
                if (exception.message?.contains("email address is already in use") == true) {
                    callback("EMAIL")
                } else {
                    callback(exception.message ?: "ERROR")
                }
            }
    }
}
