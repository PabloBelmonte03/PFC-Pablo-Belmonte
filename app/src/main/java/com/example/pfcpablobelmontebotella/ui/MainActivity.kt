package com.example.pfcpablobelmontebotella.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pfcpablobelmontebotella.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        val emailEditText = findViewById<EditText>(R.id.editTextUsuario)
        val passwordEditText = findViewById<EditText>(R.id.editTextContrasenya)
        val mantenerSesionCheckBox = findViewById<CheckBox>(R.id.mantenerSesion)
        val btnLogin = findViewById<Button>(R.id.botonIiciarSesion)
        val linkRegister = findViewById<TextView>(R.id.linkregister)
        val olvideContrasenyaTextView = findViewById<TextView>(R.id.olvideContrasenya)

        // Ir al registro
        linkRegister.setOnClickListener {
            startActivity(Intent(this, PantallaRegistro::class.java))
        }

        // Ir a recuperar contraseña
        olvideContrasenyaTextView.setOnClickListener {
            startActivity(Intent(this, RecuperarPassword::class.java))
        }

        // Lógica de login con FirebaseAuth
        btnLogin.setOnClickListener {

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid

                            db.collection("usuarios").document(uid).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val nombre = document.getString("nombre") ?: ""
                                        Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()

                                        preferences.edit()
                                            .putBoolean("mantenerSesion", mantenerSesionCheckBox.isChecked)
                                            .putString("USER_ID", uid)
                                            .putString("USER_EMAIL", email)
                                            .apply()

                                        val intent = Intent(this, LoginUserMain::class.java)
                                        startActivity(intent)
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            finish()
                                        }, 500)
                                    } else {
                                        Toast.makeText(this, "Usuario no encontrado en Firestore", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onStart() {

        super.onStart()
        val mantener = preferences.getBoolean("mantenerSesion", false)
        val userId = preferences.getString("USER_ID", null)

        if (mantener && userId != null) {
            Log.d("MAIN", "Autologin activado con usuario $userId")
            val intent = Intent(this, LoginUserMain::class.java)
            startActivity(intent)
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 500)
        }

    }


}
