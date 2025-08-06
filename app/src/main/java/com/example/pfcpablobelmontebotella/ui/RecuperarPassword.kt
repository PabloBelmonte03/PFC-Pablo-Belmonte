package com.example.pfcpablobelmontebotella.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pfcpablobelmontebotella.R
import com.google.firebase.auth.FirebaseAuth

// Actividad para recuperar la contraseña mediante email
class RecuperarPassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth         // Objeto para autenticar con Firebase
    private lateinit var emailEditText: EditText    // Campo para escribir el email
    private lateinit var recuperarButton: Button    // Botón para enviar email de recuperación

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperar_pass)    // Carga el layout correspondiente

        auth = FirebaseAuth.getInstance()          // Inicializamos FirebaseAuth
        emailEditText = findViewById(R.id.emailEditText)  // Referencia al campo de email
        recuperarButton = findViewById(R.id.recuperarButton) // Referencia al botón

        // Cuando se pulsa el botón, comprobamos que el email no esté vacío y enviamos el email
        recuperarButton.setOnClickListener {
            val email = emailEditText.text.toString()
            if (email.isNotEmpty()) {
                enviarEmailRecuperacion(email)  // Llamamos a la función para enviar el email
            } else {
                Toast.makeText(this, "Introduce tu email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función que envía el email de recuperación de contraseña
    private fun enviarEmailRecuperacion(email: String) {
        auth.sendPasswordResetEmail(email)  // Método Firebase para enviar email
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Se ha enviado un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show()
                    finish() // Cerramos esta actividad y volvemos a la pantalla anterior (login)
                } else {
                    // Si hay error mostramos el mensaje que devuelve Firebase
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

}
