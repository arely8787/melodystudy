package mx.edu.uttt.melodystudy1.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.uttt.melodystudy1.model.UsuarioSesion
import mx.edu.uttt.melodystudy1.network.Constants
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UsuarioApi {

    /** Registra un usuario. Devuelve UsuarioSesion o lanza excepción con el mensaje del servidor. */
    suspend fun registrar(
        apodo: String,
        contrasena: String,
        avatar: String,
        nacimiento: String   // "YYYY-MM-DD"
    ): UsuarioSesion = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("apodo",      apodo)
            put("contrasena", contrasena)
            put("avatar",     avatar)
            put("nacimiento", nacimiento)
        }.toString()
        postJson(Constants.REGISTER, payload)
    }

    /** Login. Devuelve UsuarioSesion o lanza excepción con el mensaje del servidor. */
    suspend fun login(apodo: String, contrasena: String): UsuarioSesion =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("apodo",      apodo)
                put("contrasena", contrasena)
            }.toString()
            postJson(Constants.LOGIN, payload)
        }

    // ── Helper interno ───────────────────────────────────────────────

    private fun postJson(endpoint: String, body: String): UsuarioSesion {
        val url  = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput      = true
            connectTimeout = 10_000
            readTimeout    = 15_000
        }

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code     = conn.responseCode
        val stream   = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        val json     = JSONObject(response)

        // El servidor devolvió un error
        if (json.has("error")) throw Exception(json.getString("error"))

        return UsuarioSesion(
            id          = json.getInt("id"),
            apodo       = json.getString("apodo"),
            avatar      = json.getString("avatar"),
            nacimiento  = json.getString("nacimiento"),
            nivel       = json.getInt("nivel"),
            progreso    = json.getDouble("progreso").toFloat()
        )
    }
}