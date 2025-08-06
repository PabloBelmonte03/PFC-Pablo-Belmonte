package com.example.pfcpablobelmontebotella.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pfcpablobelmontebotella.R
import com.example.pfcpablobelmontebotella.firebase.UsuarioFirebase
import com.example.pfcpablobelmontebotella.model.Usuario

class PantallaRegistro : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_registro)

        // Icono DNI Usuario
        val editTextDNI = findViewById<EditText>(R.id.DNI_User)
        val iconDNI = ContextCompat.getDrawable(this, R.drawable.ic_dni)
        val textSizePxDNI = editTextDNI.textSize
        iconDNI?.setBounds(0, 0, textSizePxDNI.toInt(), textSizePxDNI.toInt())
        editTextDNI.setCompoundDrawables(iconDNI, null, null, null)

        // Icono del Usuario
        val editTextUsu = findViewById<EditText>(R.id.RegisterTextUsuario)
        val iconUser = ContextCompat.getDrawable(this, R.drawable.ic_user)
        val textSizePxUsu = editTextUsu.textSize
        iconUser?.setBounds(0, 0, textSizePxUsu.toInt(), textSizePxUsu.toInt())
        editTextUsu.setCompoundDrawables(iconUser, null, null, null)

        // Icono del email
        val editTextEmail = findViewById<EditText>(R.id.RegisterTextEmail)
        val iconEmail = ContextCompat.getDrawable(this, R.drawable.ic_mail)
        val textSizePxEmail = editTextEmail.textSize
        iconEmail?.setBounds(0, 0, textSizePxEmail.toInt(), textSizePxEmail.toInt())
        editTextEmail.setCompoundDrawables(iconEmail, null, null, null)

        // Icono contraseña
        val editTextPass = findViewById<EditText>(R.id.RegisterPass)
        val iconPass = ContextCompat.getDrawable(this, R.drawable.ic_pass)
        iconPass?.setBounds(0, 0, textSizePxEmail.toInt(), textSizePxEmail.toInt())
        editTextPass.setCompoundDrawables(iconPass, null, null, null)

        // Icono repetir contraseña
        val editTextReapeatPass = findViewById<EditText>(R.id.ReapeatRegisterPass)
        editTextReapeatPass.setCompoundDrawables(iconPass, null, null, null)

        val btn_Register = findViewById<Button>(R.id.btn_Registrar)

        //Lógica btn register.
        btn_Register.setOnClickListener {

            val dni = editTextDNI.text.toString().trim()
            val nombre = editTextUsu.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val pass = editTextPass.text.toString().trim()
            val repeatPass = editTextReapeatPass.text.toString().trim()

            if (dni.isEmpty() || nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || repeatPass.isEmpty()) {
                Toast.makeText(this, "No pueden haber campos vacíos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != repeatPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newUser = Usuario(
                DNI_USU = dni,
                Nom_usuario = nombre,
                email = email,
                tipo_usuario = "Vecino"
            )

            // Registro a través de UsuarioFirebase
            UsuarioFirebase.registrar(newUser, pass) { resultado ->
                when (resultado) {
                    "OK" -> {
                        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginUserMain::class.java))
                        finish()
                    }
                    "DNI" -> {
                        Toast.makeText(this, "El DNI ya está registrado", Toast.LENGTH_SHORT).show()
                    }
                    "EMAIL" -> {
                        Toast.makeText(this, "El email ya está registrado", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, "Error inesperado al registrar: $resultado", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }

    }
}
