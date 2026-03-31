# 🎵 MelodyStudy

Aplicación educativa que convierte temas de estudio en canciones generadas con Inteligencia Artificial. El usuario escribe un tema, elige un género musical y la app genera una canción con letra educativa, audio sintetizado y un examen de comprensión automático.

---

## 🧠 ¿Cómo funciona?
```
Usuario escribe un tema  →  IA genera la letra  →  TTS convierte a audio
        ↓
   Se guarda en BD  →  IA genera examen  →  Usuario estudia y se evalúa
        ↓
   Sistema de XP y niveles según desempeño
```

---

## 🏗️ Arquitectura
```
┌─────────────────────┐       HTTP/REST        ┌──────────────────────┐
│   Android App       │ ◄───────────────────► │  Backend API         │
│   Kotlin + Compose  │                        │  Java + Javalin      │
└─────────────────────┘                        └──────────┬───────────┘
                                                          │
                                          ┌───────────────┼───────────────┐
                                          ▼               ▼               ▼
                                    ┌──────────┐  ┌──────────────┐  ┌──────────┐
                                    │  MySQL   │  │  TTS Service │  │  Groq AI │
                                    │  Base de │  │  Python +    │  │  (LLM)   │
                                    │  Datos   │  │  Kokoro      │  └──────────┘
                                    └──────────┘  └──────────────┘
```

---

## 📁 Estructura del repositorio
```
melodystudy/
├── Android/          # App móvil Android (Kotlin + Jetpack Compose)
├── Backend/          # API REST (Java 21 + Javalin + MySQL)
├── Tts/              # Servicio de texto a voz (Python + Kokoro)
├── Database/         # Script SQL de la base de datos
└── README.md
```

---

## ✅ Requisitos previos

| Herramienta       | Versión mínima | Descarga |
|-------------------|----------------|----------|
| Android Studio    | Hedgehog+      | [developer.android.com](https://developer.android.com/studio) |
| Java JDK          | 21+            | [adoptium.net](https://adoptium.net) |
| Python            | 3.10+          | [python.org](https://www.python.org) |
| MySQL             | 8.0+           | [mysql.com](https://dev.mysql.com/downloads/) |
| Groq API Key      | —              | [console.groq.com](https://console.groq.com) |

---

## 🚀 Instalación paso a paso

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/melodystudy.git
cd melodystudy
```

---

### 2. Base de datos

Importa el script en MySQL Workbench o desde consola:
```bash
mysql -u root -p < Database/melodystudy.sql
```

Verifica que se creó la base de datos `melodydb` con las tablas:
- `usuarios`
- `canciones`
- `examenes`
- `preguntas`

---

### 3. Servicio TTS (Python)
```bash
cd Tts

# Crear entorno virtual
python -m venv venv

# Activar entorno (Windows)
venv\Scripts\activate

# Activar entorno (Mac/Linux)
source venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt

# Levantar servidor
python server.py
```

> ✅ El servicio TTS corre en `http://localhost:5050`

---

### 4. Backend (Java)

Abre la carpeta `Backend/` en **IntelliJ IDEA**.

Configura las variables de entorno antes de correr (en IntelliJ: `Run → Edit Configurations → Environment variables`):
```
GROQ_KEY=tu_api_key_de_groq
SERVER_HOST=tu_ip_local        # ej: 172.20.10.3
```

Verifica la conexión a MySQL en `src/main/java/mx/edu/uttt/config/Database.java`:
```java
private static final String URL  = "jdbc:mysql://localhost:3306/melodydb";
private static final String USER = "root";
private static final String PASS = "tu_contraseña";
```

Corre la clase `Main.java`.

> ✅ La API REST corre en `http://localhost:7000`  
> ✅ Verifica en el navegador: `http://localhost:7000` → debe mostrar `MelodyStudy backend activo`

---

### 5. App Android

Abre la carpeta `Android/` en **Android Studio**.

Edita el archivo:
```
app/src/main/java/mx/edu/uttt/melodystudy1/network/Constants.kt
```

Cambia el `HOST` según tu caso:
```kotlin
// Emulador de Android Studio
private const val HOST = "10.0.2.2"

// Celular físico (usa la IP de tu PC en la red local)
private const val HOST = "172.20.10.3"
```

Conecta tu celular o inicia un emulador y presiona **Run**.

---

## 🌐 Red local (celular físico)

Para probar con un celular físico, PC y celular deben estar en la **misma red WiFi**.
```
Celular (app)  ──WiFi──►  Router/Hotspot  ◄──WiFi──  PC (backend)
                                                       └─ IP: 172.20.10.3
```

1. Conecta ambos dispositivos al mismo WiFi o hotspot
2. Obtén la IP de tu PC: ejecuta `ipconfig` en CMD y busca el adaptador WiFi
3. Usa esa IP en `Constants.kt` y en la variable `SERVER_HOST` del backend
4. Abre el puerto 7000 en el firewall de Windows:
```cmd
netsh advfirewall firewall add rule name="MelodyStudy 7000" dir=in action=allow protocol=TCP localport=7000
```

---

## 🎮 Funcionalidades

| Función | Descripción |
|---|---|
| 🎵 Generar canción | La IA crea letra educativa según tema y género |
| 🔊 Audio sintetizado | El TTS convierte la letra en MP3 con Kokoro |
| 📝 Examen automático | La IA genera 5 preguntas de opción múltiple |
| ⭐ Sistema de XP | Gana puntos al guardar canciones y aprobar exámenes |
| 🏆 Niveles | Sube de nivel acumulando XP |
| 📚 Historial | Accede a tus canciones y calificaciones anteriores |

---

## 🗄️ Base de datos
```
usuarios
   └── canciones
           └── examenes
                   └── preguntas
```

| Tabla | Descripción |
|---|---|
| `usuarios` | Registro, login, nivel y XP |
| `canciones` | Letra, género, audio_url por usuario |
| `examenes` | Examen vinculado a cada canción + calificación |
| `preguntas` | 5 preguntas de opción múltiple por examen |

---

## 🔑 Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `GROQ_KEY` | API Key de Groq para generación con IA | `gsk_...` |
| `OPENAI_KEY` | API Key de OpenAI (alternativa) | `sk-...` |
| `SERVER_HOST` | IP de la PC en la red local | `172.20.10.3` |

---

## 🛠️ Tecnologías utilizadas

**Android**
- Kotlin + Jetpack Compose
- Navigation Component
- OkHttp / HttpURLConnection
- MediaPlayer

**Backend**
- Java 21
- Javalin (servidor HTTP)
- MySQL + JDBC
- Groq API (LLM)

**TTS**
- Python 3
- Kokoro (modelo ONNX)
- Flask

---

## 👨‍💻 Desarrollado en

Universidad Tecnológica de Tula-Tepeji — UTTT  
Proyecto integrador — 2026