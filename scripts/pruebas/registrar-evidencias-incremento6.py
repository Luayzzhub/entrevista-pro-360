"""Consolida evidencias ejecutadas; no instala dependencias ni modifica Git."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / ".tools"
DEST = ROOT / "docs/evidencias"

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True, encoding="utf-8").strip()

def log(nombre):
    return (TOOLS / nombre).read_text(encoding="utf-8", errors="replace")

def backend(nombre):
    texto = log(nombre)
    assert "[INFO] BUILD SUCCESS" in texto[-1000:], nombre
    datos = re.findall(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)", texto)
    total, fallas, errores, omitidas = map(int, datos[-1])
    assert fallas == errores == 0
    return dict(descubiertas=total, ejecutadas=total-omitidas, omitidas=omitidas, fallas=fallas, errores=errores, log_local=".tools/"+nombre)

def main():
    head = git("rev-parse", "HEAD")
    assert head == "4d00e56b0f905f1a2b0770453b913538bd9519c8"
    assert not git("diff", "--cached", "--name-only")
    assert not git("diff", "--name-only", "--", "backend/src/main/resources/db/migration")
    migraciones = sorted(p.name for p in (ROOT/"backend/src/main/resources/db/migration").glob("*.sql"))
    assert migraciones == ["V1__esquema_inicial.sql", "V2__catalogo_semilla.sql"]
    assert git("check-ignore", ".tools/inc6-mysql-test.log")
    h2 = backend("inc6-h2-full.log")
    mysql = backend("inc6-mysql-test.log")
    assert h2["descubiertas"] == mysql["descubiertas"] == 132
    assert h2["omitidas"] == 2 and mysql["omitidas"] == 0
    assert "BUILD SUCCESS" in log("inc6-package.log")[-1000:]
    angular = log("inc6-angular-build.log")
    assert "Application bundle generation complete" in angular and not re.search(r"WARNING|ERROR", angular)
    media = log("inc6-media.log")
    assert "pass 6" in media and "fail 0" in media
    browser = json.loads((DEST/"pruebas-browser-incremento-6.json").read_text(encoding="utf-8-sig"))
    restart = json.loads((DEST/"recuperacion-jvm-incremento-6.json").read_text(encoding="utf-8-sig"))
    assert browser["correcto"] and browser["total_casos"] == 3 and len(browser["stt"]) == 6
    assert restart["correcto"] and restart["jvm_antes"] != restart["jvm_despues"]
    assert restart["contexto_despues"]["etapa_actual"] == "EVALUAR"
    schema = log("inc6-schema.txt")
    assert "8.4.13" in schema
    columnas = dict((k,int(v)) for k,v in re.findall(r"^(evaluaciones_star|metricas_comunicacion|preguntas|puestos|respuestas_video|sesiones_entrevista|transcripciones|usuarios)\t(\d+)\s*$",schema,re.M))
    assert len(columnas) == 8 and sum(columnas.values()) == 68
    suites = []
    for archivo in sorted((ROOT/"backend/target/surefire-reports").glob("TEST-*.xml")):
        elem = ET.parse(archivo).getroot()
        suites.append(dict(nombre=elem.attrib["name"], pruebas=int(elem.attrib["tests"]),
                           fallas=int(elem.attrib["failures"]), errores=int(elem.attrib["errors"]), omitidas=int(elem.attrib["skipped"])))
    assert sum(s["pruebas"] for s in suites) == 132
    assert all(s["fallas"] == s["errores"] == s["omitidas"] == 0 for s in suites)
    jar = ROOT/"backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar"
    evidencia = {
        "incremento":6,"fecha_utc":datetime.now(timezone.utc).isoformat(),"resultado":"VERIFICADO_PENDIENTE_AUDITORIA",
        "git":{"rama":git("branch","--show-current"),"head":head,"incremento_5_auditoria":"APROBADO_SIN_OBSERVACIONES",
               "incremento_5_publicado":"origin/main","antes_incremento_6":"## main...origin/main (limpio)",
               "incremento_6_sin_commit":True,"indice_vacio":True,"tools_ignorado":True},
        "alcance":{"RF-13":"Verificado con HTTP STT local real","RF-14":"Verificado MET-1.0","RF-20":"En progreso; etapas 1 a 3 y reintento",
                   "limite_final":"TRANSCRITO / ultimo_hito_valido=TRANSCRITO / etapa_actual=EVALUAR",
                   "LLM_invocado":False,"sesiones_completadas_por_worker":False},
        "backend":{"h2":h2,"mysql84":mysql,"suites_mysql":suites},
        "frontend":{"test_media":{"ejecutadas":6,"fallas":0},"build":"correcto, sin advertencias de presupuesto",
                    "chrome":browser["navegador"],"casos_reales":browser["casos"],"errores_javascript":browser["errores_javascript"],
                    "evidencia":"pruebas-browser-incremento-6.json"},
        "recuperacion_jvm":{"correcto":True,"terminacion_durante_stt":True,"lease_expirado_por_sql_para_acelerar":True,
                           "audio_no_repetido":True,"evidencia":"recuperacion-jvm-incremento-6.json"},
        "esquema":{"mysql":"8.4.13","tablas_negocio":8,"columnas_negocio":68,"columnas_por_tabla":columnas,
                   "migraciones":migraciones,"migraciones_modificadas":False,"ddl_auto":"validate"},
        "configuracion":{"worker_predeterminado":True,"intervalo_ms":2000,"heartbeat_ms":20000,"lease_segundos":90,
                         "backoff_segundos":[5,15],"timeout_ffmpeg_y_stt_segundos":60,
                         "stt_model":"gpt-4o-mini-transcribe-2025-12-15","browser_intervalo_ms":500,"browser_backoff_segundos":[1,1]},
        "multimedia":{"ffmpeg":"9.0.2","ffprobe":"9.0.2","wav":"pcm_s16le mono 16000 Hz","ffmpeg_real":True},
        "artefacto_jar_sha256":hashlib.sha256(jar.read_bytes()).hexdigest(),
        "limites":["S3Client y HttpClient reales sobre endpoints HTTP locales; no certifica IAM/bucket AWS ni cuenta/modelo OpenAI de produccion.",
                   "Chrome usa dispositivos emulados, no hardware fisico.",
                   "Flyway 11.7.2 emite recomendacion de actualizacion por MySQL 8.4; migracion y validate pasaron sin modificar dependencias.",
                   "La expiracion temporal en el experimento JVM se acelero por SQL; claim previo, terminacion y reanudacion fueron reales."],
        "aislamiento":{"unitarias_integracion":"ep360_incremento6_test; worker programado deshabilitado",
                       "chrome":"ep360_incremento6_browser; worker habilitado",
                       "reinicio_jvm":"ep360_incremento6_restart; bucket ep360-restart-local"}
    }
    (DEST/"pruebas-incremento-6.json").write_text(json.dumps(evidencia,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"correcto":True,"h2":h2["ejecutadas"],"mysql":mysql["ejecutadas"],"chrome":3,"indice_vacio":True}))

if __name__ == "__main__":
    main()

