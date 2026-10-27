"""Consolida resultados ejecutados del Incremento 8, sin staging ni commit."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, re, subprocess
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
BASE="ca798f5d112469ccac07668e53959cb1feaed314"
def git(*args):
    return subprocess.check_output(["git",*args],cwd=ROOT,text=True,encoding="utf-8").strip()
def log(name):
    return (ROOT/".tools"/name).read_text(encoding="utf-8",errors="replace")
def pruebas(name):
    texto=log(name)
    assert "BUILD SUCCESS" in texto[-1000:],name
    values=re.findall(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)",texto)[-1]
    total,fallas,errores,omitidas=map(int,values)
    assert fallas==errores==0
    return dict(descubiertas=total,ejecutadas=total-omitidas,omitidas=omitidas,fallas=fallas,errores=errores,log_local=".tools/"+name)
def main():
    assert git("rev-parse","HEAD")==git("rev-parse","origin/main")==BASE
    assert not git("diff","--cached","--name-only")
    invariantes=["backend/pom.xml","frontend/package.json","frontend/package-lock.json","backend/src/main/resources/db/migration"]
    assert not git("diff","--name-only","--",*invariantes)
    migraciones=sorted(p.name for p in (ROOT/"backend/src/main/resources/db/migration").glob("*.sql"))
    assert migraciones==["V1__esquema_inicial.sql","V2__catalogo_semilla.sql"]
    assert git("check-ignore",".tools/inc8-browser.log")
    h2=pruebas("inc8-h2.log");mysql=pruebas("inc8-mysql.log")
    assert h2["descubiertas"]==mysql["descubiertas"]==170 and h2["omitidas"]==2 and mysql["omitidas"]==0
    assert "BUILD SUCCESS" in log("inc8-package.log")[-1000:]
    build=log("inc8-angular-build.log")
    assert "Application bundle generation complete" in build and not re.search(r"WARNING|ERROR",build)
    assert "pass 6" in log("inc8-media.log") and "fail 0" in log("inc8-media.log")
    browser_path=ROOT/"docs/evidencias/pruebas-browser-incremento-8.json"
    browser=json.loads(browser_path.read_text(encoding="utf-8"))
    assert browser["correcto"] and browser["total_casos"]==8 and not browser["errores_javascript"] and not browser["errores_consola"]
    completa=browser["sesion_completada"]
    assert completa["estado_sesion"]=="COMPLETADA" and completa["puntaje_promedio"]==81.25 and completa["respuestas_evaluadas"]==3
    assert completa["fecha_fin"] and completa["pregunta_habilitada"] is None
    assert [p["puntaje_general"] for p in completa["preguntas"]]==[75.00,81.25,87.50]
    assert browser["llamadas_stt"]==browser["llamadas_llm"]==4
    for campo in ["public_id_cloud","referencia_audio","procesamiento_contexto"]:
        assert campo not in json.dumps(completa)
    schema=log("inc8-schema.txt");assert "8.4.13" in schema
    columnas=dict((k,int(v)) for k,v in re.findall(r"^(evaluaciones_star|metricas_comunicacion|preguntas|puestos|respuestas_video|sesiones_entrevista|transcripciones|usuarios)\t(\d+)\s*$",schema,re.M))
    assert len(columnas)==8 and sum(columnas.values())==68 and columnas["evaluaciones_star"]==14
    suites=[]
    for p in sorted((ROOT/"backend/target/surefire-reports").glob("TEST-*.xml")):
        s=ET.parse(p).getroot()
        suites.append(dict(nombre=s.attrib["name"],pruebas=int(s.attrib["tests"]),fallas=int(s.attrib["failures"]),errores=int(s.attrib["errors"]),omitidas=int(s.attrib["skipped"])))
    assert sum(s["pruebas"] for s in suites)==170 and all(s["fallas"]==s["errores"]==s["omitidas"]==0 for s in suites)
    chrome=json.loads((ROOT/".tools/inc8-chrome-runtime.json").read_text(encoding="utf-8-sig"))
    jar=ROOT/"backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar"
    evidencia={
        "incremento":8,"fecha_utc":datetime.now(timezone.utc).isoformat(),"resultado":"VERIFICADO_PENDIENTE_AUDITORIA",
        "git":{"rama":"main","head":BASE,"incremento_7_auditoria":"AUDITADO_APROBADO_SIN_OBSERVACIONES",
               "mensaje_commit_7":"feat: implementar evaluacion EVAL-1.0 COM-1.0, reproduccion firmada PA-04 y resultado MCK09",
               "incremento_7_publicado":"origin/main; HEAD y ls-remote coinciden",
               "antes_incremento_8":"## main...origin/main (limpio)","incremento_8_sin_commit":True,"indice_vacio":True,"tools_ignorado":True},
        "RF":{"RF-16":"Verificado: feedback individual y resumen de sesion","RF-17":"En progreso: detalle/resumen propios, historial integral pendiente"},
        "dominio":{"metodo":"SesionEntrevista.calcularPromedioGlobal","capa":"domain pura",
                   "precondiciones":"EN_PROCESO, tres preguntas activas distintas del puesto (orden1/2/3), tres respuestas EVALUADO con transcripcion, MET-1.0 y EVAL-1.0/COM-1.0 validas",
                   "formula":"BigDecimal(sum(puntaje_general)/3), escala2 HALF_UP",
                   "casos":[{"puntajes":["75.00","81.25","87.50"],"promedio":"81.25"},
                            {"puntajes":["0.00","0.00","6.25"],"promedio":"2.08"},
                            {"puntajes":["0.00","0.00","0.00"],"promedio":"0.00"}],
                   "sin_relleno_con_ceros":True,"terminales_no_recalculadas":True,"fecha_fin":"UTC a segundos, nunca anterior al inicio"},
        "atomicidad":{"evaluacion_respuesta_y_cierre_mismo_commit":True,"locks":"sesion -> respuesta","cancelacion_durante_llm_P3":"CANCELADA, sin evaluacion P3 ni promedio",
                      "cancelacion_posterior_a_cierre":"409 SESION_YA_COMPLETADA, conserva promedio y fecha",
                      "fallo_definitivo_P3":"CANCELADA sin promedio, conserva P1/P2","nueva_sesion_tras_completar":"201",
                      "lecturas_resultados":"por lotes, dentro de la transaccion final"},
        "api":{"detalle":"GET /api/v1/sesiones/{id}","campos_previos_conservados":True,
               "preguntas_nuevos":["puntaje_general","version_rubrica","puede_reintentar","motivo_fallo"],"nuevo_sesion":"motivo_cierre",
               "motivos":"mensajes seguros; definitivo o finalizacion anticipada; sin nuevos campos de BD",
               "claves_s3_y_contexto_expuestos":False},
        "backend":{"h2":h2,"mysql84":mysql,"suites_mysql":suites,"pruebas_nuevas":{"dominio":8,"integracion":8}},
        "frontend":{"ruta":"/sesiones/:id/resumen","authGuard":True,"medios":{"ejecutadas":6,"fallas":0},
                    "build":"correcto, sin advertencias de presupuesto","chrome":browser["navegador"],"distribucion_chrome":chrome,
                    "casos":browser["casos"],"errores_pagina":browser["errores_javascript"],"errores_consola":browser["errores_consola"],
                    "promedio_real":81.25,"cancelada":"1/3, motivo disponible, dos Sin respuesta registrada, sin promedio final",
                    "enlaces":"MCK09, detalle, resumen y MCK03 conectados","evidencia":"pruebas-browser-incremento-8.json"},
        "esquema":{"mysql":"8.4.13","tablas":8,"columnas":68,"columnas_por_tabla":columnas,"ddl_auto":"validate","migraciones":migraciones,
                   "V1_V2_modificadas":False,"V3":False},
        "dependencias":{"pom_modificado":False,"package_json_modificado":False,"lock_modificado":False},
        "jar_sha256":hashlib.sha256(jar.read_bytes()).hexdigest(),
        "aislamiento":{"maven":"ep360_incremento8_test; planificador deshabilitado, worker determinista","chrome":"ep360_incremento8_browser; worker500ms/backoff1-1s; produccion2000ms/5-15s"},
        "limites":["S3/STT/LLM HTTP locales con clientes, ffmpeg, JAR y MySQL reales; cuentas, IAM y proveedores de produccion no certificados.",
                   "Chrome for Testing portatil en .tools, con medios emulados; no certifica hardware fisico.",
                   "Historial integral y comparacion siguen pendientes; RF-17 no se declara completo.",
                   "motivo_cierre se deriva del error definitivo persistido; sin una nueva columna de motivo historico.",
                   "Flyway11.7.2 conserva su recomendacion de actualizacion por MySQL8.4, aunque migraciones y validate pasaron."]
    }
    (ROOT/"docs/evidencias/pruebas-incremento-8.json").write_text(json.dumps(evidencia,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(dict(correcto=True,h2=h2["ejecutadas"],mysql=mysql["ejecutadas"],chrome=8,sin_staging=True)))
if __name__=="__main__":
    main()

