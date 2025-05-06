package mx.edu.itesca.happybox_10

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.dmoral.toasty.Toasty

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameEditText: TextInputEditText = findViewById(R.id.nameEditText)
        val emailEditText: TextInputEditText = findViewById(R.id.emailEditText)
        val passwordEditText: TextInputEditText = findViewById(R.id.passwordEditText)
        val confirmPasswordEditText: TextInputEditText = findViewById(R.id.confirmPasswordEditText)
        val registerButton: MaterialButton = findViewById(R.id.registerButton)
        val nameInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.nameInputLayout)
        val emailInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.emailInputLayout)
        val passwordInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.passwordInputLayout)
        val confirmPasswordInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.confirmPasswordInputLayout)

        val maxLengthNC = 100
        val maxLengthPwd=40
        setMaxLengthErrorWatcher(nameEditText, nameInputLayout, maxLengthNC)
        setMaxLengthErrorWatcher(emailEditText, emailInputLayout, maxLengthNC)
        setMaxLengthErrorWatcher(passwordEditText, passwordInputLayout, maxLengthPwd)
        setMaxLengthErrorWatcher(confirmPasswordEditText, confirmPasswordInputLayout, maxLengthPwd)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                //Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                Toasty.error(this, "Completa todos los campos", Toast.LENGTH_LONG, true).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                //Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                Toasty.error(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG, true).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.let { user ->
                            val uid = user.uid
                            val userMap = hashMapOf(
                                "nombreUsuario" to name,
                                "correoUsuario" to email,
                                "direccionUsuario" to "",
                                "telefonoUsuario" to "",
                                "rolUsuario" to "Cliente",
                                "preferenciasUsuario" to hashMapOf(
                                    "categoriasPreferidas" to listOf<String>(),
                                    "favoritos" to listOf<String>()
                                )
                            )
                            db.collection("Usuarios").document(uid).set(userMap)
                                .addOnSuccessListener {
                                    //Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    Toasty.success(this, "¡Registro exitoso!", Toast.LENGTH_SHORT, true).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    //Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Toasty.error(this, "Error al guardar datos", Toast.LENGTH_LONG, true).show()
                                }
                        }
                    } else {
                        //Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        Toasty.error(this, "Error en el registro verificar datos", Toast.LENGTH_SHORT, true).show()

                    }
                }
        }
    }

    private fun setMaxLengthErrorWatcher(
        editText: TextInputEditText,
        layout: com.google.android.material.textfield.TextInputLayout,
        maxLength: Int
    ) {
        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length >= maxLength) {
                    layout.error = "Máximo $maxLength caracteres alcanzado"
                } else {
                    layout.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}