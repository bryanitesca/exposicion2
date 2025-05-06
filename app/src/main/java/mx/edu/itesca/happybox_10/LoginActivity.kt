package mx.edu.itesca.happybox_10

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.SignInButton
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        val emailEditText: TextInputEditText = findViewById(R.id.etEmail)
        val passwordEditText: TextInputEditText = findViewById(R.id.etPassword)
        val loginButton: MaterialButton = findViewById(R.id.btnLogin)
        val googleSignInButton: SignInButton = findViewById(R.id.googleSignInButton)
        val btnRegister: MaterialButton = findViewById(R.id.btnRegister)
        val tvForgotPassword: TextView = findViewById(R.id.tvForgotPassword)
        val rememberMeCheckBox: CheckBox = findViewById(R.id.rememberMeCheckBox)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toasty.info(this, "Por favor, ingresa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        Toasty.success(this, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT, true).show()
                        updateUI(user)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toasty.error(this, "Correo o contraseña incorrectos. Verifica tus datos.", Toast.LENGTH_SHORT, true).show()
                    }
                }
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            Toasty.info(this, "Funcionalidad no implementada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun signInWithGoogle() {
        // Configurar la opción de inicio de sesión con Google
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            getString(R.string.default_web_client_id) // Web Client ID de Firebase
        )
            .setNonce("nonce_${System.currentTimeMillis()}") // Opcional, mejora seguridad
            .build()

        // Crear la solicitud de credenciales
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Iniciando flujo de inicio de sesión con Google")
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                Log.d(TAG, "Credencial obtenida: ${credential.type}")

                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Log.d(TAG, "Token ID obtenido: ${googleIdTokenCredential.idToken}")
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    Log.e(TAG, "Credencial inesperada: ${credential.type}")
                    Toasty.error(this@LoginActivity, "Credencial no válida", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener credenciales: ${e.message}", e)
                Toasty.error(
                    this@LoginActivity,
                    "Error al iniciar con Google: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toasty.error(this, "Error verifica datos", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, Principal::class.java))
            finish()
        }
    }
}