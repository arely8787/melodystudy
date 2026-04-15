# 🎵 MelodyStudy

Aplicación educativa que convierte temas de estudio en canciones generadas con Inteligencia Artificial. El usuario escribe un tema, elige un género musical y la app genera una canción con letra educativa, audio sintetizado y un examen de comprensión automático.

---

## 🧠 ¿Cómo funciona?
```
Usuario escribe un tema  
        ↓
IA genera la letra (Groq)
        ↓
Coqui TTS genera voz base (español neuronal)
        ↓
RVC convierte la voz al timbre personalizado (Ifu)
        ↓
MusicGen genera música instrumental
        ↓
Mezcla inteligente (ducking + mastering)
        ↓
Se guarda en BD + se genera examen automáticamente
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
                                    │  MySQL   │  │  Audio AI    │  Groq AI │
                                    │  Base de │  │  Python +    │  │  (LLM)   │
                                    │  Datos   │  │  Coqui TTS   │  └──────────┘
                                    └──────────┘  │  RVC (Voice) │ 
                                                  │  MusicGen    │ 
                                                  └──────────────┘

```

---

## 📁 Estructura del repositorio
```
melodystudy/
├── Android/          # App móvil Android (Kotlin + Jetpack Compose)
├── Backend/          # API REST (Java 21 + Javalin + MySQL)
├── Tts/              # Servicio de audio IA (Coqui + RVC + MusicGen + mezcla)
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

## 3. Servicio de Audio IA (Python)

Este servicio genera canciones completas usando múltiples modelos de IA:

- **Coqui TTS** → genera voz neuronal en español
- **RVC (Retrieval-based Voice Conversion)** → convierte la voz al timbre personalizado (ej: Ifu)
- **MusicGen** → genera música instrumental
- **FFmpeg** → mezcla, efectos, compresión y mastering final

### Instalación

```bash
cd Tts

python -m venv venv
venv\Scripts\activate

pip install -r requirements.txt
```
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
| 🎤 Canción con IA | Voz neuronal + conversión de timbre + música generada automáticamente |
| 📝 Examen automático | La IA genera 5 preguntas de opción múltiple |
| ⭐ Sistema de XP | Gana puntos al guardar canciones y aprobar exámenes |
| 🏆 Niveles | Sube de nivel acumulando XP |
| 📚 Historial | Accede a tus canciones y calificaciones anteriores |

---


## ⭐ Sistema de XP y Niveles

MelodyStudy usa un sistema de progresión local — el servidor solo guarda
`nivel` y `progreso` (0.0 – 1.0). Toda la lógica de XP vive en el cliente.

### Acciones que dan XP

| Acción | XP ganado |
|---|---|
| Guardar una canción | +5 XP |
| Aprobar un examen (≥ 60/100) | +10 XP |
| Examen perfecto (100/100) | +20 XP |

### Tabla de niveles

| Nivel | Título | XP necesario |
|---|---|---|
| 1 | Aprendiz | 100 XP |
| 2 | Estudiante | 200 XP |
| 3 | Curioso | 350 XP |
| 4 | Explorador | 500 XP |
| 5 | Melómano | 700 XP |
| 6 | Compositor | 950 XP |
| 7 | Maestro | 1,250 XP |
| 8 | Virtuoso | 1,600 XP |
| 9 | Leyenda | 2,000 XP |
| 10 | MelodyMaster | 2,000 XP *(máximo)* |

### ¿Cómo funciona el cálculo?

El progreso dentro de un nivel se representa como un valor entre `0.0` y `1.0`:
```
xpActual  =  progreso  ×  xpMaxDelNivel
```

Ejemplo: nivel 2, progreso 0.35
```
xpActual = 0.35 × 200 = 70 XP
Faltan   = 200 - 70   = 130 XP para nivel 3
```

Al ganar XP, si se supera el máximo del nivel actual se sube automáticamente:
```
xpNuevo = xpActual + xpGanado

Si xpNuevo ≥ xpMaxDelNivel:
    xpNuevo -= xpMaxDelNivel
    nivel++
    (repite si sigue subiendo varios niveles)

nuevoProgreso = xpNuevo / xpMaxDelNivel(nivelNuevo)
```

Esto permite subir varios niveles de un solo golpe si el XP ganado
es suficiente. Al llegar al nivel 10 el progreso se bloquea en `1.0`.

### Flujo completo
```
Usuario aprueba examen (80/100)
        ↓
xpDeExamen(80) → +10 XP
        ↓
aplicarXP(nivelActual, progresoActual, 10)
        ↓
UsuarioViewModel actualiza sesión local → header se refresca
        ↓
PUT /usuarios/{id}/xp → BD guarda nuevo nivel y progreso
```

---

---

## 👤 Registro y personalización

Al crear una cuenta el usuario configura su perfil con los siguientes datos:

| Campo | Descripción |
|---|---|
| Avatar | Emoji seleccionado de una galería de 10 opciones |
| Apodo | Nombre que se muestra en la app |
| Contraseña | Campo con opción de mostrar/ocultar |
| Fecha de nacimiento | Seleccionada desde un calendario |

### Galería de avatares disponibles

El usuario elige uno de los siguientes emojis como su avatar personal:
```
🐱  🐶  🦊  🐼  🐨
🦁  🐸  🐧  🦄  🐙
```

El avatar seleccionado se muestra en:
- La pantalla de registro (previsualización grande)
- El header de la app junto al apodo y nivel
- La pantalla de perfil

### Flujo de registro
```
1. Elegir avatar (emoji)
2. Escribir apodo
3. Escribir contraseña
4. Seleccionar fecha de nacimiento
        ↓
POST /register → BD guarda el usuario
        ↓
Redirige al Login
```

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
- Coqui TTS (voz neuronal)
- RVC (voice conversion)
- MusicGen (generación musical)
- FFmpeg (procesamiento de audio)
- Flask

---

## 🎧 Pipeline de generación de audio

El sistema genera cada canción en múltiples etapas:

1. Coqui TTS → voz base (~6s)
2. RVC → conversión de voz (~20s)
3. MusicGen → música (~40s)
4. Ducking → mezcla inteligente
5. Mastering → compresión y limitación final

⏱️ Tiempo total: ~70–80 segundos por canción

---

## 🤖 Inteligencia Artificial aplicada

MelodyStudy integra múltiples modelos de IA especializados:

- LLM (Groq) → generación de contenido educativo
- TTS neuronal → síntesis de voz realista
- Voice Conversion (RVC) → personalización de voz
- Generación musical (MusicGen) → creación de acompañamiento
- DSP (FFmpeg) → mezcla y mastering automático

---

## 👨‍💻 Desarrollado en

Universidad Tecnológica de Tula-Tepeji — UTTT  
Proyecto integrador — 2026
