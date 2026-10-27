// Chrome real + API/JAR/MySQL reales. S3/STT/LLM usan fixtures HTTP locales.
// Ejecutar con .tools/preview-inc9.ps1, MySQL8.4.13 y EP360_CHROME_BIN opcional.
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';
import { execFileSync } from 'node:child_process';
const require = createRequire(import.meta.url);
const { chromium, expect } = require('../../.tools/browser-check/node_modules/playwright/test');
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const base = 'http://127.0.0.1:14200', control = 'http://127.0.0.1:19091';
const casos = [], errores = [], consola = [], peticiones = [];
const paso = (nombre, datos = {}) => { casos.push({ nombre, correcto: true, ...datos }); console.log('PASS ' + nombre); };
const espera = ms => new Promise(r => setTimeout(r, ms));
const video = path.join(root, '.tools/inc9-browser-video.webm');
if (!fs.existsSync(video)) execFileSync(path.join(root, '.tools/ffmpeg/ffmpeg.exe'),
  ['-hide_banner', '-loglevel', 'error', '-f', 'lavfi', '-i', 'testsrc2=size=320x240:rate=15', '-f', 'lavfi', '-i', 'sine=frequency=440:sample_rate=48000',
   '-t', '2', '-c:v', 'libvpx', '-c:a', 'libopus', '-y', video], { windowsHide: true });
const browser = await chromium.launch({ executablePath: process.env.EP360_CHROME_BIN || path.join(root, '.tools/chrome-for-testing/chrome-win64/chrome.exe'),
  headless: true, args: ['--autoplay-policy=no-user-gesture-required'] });
const context = await browser.newContext({ viewport: { width: 1365, height: 1000 } });
const page = await context.newPage();
page.on('pageerror', e => errores.push(e.message));
page.on('console', m => { if (m.type() === 'error') consola.push(m.text()); });
page.on('response', r => { if (r.url().includes('/api/v1/')) peticiones.push({ metodo: r.request().method(), url: new URL(r.url()).pathname + new URL(r.url()).search, estado: r.status() }); });
const inicioLlm = (await (await fetch(control + '/__llm-audit')).json()).peticiones.length;
const inicioStt = (await (await fetch(control + '/__audit')).json()).peticiones.length;
let evidencia;
try {
  const email = 'inc9-' + Date.now() + '@example.test', password = 'Historial-local-2026!';
  const registro = await context.request.post(base + '/api/v1/auth/registro', { data: { nombre_completo: 'Prueba Historial', email, password } });
  expect(registro.status()).toBe(201);
  const login = await context.request.post(base + '/api/v1/auth/login', { data: { email, password } }); expect(login.status()).toBe(200);
  const token = (await login.json()).access_token, headers = { Authorization: 'Bearer ' + token };
  const puestos = await (await context.request.get(base + '/api/v1/puestos')).json();
  const p = puestos.find(p => p.nombre_puesto === 'Practicante de Desarrollo TI') || puestos[0];
  const pOtro = puestos.find(o => o.id_puesto !== p.id_puesto);
  const pVacio = puestos.find(o => o.id_puesto !== p.id_puesto && o.id_puesto !== pOtro.id_puesto);
  const apiGet = async ruta => { const r = await context.request.get(base + '/api/v1' + ruta, { headers }); expect(r.status()).toBe(200); return r.json(); };
  const crear = async idPuesto => { const r = await context.request.post(base + '/api/v1/sesiones', { headers, data: { id_puesto: idPuesto } }); expect(r.status()).toBe(201); return r.json(); };
  const evaluar = async (sesion, pregunta, score) => {
    const puntajes = score === 87.5 ? [75, 100, 75, 100] : [75, 100, 50, 75];
    expect((await fetch(control + '/__llm-control', { method: 'PUT', body: JSON.stringify({ puntajes }) })).status).toBe(200);
    const r = await context.request.put(base + '/api/v1/sesiones/' + sesion + '/preguntas/' + pregunta + '/respuesta',
      { headers, multipart: { video: { name: 'historial.webm', mimeType: 'video/webm', buffer: fs.readFileSync(video) } } });
    expect(r.status()).toBe(202); const id = (await r.json()).id_respuesta;
    await expect.poll(async () => (await apiGet('/respuestas/' + id)).estado_procesamiento, { timeout: 45000 }).toBe('EVALUADO');
    return id;
  };
  const completas = [];
  for (const [scores, final] of [[[87.5,87.5,87.5],87.50], [[75,75,75],75.00], [[75,75,87.5],79.17], [[75,87.5,87.5],83.33]]) {
    const s = await crear(p.id_puesto); const rs = [];
    for (let i = 0; i < 3; i++) rs.push(await evaluar(s.id_sesion, s.preguntas[i].id_pregunta, scores[i]));
    const fin = await apiGet('/sesiones/' + s.id_sesion); expect(fin.estado_sesion).toBe('COMPLETADA'); expect(fin.puntaje_promedio).toBe(final);
    completas.push({ ...fin, respuestas: rs });
  }
  const unica = await crear(pOtro.id_puesto);
  for (let i=0;i<3;i++) await evaluar(unica.id_sesion, unica.preguntas[i].id_pregunta,75);
  const cancelada = await crear(p.id_puesto);
  expect((await context.request.post(base + '/api/v1/sesiones/' + cancelada.id_sesion + '/cancelacion', { headers })).status()).toBe(200);
  const pendiente = await crear(p.id_puesto);
  await evaluar(pendiente.id_sesion, pendiente.preguntas[0].id_pregunta,75);
  paso('Datos reales persistidos: cinco sesiones completas con 15 respuestas, una cancelada y una EN_PROCESO con una evaluada',
    { canonico: completas.map(s => s.puntaje_promedio), id_cancelada: cancelada.id_sesion, id_en_proceso: pendiente.id_sesion });

  await page.goto(base + '/login'); await page.locator('#login-email').fill(email); await page.locator('#login-password').fill(password);
  await page.getByRole('button', { name:'Iniciar sesión', exact:true }).click(); await expect(page).toHaveURL(/\/panel$/);
  await page.getByRole('link', { name:'Historial de sesiones', exact:true }).click(); await expect(page).toHaveURL(/\/historial$/);
  await expect(page.getByRole('heading', { name:'Historial de sesiones', exact:true })).toBeVisible();
  await expect(page.locator('.history-session')).toHaveCount(7);
  await expect(page.locator('.history-session').filter({hasText:'Cancelada'})).toContainText('Sin promedio');
  await expect(page.locator('.history-session').filter({hasText:'En proceso'})).toContainText('Sin promedio');
  await expect(page.locator('.history-session').filter({hasText:'Completada'}).first()).toContainText('/ 100');
  await expect(page.getByText(/Selecciona un puesto y la rúbrica COM-1.0/)).toBeVisible();
  await expect(page.locator('.history-session').first()).toContainText('Inicio:');
  await expect(page.locator('.history-session').first()).toContainText('Fin: En curso');
  paso('MCK03 → MCK10: lista propia completa, estados, ambas fechas y Sin promedio en sesiones incompletas');

  await page.getByLabel('Versión de rúbrica',{exact:true}).selectOption('COM-1.0');
  await page.getByLabel('Estado',{exact:true}).selectOption('CANCELADA');
  await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  await expect(page.getByText('El filtro de rúbrica incluye únicamente sesiones completas',{exact:true})).toBeVisible();
  await expect(page.getByText(/No existen coincidencias: el filtro de rúbrica/)).toBeVisible();
  expect(await page.getByLabel('Estado',{exact:true}).inputValue()).toBe('CANCELADA');
  expect(await page.getByLabel('Versión de rúbrica',{exact:true}).inputValue()).toBe('COM-1.0');
  paso('D-10: aviso exacto e intersección vacía con CANCELADA, filtros conservados');

  await page.getByLabel('Puesto',{exact:true}).selectOption(String(p.id_puesto)); await page.getByLabel('Estado',{exact:true}).selectOption('');
  await page.getByLabel('Sesiones por página',{exact:true}).selectOption('2');
  await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  await expect(page.locator('[data-testid=promedio]')).toHaveText('81.25 / 100'); await expect(page.locator('[data-testid=mejor]')).toHaveText('87.50 / 100');
  await expect(page.locator('[data-testid=primera]')).toHaveText('-4.17'); await expect(page.locator('[data-testid=anterior]')).toHaveText('+4.16');
  await expect(page.getByRole('img',{name:/Evolución de puntajes/})).toBeVisible(); await expect(page.locator('svg circle')).toHaveCount(4);
  await expect(page.locator('table tbody tr')).toHaveCount(4); await expect(page.locator('.history-session')).toHaveCount(2);
  await expect(page.getByText('Página 1 de 2',{exact:true})).toBeVisible();
  const canonico = await apiGet('/sesiones?id_puesto=' + p.id_puesto + '&version_rubrica=COM-1.0&size=2');
  expect(canonico.comparacion.serie.map(s => s.puntaje_promedio)).toEqual([87.5,75,79.17,83.33]);
  await page.screenshot({path:path.join(root,'.tools/inc9-historial-comparable.png'),fullPage:true});
  paso('PM-17 exacto: cuatro sesiones en serie cronológica y media81.25/mejor87.50/cambios-4.17,+4.16 antes de paginar');
  await page.getByRole('button',{name:'Página siguiente',exact:true}).click(); await expect(page.getByText('Página 2 de 2',{exact:true})).toBeVisible();
  await expect(page.locator('[data-testid=promedio]')).toHaveText('81.25 / 100'); await expect(page.locator('.history-session')).toHaveCount(2);
  await page.getByRole('button',{name:'Página anterior',exact:true}).click(); await expect(page.getByText('Página 1 de 2',{exact:true})).toBeVisible();
  await page.setViewportSize({width:390,height:844}); expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:path.join(root,'.tools/inc9-historial-movil.png'),fullPage:true}); await page.setViewportSize({width:1365,height:1000});
  paso('Paginación real: páginas1/2 y2/2 mantienen los indicadores del conjunto; móvil390 sin desbordamiento de página');

  await page.locator('.history-session').first().getByRole('link',{name:'Ver resumen',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Resumen de sesión',exact:true})).toBeVisible();
  await page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true}).first().click();
  await expect(page.getByRole('heading',{name:'Resultado de tu respuesta',exact:true})).toBeVisible();
  const v=page.locator('video.result-video');await expect(v).toBeVisible(); await expect.poll(()=>v.evaluate(v=>v.readyState),{timeout:15000}).toBeGreaterThanOrEqual(2);
  await v.evaluate(async v=>{v.muted=true;await v.play();});await expect.poll(()=>v.evaluate(v=>v.currentTime)).toBeGreaterThan(0);
  const url=await v.getAttribute('src');expect(url).toContain('X-Amz-Expires=300');
  await espera(1100);
  const renovacion=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/reproduccion'));
  await page.getByRole('button',{name:'Renovar enlace de video',exact:true}).click();
  const renovada=await renovacion;expect(renovada.status()).toBe(200);expect(renovada.headers()['cache-control']).toBe('no-store');
  expect((await renovada.json()).video_url_cloud).not.toBe(url);
  await expect.poll(()=>v.evaluate(v=>v.readyState),{timeout:15000}).toBeGreaterThanOrEqual(2);
  await page.getByRole('link',{name:'Historial',exact:true}).last().click();await expect(page).toHaveURL(/\/historial$/);
  paso('Historial → resumen → MCK09: video real reproduce y renueva URL SigV4 de300s/no-store; vuelve a Historial');

  await page.getByLabel('Puesto',{exact:true}).selectOption(String(pVacio.id_puesto));await page.getByLabel('Versión de rúbrica',{exact:true}).selectOption('COM-1.0');
  await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  for(const key of ['promedio','mejor','primera','anterior'])await expect(page.locator('[data-testid='+key+']')).toHaveText('Sin datos');
  await expect(page.locator('svg')).toHaveCount(0);await expect(page.getByText('Página 0 de 0',{exact:true})).toBeVisible();
  paso('Cero completas: indicadores Sin datos, sin gráfico ni ceros inventados');
  await page.getByLabel('Puesto',{exact:true}).selectOption(String(pOtro.id_puesto));await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  await expect(page.locator('[data-testid=promedio]')).toHaveText('75.00 / 100');await expect(page.locator('svg circle')).toHaveCount(1);
  await expect(page.getByText('Sin variación disponible (se requieren al menos 2 sesiones completas)',{exact:true})).toBeVisible();
  paso('Una completa: punto único, media75.00 y sin variación disponible');

  await page.getByLabel('Fecha desde',{exact:true}).fill('2026-10-17');await page.getByLabel('Fecha hasta',{exact:true}).fill('2026-10-16');
  await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  await expect(page.getByRole('alert')).toHaveText('La fecha inicial no puede ser posterior a la fecha final.');
  await page.getByRole('button',{name:'Limpiar filtros',exact:true}).click();await expect(page.locator('.history-session')).toHaveCount(7);
  expect(await page.getByLabel('Puesto',{exact:true}).inputValue()).toBe('');expect(await page.getByLabel('Versión de rúbrica',{exact:true}).inputValue()).toBe('');
  await page.getByRole('link',{name:'Panel',exact:true}).click();await expect(page).toHaveURL(/\/panel$/);
  await page.getByRole('link',{name:'Historial',exact:true}).click();await expect(page).toHaveURL(/\/historial$/);
  paso('Fechas inválidas con mensaje, limpiar filtros a página1 y enlace de cabecera funcional');
  const llm=(await(await fetch(control+'/__llm-audit')).json()).peticiones.length-inicioLlm;
  const stt=(await(await fetch(control+'/__audit')).json()).peticiones.length-inicioStt;
  expect(llm).toBe(16);expect(stt).toBe(16);expect(errores).toEqual([]);expect(consola).toEqual([]);
  evidencia={incremento:9,fecha_utc:new Date().toISOString(),correcto:true,navegador:await browser.version(),total_casos:casos.length,casos,
    errores_javascript:errores,errores_consola:consola,comparacion_canonica:canonico.comparacion,llamadas_stt:stt,llamadas_llm:llm,peticiones_api:peticiones,
    entorno:'Chrome real + Angular + JAR + MySQL8.4.13. ffmpeg y clientes S3/STT/LLM reales sobre fixtures HTTP locales; sin interceptar API de aplicación.',
    limites:'No certifica cuentas OpenAI/AWS ni IAM de producción.'};
  fs.writeFileSync(path.join(root,'docs/evidencias/pruebas-browser-incremento-9.json'),JSON.stringify(evidencia,null,2)+'\n');
  console.log('PASS TOTAL '+casos.length+'; 0 errores de página y consola');
} catch(e) {console.error(e);console.error({errores,consola});await page.screenshot({path:path.join(root,'.tools/inc9-browser-fallo.png'),fullPage:true});process.exitCode=1;}
finally {await context.close();await browser.close();}
