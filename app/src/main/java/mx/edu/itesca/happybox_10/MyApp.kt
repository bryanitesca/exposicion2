package mx.edu.itesca.happybox_10

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 1) Inicializa Firebase
        FirebaseApp.initializeApp(this)
        // 2) Instala el Debug provider de App Check
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
    }
}
