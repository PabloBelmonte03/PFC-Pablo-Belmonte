package com.example.pfcpablobelmontebotella.firebase

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.pfcpablobelmontebotella.R
import com.example.pfcpablobelmontebotella.ui.LoginUserMain
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Actividad para gestionar el perfil de usuario: cambiar email y contraseña
class PerfilUsuario : AppCompatActivity() {

    companion object {
        private const val TAG = "PerfilUsuarioActivity" // Para logs
    }

    private lateinit var auth: FirebaseAuth          // Firebase Auth para gestión usuarios
    private lateinit var db: FirebaseFirestore       // Firestore para base de datos

    // Campos de texto y botones en la interfaz
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnGuardarEmail: MaterialButton

    private lateinit var etPassActual: TextInputEditText
    private lateinit var etPassNueva: TextInputEditText
    private lateinit var etPassConfirm: TextInputEditText
    private lateinit var btnCambiarPass: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_usuario)  // Carga la UI

        // Inicializamos Firebase Auth y Firestore, y ponemos idioma español en Auth
        auth = FirebaseAuth.getInstance().apply { setLanguageCode("es") }
        db = FirebaseFirestore.getInstance()

        // Obtenemos referencias a los componentes de la interfaz
        etEmail = findViewById(R.id.etEmail)
        btnGuardarEmail = findViewById(R.id.btnGuardarPerfil)

        etPassActual = findViewById(R.id.etPassActual)
        etPassNueva = findViewById(R.id.etPassNueva)
        etPassConfirm = findViewById(R.id.etPassConfirm)
        btnCambiarPass = findViewById(R.id.btnCambiarPass)

        // Botón para volver a la pantalla de login
        findViewById<ImageButton>(R.id.btn_volver_comunidades).setOnClickListener {
            startActivity(Intent(this, LoginUserMain::class.java))
            finish()  // Cierra esta actividad
        }

        cargarEmailInicial()  // Carga el email actual en el campo
        btnGuardarEmail.setOnClickListener { actualizarEmail() }  // Guarda el nuevo email
        btnCambiarPass.setOnClickListener { cambiarContrasena() } // Cambia la contraseña
    }

    // Carga el email actual del usuario y lo pone en el campo de texto
    private fun cargarEmailInicial() {
        auth.currentUser?.let { user ->
            etEmail.setText(user.email)
        }
    }

    // Actualiza el email del usuario
    private fun actualizarEmail() {
        val user = auth.currentUser ?: return
        val nuevoEmail = etEmail.text.toString().trim()
        val emailActual = user.email ?: ""

        // Validaciones básicas: email no vacío y diferente al actual
        if (nuevoEmail.isEmpty()) {
            Toast.makeText(this, "El email no puede quedar vacío", Toast.LENGTH_SHORT).show()
            return
        }
        if (nuevoEmail == emailActual) {
            Toast.makeText(this, "Introduce un email distinto al actual", Toast.LENGTH_SHORT).show()
            return
        }

        // Pedimos que el usuario se re-autentique con su contraseña actual
        solicitarReautenticacion { currentPassword ->
            val cred = EmailAuthProvider.getCredential(emailActual, currentPassword)
            user.reauthenticate(cred)  // Reautenticación para seguridad
                .addOnSuccessListener {
                    // Si pasa, enviamos email de verificación para el nuevo email
                    user.verifyBeforeUpdateEmail(nuevoEmail)
                        ?.addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Se ha enviado un email de verificación a $nuevoEmail",
                                Toast.LENGTH_LONG
                            ).show()

                            // Actualizamos el email también en Firestore
                            db.collection("Usuarios")
                                .document(user.uid)
                                .update("email", nuevoEmail)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Email actualizado en Firestore")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error actualizando Firestore", e)
                                }
                        }
                        ?.addOnFailureListener { e ->
                            Log.e(TAG, "Error verifyBeforeUpdateEmail", e)
                            Toast.makeText(
                                this,
                                "No se pudo solicitar cambio de email: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Reautenticación fallida", e)
                    Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Cambia la contraseña del usuario
    private fun cambiarContrasena() {
        val user = auth.currentUser ?: return
        val actual = etPassActual.text.toString()
        val nueva = etPassNueva.text.toString()
        val confirm = etPassConfirm.text.toString()

        // Validaciones: campos no vacíos, nueva contraseña igual a confirmación, y diferente a la actual
        if (actual.isBlank() || nueva.isBlank() || confirm.isBlank()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (nueva != confirm) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }
        if (actual == nueva) {
            Toast.makeText(this, "La nueva contraseña debe ser diferente a la actual", Toast.LENGTH_SHORT).show()
            return
        }

        val emailForCred = user.email ?: return
        val cred = EmailAuthProvider.getCredential(emailForCred, actual)

        // Reautenticamos al usuario para confirmar la identidad antes de cambiar la contraseña
        user.reauthenticate(cred)
            .addOnSuccessListener {
                // Actualizamos la contraseña en Firebase Auth
                user.updatePassword(nueva)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                        // Limpiamos los campos después del cambio
                        etPassActual.text?.clear()
                        etPassNueva.text?.clear()
                        etPassConfirm.text?.clear()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error cambiando contraseña", e)
                        Toast.makeText(this, "Error al cambiar contraseña: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Reautenticación fallida", e)
                Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Muestra un diálogo para pedir la contraseña actual,
     * y la devuelve al callback cuando el usuario la introduce.
     */
    private fun solicitarReautenticacion(onPasswordEntered: (String) -> Unit) {
        val input = TextInputEditText(this).apply {
            hint = "Contraseña actual"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(16, 16, 16, 16)  // Añade algo de espacio interno
        }
        AlertDialog.Builder(this)
            .setTitle("Reautenticación requerida")
            .setView(input)  // Ponemos el campo de texto en el diálogo
            .setPositiveButton("Aceptar") { _, _ ->
                val pwd = input.text.toString()
                if (pwd.isBlank()) {
                    Toast.makeText(this, "Introduce la contraseña", Toast.LENGTH_SHORT).show()
                } else {
                    onPasswordEntered(pwd)  // Pasamos la contraseña al callback
                }
            }
            .setNegativeButton("Cancelar", null)  // Solo cierra el diálogo
            .show()
    }
}
