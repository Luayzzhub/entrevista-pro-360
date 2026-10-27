// Orquestación reproducible local. No usa git add/commit, ni elimina bases o datos.
import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import { spawn, execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const tools = path.join(root, '.tools');
const javaHome = process.env.JAVA_HOME || 'C:/Program Files/Java/jdk-17';
const env = { ...process.env, JAVA_HOME: javaHome, PATH: path.join(javaHome, 'bin') + path.delimiter + process.env.PATH,
  FFMPEG_BIN: path.join(tools, 'ffmpeg/ffmpeg.exe'), FFPROBE_BIN: path.join(tools, 'ffmpeg/ffprobe.exe'),
  TEST_DB_URL: 'jdbc:mysql://127.0.0.1:33307/ep360_incremento10_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true',
  TEST_DB_USERNAME: 'root', TEST_DB_PASSWORD: '', DB_USERNAME: 'root', DB_PASSWORD: '',
  S3_BUCKET: 'ep360-pruebas-local', S3_ENDPOINT_OVERRIDE: 'http://127.0.0.1:19090', AWS_REGION: 'us-east-1',
  AWS_ACCESS_KEY_ID: 'ep360-local-test', AWS_SECRET_ACCESS_KEY: 'ep360-local-test-secret',
  OPENAI_API_KEY: 'clave-local', OPENAI_BASE_URL: 'http://127.0.0.1:19091',
  STT_MODEL: 'gpt-4o-mini-transcribe-2025-12-15', LLM_MODEL: 'gpt-5.4-mini-2026-03-17',
  EP360_FIXTURE_DATA_DIR: path.join(tools, 'inc10-http-data'), BCRYPT_COST: '12' };
delete env.JWT_SECRET; delete env.AWS_SESSION_TOKEN; delete env.SPRING_PROFILES_ACTIVE;
const mysql = path.join(tools, 'mysql84/bin/mysql.exe');
const mysqlArgs = ['--no-defaults', '--host=127.0.0.1', '--port=33307', '--user=root', '--batch'];
const children = [], pasos = [];
const manifest = { incremento: 10, inicio_utc: new Date().toISOString(), correcto: false, pasos };
const pausa = ms => new Promise(r => setTimeout(r, ms));
const git = (...args) => execFileSync('git', args, { cwd: root, encoding: 'utf8', windowsHide: true }).trim();
const puerto = port => new Promise(resolve => { const s = net.connect(port, '127.0.0.1'); s.once('connect', () => { s.destroy(); resolve(true); }); s.once('error', () => resolve(false)); });
const sql = consulta => execFileSync(mysql, [...mysqlArgs, '--execute=' + consulta], { cwd: root, env, encoding: 'utf8', windowsHide: true });
function lanzar(nombre, exe, args, cwd = root) {
  const out = fs.openSync(path.join(tools, 'inc10-' + nombre + '.log'), 'w');
  const child = spawn(exe, args, { cwd, env, windowsHide: true, stdio: ['ignore', out, out] }); fs.closeSync(out);
  child.on('error', e => { child.fallo = e; }); children.push(child); return child;
}
async function ejecutar(nombre, exe, args, cwd = root) {
  console.log('INICIO ' + nombre); const inicio = Date.now(), child = lanzar(nombre, exe, args, cwd);
  const codigo = await new Promise((resolve, reject) => { child.once('error', reject); child.once('exit', resolve); });
  pasos.push({ nombre, codigo, duracion_ms: Date.now() - inicio, log: '.tools/inc10-' + nombre + '.log' });
  if (codigo !== 0) throw Error(nombre + ' falló: ver .tools/inc10-' + nombre + '.log');
  console.log('PASS ' + nombre);
}
const psQuote = s => "'" + s.replaceAll("'", "''") + "'";
async function maven(nombre, args) {
  const mvn = path.join(tools, 'apache-maven-3.9.9/bin/mvn.cmd');
  await ejecutar(nombre, 'powershell.exe', ['-NoProfile', '-Command', '& ' + psQuote(mvn) + ' ' + ['-Dmaven.repo.local=../.tools/m2', '-B', '-ntp', ...args].map(psQuote).join(' ') + '; exit $LASTEXITCODE'], path.join(root, 'backend'));
}
function copiarReportes(nombre) {
  const origen = path.join(root, 'backend/target/surefire-reports'), destino = path.join(tools, 'inc10-' + nombre + '-reports');
  fs.mkdirSync(destino, { recursive: true });
  for (const archivo of fs.readdirSync(origen).filter(n => /^TEST-.*\.xml$/.test(n))) fs.copyFileSync(path.join(origen, archivo), path.join(destino, archivo));
}
async function esperar(nombre, test, child) {
  for (let i = 0; i < 90; i++) {
    if (child?.fallo || (child && child.exitCode !== null)) throw Error(nombre + ' terminó antes de estar listo');
    try { if (await test()) return; } catch { /* El servicio aún arranca. */ }
    await pausa(1000);
  }
  throw Error(nombre + ' no respondió en 90 s');
}
try {
  fs.mkdirSync(tools, { recursive: true });
  manifest.head = git('rev-parse', 'HEAD'); manifest.origin_main = git('rev-parse', 'origin/main');
  if (manifest.head !== manifest.origin_main || git('diff', '--cached', '--name-only')) throw Error('Se requiere main publicado e índice vacío');
  if (git('branch', '--show-current') !== 'main') throw Error('Se requiere main');
  manifest.commit_incremento9 = git('log', '-1', '--format=%s');
  if (manifest.commit_incremento9 !== 'feat: implementar historial paginado D-10, comparacion evolutiva PM-17 y vista MCK10') throw Error('Falta cierre del Incremento 9');
  if (git('diff', 'HEAD', '--name-only', '--', 'backend/pom.xml', 'frontend/package.json', 'frontend/package-lock.json', 'backend/src/main/resources/db/migration')) throw Error('Invariantes modificadas');
  manifest.tools_ignorado = !!git('check-ignore', '.tools/inc10-ejecucion.json');
  if (git('ls-files', '.tools')) throw Error('.tools aparece en el índice');
  const migrations = fs.readdirSync(path.join(root, 'backend/src/main/resources/db/migration')).filter(n => n.endsWith('.sql')).sort();
  if (migrations.join() !== 'V1__esquema_inicial.sql,V2__catalogo_semilla.sql') throw Error('Migraciones inesperadas');
  for (const p of [18080,14200,19090,19091]) if (await puerto(p)) throw Error('Puerto ocupado por otro proceso: ' + p);
  if (!await puerto(33307)) {
    const db = lanzar('mysql-runtime', path.join(tools, 'mysql84/bin/mysqld.exe'), ['--no-defaults', '--basedir=' + path.join(tools,'mysql84'), '--datadir=' + path.join(tools,'mysql84-data'), '--port=33307', '--bind-address=127.0.0.1', '--mysqlx=0', '--skip-log-bin', '--innodb-buffer-pool-size=64M', '--console']);
    await esperar('MySQL', () => { sql('SELECT 1'); return true; }, db);
    manifest.mysql_iniciado_por_suite = true;
  }
  manifest.mysql_version = sql('SELECT VERSION();').trim().split(/\r?\n/).at(-1);
  if (manifest.mysql_version !== '8.4.13') throw Error('Se requiere MySQL 8.4.13');
  sql('CREATE DATABASE IF NOT EXISTS ep360_incremento10_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS ep360_incremento10_browser CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;');
  env.DB_URL = 'jdbc:mysql://127.0.0.1:33307/ep360_incremento10_browser?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true';
  const testDbUrl = env.TEST_DB_URL;
  delete env.TEST_DB_URL; // Las dos pruebas information_schema se habilitan por esta variable.
  await maven('h2', ['test']); copiarReportes('h2');
  env.TEST_DB_URL = testDbUrl;
  await maven('mysql', ['-Dspring.profiles.active=mysql', 'test']); copiarReportes('mysql');
  await maven('package', ['-DskipTests', 'package']);
  await ejecutar('media', process.execPath, ['--test', 'tests/media-captura.test.mjs'], path.join(root,'frontend'));
  await ejecutar('angular-build', process.execPath, ['node_modules/@angular/cli/bin/ng.js','build'], path.join(root,'frontend'));
  const fixture = lanzar('http-runtime', process.execPath, ['scripts/pruebas/pipeline-resumen-http-local.cjs']);
  await esperar('Fixtures', async () => (await fetch('http://127.0.0.1:19091/__audit')).ok, fixture);
  const backend = lanzar('backend-runtime', path.join(javaHome,'bin/java.exe'), ['-jar', path.join(root,'backend/target/entrevista-pro-360-0.1.0-SNAPSHOT.jar'), '--server.port=18080', '--integraciones.worker-intervalo-ms=1000', '--integraciones.worker-backoff-1-segundos=1', '--integraciones.worker-backoff-2-segundos=1']);
  await esperar('JAR', async () => (await fetch('http://127.0.0.1:18080/api/v1/puestos')).ok, backend);
  const frontend = lanzar('frontend-runtime', process.execPath, ['scripts/pruebas/servidor-entregable.mjs']);
  await esperar('Angular AOT', async () => (await fetch('http://127.0.0.1:14200/')).ok, frontend);
  await ejecutar('browser', process.execPath, ['scripts/pruebas/recorrido-incremento10.mjs']);
  const consulta = "USE ep360_incremento10_browser; SELECT VERSION(); SELECT TABLE_NAME,COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME<>'flyway_schema_history' GROUP BY TABLE_NAME ORDER BY TABLE_NAME; SELECT version,success FROM flyway_schema_history ORDER BY installed_rank; SELECT s.id_sesion,s.estado_sesion,s.puntaje_promedio,s.fecha_fin,COUNT(DISTINCT r.id_respuesta) respuestas,COUNT(DISTINCT e.id_evaluacion) evaluaciones FROM sesiones_entrevista s LEFT JOIN respuestas_video r ON r.id_sesion=s.id_sesion LEFT JOIN evaluaciones_star e ON e.id_respuesta=r.id_respuesta GROUP BY s.id_sesion ORDER BY s.id_sesion;";
  fs.writeFileSync(path.join(tools,'inc10-schema.txt'), sql(consulta));
  manifest.correcto = true;
} catch (e) { manifest.error = e.message; console.error(e.message); process.exitCode = 1; }
finally {
  for (const child of [...children].reverse()) {
    if (child.exitCode === null && !child.fallo) {
      // MySQL se apaga por su protocolo; los demás son únicamente procesos propios.
      if (child.spawnfile.endsWith('mysqld.exe')) {
        try { execFileSync(path.join(tools,'mysql84/bin/mysqladmin.exe'), [...mysqlArgs.filter(a => a !== '--batch'),'shutdown'], { env, windowsHide:true }); } catch { child.kill(); }
      } else child.kill();
      await Promise.race([new Promise(r => { if(child.exitCode!==null)r();else child.once('exit',r); }), pausa(5000)]);
    }
  }
  manifest.fin_utc = new Date().toISOString();
  fs.writeFileSync(path.join(tools,'inc10-ejecucion.json'), JSON.stringify(manifest,null,2)+'\n');
}
if (manifest.correcto) await ejecutar('evidencias', process.env.EP360_PYTHON_BIN || 'python', ['scripts/pruebas/registrar-evidencias-incremento10.py']);
