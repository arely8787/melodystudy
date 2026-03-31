from flask import Flask, request, send_file
import edge_tts
import asyncio
import subprocess
import tempfile
import os
import hashlib
import random
from midiutil import MIDIFile

app = Flask(__name__)

# ── Configuración por género ──────────────────────────────────────────────────

GENERO_CONFIG = {
    'pop': {
        'tempo':       (90, 110),
        'patron':      [1, 1, 2, 1],
        'vel_acordes': 52,
        'vel_melodia': 78,
        'vel_bajo':    62,
        'peso_piano':  1.6,
        'escala_idx':  None,   # libre (hash del texto)
        'rate_voz':    '-8%',
        'pitch_voz':   '+2Hz',
    },
    'rock': {
        'tempo':       (120, 145),
        'patron':      [1, 1, 1, 1],
        'vel_acordes': 65,
        'vel_melodia': 85,
        'vel_bajo':    72,
        'peso_piano':  1.4,
        'escala_idx':  None,
        'rate_voz':    '-4%',
        'pitch_voz': '-1Hz',
    },
    'balada': {
        'tempo':       (55, 72),
        'patron':      [2, 2, 4, 2],
        'vel_acordes': 42,
        'vel_melodia': 65,
        'vel_bajo':    50,
        'peso_piano':  2.0,
        'escala_idx':  None,
        'rate_voz':    '-12%',
        'pitch_voz':   '+3Hz',
    },
    'clasica': {
        'tempo':       (60, 80),
        'patron':      [2, 1, 1, 2, 1],
        'vel_acordes': 48,
        'vel_melodia': 70,
        'vel_bajo':    55,
        'peso_piano':  2.2,
        'escala_idx':  None,
        'rate_voz':    '-10%',
        'pitch_voz':   '+4Hz',
    },
}

def normalizar_genero(genero: str) -> str:
    """Normaliza el nombre del género al key del diccionario."""
    g = genero.lower().strip()
    if 'cl' in g:   return 'clasica'
    if 'bal' in g:  return 'balada'
    if 'rock' in g: return 'rock'
    return 'pop'   # pop es el default

# ── Voz ───────────────────────────────────────────────────────────────────────

import asyncio

async def generar_voz(text: str, output_path: str, rate: str, pitch: str):
    for intento in range(3):
        try:
            communicate = edge_tts.Communicate(
                text,
                voice="es-MX-DaliaNeural",
                rate=rate,
                pitch=pitch
            )

            await asyncio.wait_for(
                communicate.save(output_path),
                timeout=20   # ⏱️ máximo 20 segundos
            )
            return

        except asyncio.TimeoutError:
            print(f"⏱️ Timeout en TTS intento {intento+1}")
        except Exception as e:
            print(f"Error TTS intento {intento+1}: {e}")

        await asyncio.sleep(1)

    raise Exception("Falló generación de voz")

# ── Análisis musical ──────────────────────────────────────────────────────────

def analizar_texto(text: str, genero: str = 'pop') -> dict:
    """Determina el perfil musical usando el género elegido + hash del texto."""
    cfg      = GENERO_CONFIG[genero]
    hash_val = int(hashlib.md5(text.encode()).hexdigest(), 16)

    # Tempo dentro del rango del género
    rng_tempo = random.Random(hash_val)
    tempo     = rng_tempo.randint(*cfg['tempo'])

    escalas = [
        ("Do Mayor",  0,  [[60,64,67],[67,71,74],[57,60,64],[65,69,72]], [72,74,76,74,72,71,69,67]),
        ("Re Mayor",  2,  [[62,66,69],[69,73,76],[59,62,66],[67,71,74]], [74,76,78,76,74,73,71,69]),
        ("Fa Mayor",  5,  [[65,69,72],[72,76,79],[62,65,69],[70,74,77]], [77,79,81,79,77,76,74,72]),
        ("Sol Mayor", 7,  [[67,71,74],[74,78,81],[64,67,71],[72,76,79]], [79,81,83,81,79,78,76,74]),
        ("La menor",  9,  [[57,60,64],[64,67,71],[60,64,67],[53,57,60]], [69,71,72,71,69,67,65,64]),
        ("Re menor",  2,  [[62,65,69],[69,72,76],[65,69,72],[57,60,64]], [74,76,77,76,74,72,71,69]),
        ("Mi menor",  4,  [[64,67,71],[71,74,78],[67,71,74],[59,62,66]], [76,78,79,78,76,74,72,71]),
        ("Si menor",  11, [[59,62,66],[66,69,73],[62,66,69],[54,57,61]], [71,73,74,73,71,69,67,66]),
    ]

    # Rock/Pop usan escalas mayores (0-3), Balada/Clásica menores (4-7)
    if genero in ('rock', 'pop'):
        escala = escalas[hash_val % 4]
    else:
        escala = escalas[4 + (hash_val % 4)]

    return {
        'tempo':   tempo,
        'escala':  escala,
        'patron':  cfg['patron'],
        'seed':    hash_val,
        'cfg':     cfg,
    }

# ── MIDI ──────────────────────────────────────────────────────────────────────

def generar_midi_piano(duracion_segundos: float, output_path: str,
                        text: str, genero: str = 'pop'):
    """Genera MIDI con estilo propio de cada género."""
    analisis     = analizar_texto(text, genero)
    tempo        = analisis['tempo']
    escala       = analisis['escala']
    patron       = analisis['patron']
    seed         = analisis['seed']
    cfg          = analisis['cfg']

    print(f"Género: {genero} | Tonalidad: {escala[0]} | Tempo: {tempo} BPM")

    acordes      = escala[2]
    melodia_base = escala[3]

    rng     = random.Random(seed)
    melodia = []
    for i in range(32):
        nota_base = melodia_base[i % len(melodia_base)]
        # Rock: saltos más amplios; clásica/balada: más suaves
        variaciones = {
            'rock':   [-3, -2, -1, 0, 0, 1, 2, 3],
            'pop':    [-2, -1,  0, 0, 0, 1, 2],
            'balada': [-1,  0,  0, 0, 1],
            'clasica':[-1,  0,  0, 0, 1],
        }
        variacion = rng.choice(variaciones.get(genero, [-2,-1,0,0,0,1,2]))
        melodia.append(max(48, min(84, nota_base + variacion)))

    # Rock añade una pista extra de "ritmo" (hi-hat simulado con nota alta)
    num_pistas = 4 if genero == 'rock' else 3
    midi = MIDIFile(num_pistas)
    for pista in range(num_pistas):
        midi.addTempo(pista, 0, tempo)

    # Instrumentos MIDI por género
    instrumentos = {
        'pop':    [0,  0,  32],      # Piano, Piano, Bajo
        'rock':   [25, 30, 34, 0],   # Guitarra acústica, Distorsión, Bajo eléctrico, Percusión
        'balada': [0,  48, 43],      # Piano, Cuerdas, Cello
        'clasica':[0,  48, 42],      # Piano, Cuerdas, Viola
    }
    for pista, prog in enumerate(instrumentos.get(genero, [0,0,32])):
        midi.addProgramChange(pista, pista, 0, prog)

    duracion_midi = min(duracion_segundos, 60.0)
    beats_totales = int((duracion_midi * tempo) / 60)

    beat    = 0
    mel_idx = 0
    pat_idx = 0

    while beat < beats_totales:
        idx_acorde = (beat // 4) % len(acordes)
        acorde     = acordes[idx_acorde]

        # Acordes (pista 0)
        dur_acorde = 3.8 if genero in ('balada', 'clasica') else 3.5
        for nota in acorde:
            midi.addNote(0, 0, nota, beat, dur_acorde, cfg['vel_acordes'])

        # Melodía (pista 1)
        duracion_nota = patron[pat_idx % len(patron)]
        nota_mel      = melodia[mel_idx % len(melodia)]
        midi.addNote(1, 1, nota_mel, beat, duracion_nota * 0.9, cfg['vel_melodia'])
        mel_idx += 1
        pat_idx += 1

        # Bajo (pista 2) — rock pulsa en cada beat, otros en tiempos fuertes
        if genero == 'rock' or beat % 4 == 0:
            nota_bajo = acorde[0] - 12
            dur_bajo  = 0.9 if genero == 'rock' else 3.8
            midi.addNote(2, 2, nota_bajo, beat, dur_bajo, cfg['vel_bajo'])

        # Ritmo extra para rock (pista 3, canal 9 = percusión MIDI)
        if genero == 'rock' and num_pistas == 4:
            # Hi-hat en cada corchea (beat y beat+0.5)
            midi.addNote(3, 9, 42, beat,       0.4, 55)   # Hi-hat cerrado
            midi.addNote(3, 9, 42, beat + 0.5, 0.4, 45)
            if beat % 4 == 0:
                midi.addNote(3, 9, 35, beat, 0.9, 75)     # Bombo
            if beat % 4 == 2:
                midi.addNote(3, 9, 38, beat, 0.9, 70)     # Caja

        beat += duracion_nota

    with open(output_path, 'wb') as f:
        midi.writeFile(f)

# ── FluidSynth ────────────────────────────────────────────────────────────────

def midi_a_wav(midi_path: str, wav_path: str) -> bool:
    soundfonts = [
        r"C:\fluidsynth\FluidR3_GM.sf2",
        "/usr/share/soundfonts/FluidR3_GM.sf2",
        "/usr/share/soundfonts/FluidR3_GS.sf2",
    ]
    sf = next((s for s in soundfonts if os.path.exists(s)), None)
    if not sf:
        print("SoundFont no encontrado, sin piano")
        return False

    result = subprocess.run([
        'fluidsynth', '-ni',
        '-F', wav_path,
        '-r', '44100',
        sf, midi_path
    ], capture_output=True, text=True)

    ok = (result.returncode == 0
          and os.path.exists(wav_path)
          and os.path.getsize(wav_path) > 1000)
    print(f"Piano generado: {ok}")
    return ok

# ── Endpoint TTS ──────────────────────────────────────────────────────────────

@app.route('/tts', methods=['POST'])
def tts():
    print("\n================ NUEVA REQUEST =================")

    data   = request.get_json()
    text   = data.get('text',   '')  if data else ''
    genero = data.get('genero', 'pop') if data else 'pop'

    if not text:
        print("❌ No text provided")
        return {'error': 'No text provided'}, 400

    genero = normalizar_genero(genero)
    cfg    = GENERO_CONFIG[genero]

    print(f"▶ TTS — género: {genero}")
    print(f"Texto length: {len(text)}")

    tmpdir = tempfile.mkdtemp()
    print(f"📁 Temp dir: {tmpdir}")

    try:
        # ── 1. GENERAR VOZ ─────────────────────────────
        print("🔵 Paso 1: Generando voz...")

        if len(text) > 250:
            text = text[:500]

        voz_path = os.path.join(tmpdir, 'voz.mp3')

        asyncio.run(
            generar_voz(
                text,
                voz_path,
                cfg['rate_voz'],
                normalizar_pitch(cfg['pitch_voz'])
            )
        )

        if not os.path.exists(voz_path):
            raise Exception("No se generó voz.mp3")

        print(f"✅ Voz generada: {os.path.getsize(voz_path)} bytes")

        # ── 2. MP3 → WAV ───────────────────────────────
        print("🔵 Paso 2: Convirtiendo a WAV...")

        voz_wav = os.path.join(tmpdir, 'voz.wav')

        r = subprocess.run(
            ['ffmpeg', '-y', '-i', voz_path, voz_wav],
            capture_output=True, text=True
        )

        if r.returncode != 0:
            print("❌ Error ffmpeg:", r.stderr[-300:])
            raise Exception("Falló conversión a WAV")

        print("✅ WAV generado")

        # ── 3. EFECTOS ────────────────────────────────
        print("🔵 Paso 3: Aplicando efectos...")

        voz_fx  = os.path.join(tmpdir, 'voz_fx.mp3')

        eq_filtros = {
            'pop': 'equalizer=f=180:width_type=o:width=2:g=3,highpass=f=80,volume=2.5',
            'rock': 'equalizer=f=100:width_type=o:width=2:g=4,highpass=f=100,volume=2.8',
            'balada': 'equalizer=f=200:width_type=o:width=2:g=4,highpass=f=60,volume=2.3',
            'clasica': 'equalizer=f=250:width_type=o:width=2:g=3,highpass=f=60,volume=2.2',
        }

        r = subprocess.run(
            ['ffmpeg', '-y', '-i', voz_wav, '-af', eq_filtros[genero], voz_fx],
            capture_output=True, text=True
        )

        if r.returncode != 0:
            print("⚠️ EQ falló, usando volumen simple")
            subprocess.run(
                ['ffmpeg', '-y', '-i', voz_wav, '-af', 'volume=2.5', voz_fx],
                capture_output=True
            )

        print("✅ Efectos aplicados")

        # ── 4. DURACIÓN ───────────────────────────────
        print("🔵 Paso 4: Calculando duración...")

        dur_cmd = subprocess.run([
            'ffprobe', '-v', 'error',
            '-show_entries', 'format=duration',
            '-of', 'default=noprint_wrappers=1:nokey=1',
            voz_fx
        ], capture_output=True, text=True)

        print("ffprobe output:", dur_cmd.stdout)

        duracion = float(dur_cmd.stdout.strip())

        print(f"✅ Duración: {duracion:.2f}s")

        # ── 5. MIDI ───────────────────────────────────
        print("🔵 Paso 5: Generando MIDI...")

        midi_path   = os.path.join(tmpdir, 'musica.mid')
        musica_wav  = os.path.join(tmpdir, 'musica.wav')
        musica_mp3  = os.path.join(tmpdir, 'musica.mp3')
        output_path = os.path.join(tmpdir, 'output.mp3')

        generar_midi_piano(duracion, midi_path, text, genero)

        print("✅ MIDI generado")

        # ── 6. PIANO ──────────────────────────────────
        print("🔵 Paso 6: Generando piano...")

        piano_ok = midi_a_wav(midi_path, musica_wav)

        print(f"Piano OK: {piano_ok}")

        if piano_ok:
            print("🔵 Paso 7: Convirtiendo música a MP3...")

            r2 = subprocess.run([
                'ffmpeg', '-y',
                '-stream_loop', '-1',
                '-i', musica_wav,
                '-t', str(duracion),
                '-af', f'volume={cfg["peso_piano"] * 1.5:.1f}',
                '-q:a', '4',
                musica_mp3
            ], capture_output=True, text=True)

            if r2.returncode != 0:
                print("❌ Error música:", r2.stderr[-300:])
                piano_ok = False

        # ── 7. MEZCLA ────────────────────────────────
        print("🔵 Paso 8: Mezcla final...")

        if piano_ok:
            r3 = subprocess.run([
                'ffmpeg', '-y',
                '-i', voz_fx,
                '-i', musica_mp3,
                '-filter_complex',
                f'[0][1]amix=inputs=2:duration=first',
                '-q:a', '2',
                output_path
            ], capture_output=True, text=True)

            if r3.returncode != 0:
                print("❌ Error mezcla:", r3.stderr[-300:])
                output_path = voz_fx
            else:
                print(f"✅ Mezcla OK: {os.path.getsize(output_path)} bytes")
        else:
            print("⚠️ Sin música, solo voz")
            output_path = voz_fx

        # ── FINAL ────────────────────────────────────
        print("🎧 Enviando archivo...")

        print(f"📦 Archivo final existe: {os.path.exists(output_path)}")
        print(f"📦 Tamaño final: {os.path.getsize(output_path)} bytes")

        return send_file(
            output_path,
            mimetype='audio/mpeg',
            as_attachment=True
        )

    except Exception as e:
        print(f"❌ ERROR GENERAL: {e}")
        import traceback
        traceback.print_exc()
        return {'error': str(e)}, 500

def normalizar_pitch(pitch: str) -> str:
    if not pitch.startswith(('+', '-')):
        return f'+{pitch}'
    return pitch

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)