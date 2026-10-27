"""Consolida evidencia ejecutada del Incremento 9 sin modificar el índice Git."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, re, subprocess
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[2]
BASE = "a05ca28d539eadb8af1b5a2c1b27e050fdc6fef3"
def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True, encoding="utf-8").strip()
def log(name):
    return (ROOT / ".tools" / name).read_text(encoding="utf-8", errors="replace")
def pruebas(name):
    s=log(name)
    assert "BUILD SUCCESS" in s[-1200:], name
    n,f,e,k=map(int,re.findall(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)",s)[-1])
    assert n==194 and f==e==0
    return dict(descubiertas=n,ejecutadas=n-k,fallas=f,errores=e,omitidas=k,log_local=".tools/"+name)
def main():
    assert git("rev-parse","HEAD")==git("rev-parse","origin/main")==BASE
    assert not git("diff","--cached","--name-only")
    invariantes=["backend/pom.xml","frontend/package.json","frontend/package-lock.json","backend/src/main/resources/db/migration"]
    assert not git("diff","--name-only","--",*invariantes)
    assert not git("ls-files",".tools")
    assert git("check-ignore",".tools/inc9-browser.log")
    assert sorted(p.name for p in (ROOT/"backend/src/main/resources/db/migration").glob("*.sql"))==["V1__esquema_inicial.sql","V2__catalogo_semilla.sql"]
    h2=pruebas("inc9-h2.log");mysql=pruebas("inc9-mysql.log")
    assert h2["omitidas"]==2 and mysql["omitidas"]==0
    assert "BUILD SUCCESS" in log("inc9-package.log")[-1200:]
    build=log("inc9-angular-build.log")
    assert "Application bundle generation complete" in build and not re.search(r"WARNING|ERROR",build)
    assert "pass 6" in log("inc9-media.log") and "fail 0" in log("inc9-media.log")
    browser=json.loads((ROOT/"docs/evidencias/pruebas-browser-incremento-9.json").read_text(encoding="utf-8"))
    assert browser["correcto"] and browser["total_casos"]==9
    assert not browser["errores_javascript"] and not browser["errores_consola"]
    assert browser["llamadas_stt"]==browser["llamadas_llm"]==16
    c=browser["comparacion_canonica"]
    assert c["total_sesiones_comparadas"]==4 and c["promedio_conjunto"]==81.25 and c["mejor_puntaje"]==87.5
    assert c["cambio_vs_primera"]==-4.17 and c["cambio_vs_anterior"]==4.16
    assert [p["puntaje_promedio"] for p in c["serie"]]==[87.5,75,79.17,83.33]
    schema=log("inc9-schema.txt");assert "8.4.13" in schema
    columnas={k:int(v) for k,v in re.findall(r"^(evaluaciones_star|metricas_comunicacion|preguntas|puestos|respuestas_video|sesiones_entrevista|transcripciones|usuarios)\t(\d+)\s*$",schema,re.M)}
    assert len(columnas)==8 and sum(columnas.values())==68 and columnas["sesiones_entrevista"]==7
    suites=[]
    for p in sorted((ROOT/"backend/target/surefire-reports").glob("TEST-*.xml")):
        s=ET.parse(p).getroot()
        suites.append(dict(nombre=s.attrib["name"],pruebas=int(s.attrib["tests"]),fallas=int(s.attrib["failures"]),errores=int(s.attrib["errors"]),omitidas=int(s.attrib["skipped"])))
    assert sum(s["pruebas"] for s in suites)==194 and all(s["fallas"]==s["errores"]==s["omitidas"]==0 for s in suites)
    jar=ROOT/"backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar"
    resultado=dict(incremento=9,fecha_utc=datetime.now(timezone.utc).isoformat(),resultado="VERIFICADO_PENDIENTE_AUDITORIA",
        git=dict(rama="main",head=BASE,incremento_8="AUDITADO_APROBADO_SIN_OBSERVACIONES",
            mensaje_commit_8="feat: implementar cierre atomico de sesion COMPLETADA, promedio global y resumen de sesion",
            publicado="origin/main y ls-remote coinciden; estado limpio antes del Incremento9",
            incremento_9_sin_commit=True,indice_vacio=True,tools_ignorado=True),
        RF={"RF-17":"Verificado","RF-18":"Verificado","RF-19":"Verificado","total":"20/20 implementados y verificados en alcance local; pendiente auditoria del Incremento9"},
        reglas=dict(D10="COMPLETADA con promedio y fecha_fin persistidos, tres respuestas EVALUADO y tres evaluaciones COM-1.0 asociadas",
            fecha_filtro="fecha_inicio; America/Lima convertida a UTC; desde inclusivo/hasta dia siguiente exclusivo",
            borde="2026-10-17T04:30:00Z incluido en hasta2026-10-16;2026-10-17T05:00:00Z excluido",
            tabla="fecha_inicio DESC,id_sesion DESC",serie="fecha_fin ASC,id_sesion ASC",
            conjunto="filtros propios antes de paginar; resumen global independiente",
            redondeo="BigDecimal HALF_UP escala2",cero="cuatro indicadores null y serie[]",una="media/mejor reales; variaciones null",
            canonico=c),
        backend_h2=h2,backend_mysql=mysql,suites_mysql=suites,frontend=dict(build="AOT correcto sin advertencias",medios=6,chrome_casos=browser["total_casos"],
            errores_javascript=0,errores_consola=0,navegador=browser["navegador"],pantallas=["MCK03","MCK10","Resumen","MCK09"],
            svg="role=img, aria-label, puntos/fechas/puntajes y tabla cronologica",movil="390x844 sin desbordamiento de pagina"),
        esquema=dict(tablas_negocio=8,columnas=68,por_tabla=columnas,migraciones=["V1","V2"],ddl_auto="validate",version_rubrica_en_sesion=False),
        dependencias_modificadas=False,capas="LayerDependenciesTest correcto: domain/application Java puro",
        consultas="Respuestas y evaluaciones por lotes; cache por puesto para catalogo/preguntas",
        jar_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
        limites="S3/STT/LLM HTTP locales, clientes reales. No certifica IAM/bucket AWS ni cuentas/modelos OpenAI de produccion; no certifica hardware fisico.")
    (ROOT/"docs/evidencias/pruebas-incremento-9.json").write_text(json.dumps(resultado,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("Evidencia Incremento9 consistente: H2 192/194, MySQL194/194, medios6, Chrome9, esquema8/68, indice vacio.")
if __name__=="__main__":main()
