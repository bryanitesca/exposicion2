package mx.edu.itesca.happybox_10

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startTime = System.currentTimeMillis()
        setContentView(R.layout.activity_main)
        android.util.Log.d("MainActivity", "Tiempo de carga del layout: ${System.currentTimeMillis() - startTime} ms")

        val regaloTop: ImageView = findViewById(R.id.regaloTop)
        val imLetras: ImageView = findViewById(R.id.imLetras)
        Glide.with(this).load(R.drawable.regalito).centerCrop().into(regaloTop)
        Glide.with(this).load(R.drawable.letrero).centerCrop().into(imLetras)

        val button: Button = findViewById(R.id.Inicio)
        button.setOnClickListener {
            val intent: Intent = Intent(this, Principal::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}