"""Consolida la evidencia del Incremento 7 sin escribir en Git."""
from pathlib import Path
import subprocess, json, re, hashlib
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
ROOT=Path(__file__).resolve().parents[2]
BASE="f702fc4d03ff6e580e9cc92466c96a68798800db"
def git(*args):
    return subprocess.check_output(["git",*args],cwd=ROOT,text=True,encoding="utf-8").strip()
def log(name):
    return (ROOT/".tools"/name).read_text(encoding="utf-8",errors="replace")
def tests(name):
    s=log(name)
    assert "BUILD SUCCESS" in s[-1000:],name
    valores=re.findall(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)",s)[-1]
    total,fallas,errores,omitidas=map(int,valores)
    assert fallas==errores==0
    return dict(descubiertas=total,ejecutadas=total-omitidas,omitidas=omitidas,fallas=fallas,errores=errores,log_local=".tools/"+name)
def main():
    assert git("rev-parse","HEAD")==git("rev-parse","origin/main")==BASE
    assert not git("diff","--cached","--name-only")
    assert not git("diff","--name-only","--","backend/src/main/resources/db/migration")
    migraciones=sorted(p.name for p in (ROOT/"backend/src/main/resources/db/migration").glob("*.sql"))
    assert migraciones==["V1__esquema_inicial.sql","V2__catalogo_semilla.sql"]
    assert git("check-ignore",".tools/inc7-browser.log")
    h2=tests("inc7-h2.log");mysql=tests("inc7-mysql.log")
    assert h2["descubiertas"]==mysql["descubiertas"]==154 and h2["omitidas"]==2 and mysql["omitidas"]==0
    assert "BUILD SUCCESS" in log("inc7-package.log")[-1000:]
    build=log("inc7-angular-build.log")
    assert "Application bundle generation complete" in build and not re.search(r"WARNING|ERROR",build)
    assert "pass 6" in log("inc7-media.log") and "fail 0" in log("inc7-media.log")
    browser=json.loads((ROOT/"docs/evidencias/pruebas-browser-incremento-7.json").read_text(encoding="utf-8"))
    assert browser["correcto"] and browser["total_casos"]==9 and not browser["errores_javascript"]
    assert browser["llamadas_stt"]==4 and browser["llamadas_llm"]==7 and browser["puts_wav"]==4
    schema=log("inc7-schema.txt");assert "8.4.13" in schema
    columnas=dict((k,int(v)) for k,v in re.findall(r"^(evaluaciones_star|metricas_comunicacion|preguntas|puestos|respuestas_video|sesiones_entrevista|transcripciones|usuarios)\t(\d+)\s*$",schema,re.M))
    assert len(columnas)==8 and sum(columnas.values())==68 and columnas["evaluaciones_star"]==14
    suites=[]
    for p in sorted((ROOT/"backend/target/surefire-reports").glob("TEST-*.xml")):
        e=ET.parse(p).getroot()
        suites.append(dict(nombre=e.attrib["name"],pruebas=int(e.attrib["tests"]),fallas=int(e.attrib["failures"]),errores=int(e.attrib["errors"]),omitidas=int(e.attrib["skipped"])))
    assert sum(e["pruebas"] for e in suites)==154 and all(e["fallas"]==e["errores"]==e["omitidas"]==0 for e in suites)
    jar=ROOT/"backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar"
    evidencia={
        "incremento":7,"fecha_utc":datetime.now(timezone.utc).isoformat(),"resultado":"VERIFICADO_PENDIENTE_AUDITORIA",
        "git":{"rama":git("branch","--show-current"),"head":BASE,"incremento_6_auditoria":"APROBADO_SIN_OBSERVACIONES","incremento_6_publicado":"origin/main",
               "mensaje_commit_6":"feat: implementar worker durable PROC-1.0, extraccion WAV, transcripcion STT, metricas MET-1.0 y reintento MCK08",
               "antes_incremento_7":"## main...origin/main (limpio)","incremento_7_sin_commit":True,"indice_vacio":True,"tools_ignorado":True},
        "RF":{"RF-15":"Verificado","RF-16":"En progreso: feedback individual","RF-17":"En progreso: resultado propio y reproduccion","RF-20":"Verificado: etapas1-4 por respuesta"},
        "pipeline":{"etapas":["EXTRAER_AUDIO","TRANSCRIBIR","METRICAS","EVALUAR"],"final":"EVALUADO / EVALUADO / FINALIZADO",
                    "reintento_manual":"unico; EVALUAR conserva hito TRANSCRITO sin repetir audio/STT/MET","fallo_definitivo":"CANCELADA; sin promedio",
                    "pregunta3":"sesion EN_PROCESO; puntaje_promedio=null; fecha_fin=null; cierre reservado al Incremento8"},
        "backend":{"h2":h2,"mysql84":mysql,"suites_mysql":suites,"nuevas_pruebas":{"dominio_EVAL":7,"cliente_LLM":5,"integracion_evaluacion_y_firma":10}},
        "llm":{"modelo":"gpt-5.4-mini-2026-03-17","endpoint":"/v1/responses","store":False,"tools":False,"formato":"json_schema / evaluacion_eval_1_0 / strict=true",
               "entrada_campos":7,"metadatos_identidad":False,"timeout_total_segundos":60,"max_bytes_respuesta":2000000,
               "schema":"backend/src/main/resources/llm/eval-1.0.schema.json","validacion_y_puntaje":"domain/ValidadorEvaluacion",
               "salida_invalida":"SALIDA_EVALUACION_INVALIDA recuperable; no persiste fila"},
        "reproduccion":{"endpoint":"POST /api/v1/respuestas/{id}/reproduccion","autorizacion":"propietario JWT y HEAD privado","cache_control":"no-store",
                        "firma":"AWS SDK2 AwsS3V4Signer, signingClockOverride","segundos":300,"firma_criptografica_fixture":True,"manipulacion_rechazada":True,
                        "expiracion_rechazada":True,"video_ausente":"404 VIDEO_NO_DISPONIBLE; conserva resultado","infraestructura":"503 ALMACENAMIENTO_NO_DISPONIBLE",
                        "url_persistida":False},
        "frontend":{"medios":{"ejecutadas":6,"fallas":0},"build":"correcto, sin advertencias de presupuesto","chrome":browser["navegador"],
                    "casos":browser["casos"],"errores_javascript":browser["errores_javascript"],"evidencia":"pruebas-browser-incremento-7.json",
                    "STT":4,"LLM":7,"WAV":4},
        "esquema":{"mysql":"8.4.13","tablas":8,"columnas":68,"columnas_por_tabla":columnas,"ddl_auto":"validate","migraciones":migraciones,"V1_V2_modificadas":False,"V3":False},
        "jar_sha256":hashlib.sha256(jar.read_bytes()).hexdigest(),
        "aislamiento":{"maven":"ep360_incremento7_test; worker programado deshabilitado","chrome":"ep360_incremento7_browser; worker habilitado, intervalo500ms/backoff1-1s"},
        "limites":["S3/STT/LLM HTTP locales con clientes Java reales; cuentas, IAM y disponibilidad de modelos de produccion no certificadas.",
                   "Chrome con dispositivos emulados; no es certificacion de hardware fisico.",
                   "No se implementa cierre automatico, promedio global ni resumen de sesion del Incremento8.",
                   "Flyway11.7.2 recomienda actualizar por MySQL8.4; las migraciones y validate pasaron.",
                   "La API de firma del SDK2.31.54 esta marcada deprecated, pero la firma SigV4 equivalente fue verificada con Clock y sin nuevas dependencias."]
    }
    (ROOT/"docs/evidencias/pruebas-incremento-7.json").write_text(json.dumps(evidencia,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(dict(correcto=True,h2=h2["ejecutadas"],mysql=mysql["ejecutadas"],chrome=9,sin_staging=True)))
if __name__=="__main__":
    main()

