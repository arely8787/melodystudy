package mx.edu.uttt.melodystudy1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import okhttp3.OkHttpClient
import mx.edu.uttt.melodystudy1.ui.navigation.MelodyStudyApp
import mx.edu.uttt.melodystudy1.ui.theme.MelodyStudyTheme

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MelodyStudyTheme {
                MelodyStudyApp(client)
            }
        }
    }
}