package com.example.pfcpablobelmontebotella.firebase

import android.content.Context
import android.content.Intent
import com.example.pfcpablobelmontebotella.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase

object CerrarSesion {

    fun cerrarSesion (context: Context) {

        //Cerramos sesión en Firebase si estamos usando FirebaseAuth.
        FirebaseAuth.getInstance().signOut()

        //Limpiamos los datos de la sesión.
        val clearInf = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        clearInf.edit().clear().apply()

        //Redirigimos a la página de Login.
        val red = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        context.startActivity(red)

    }

}