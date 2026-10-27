// Chrome + Angular AOT + JAR + MySQL, sin interceptar ni sustituir la API.
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
const require = createRequire(import.meta.url);
const { chromium, expect } = require('../../.tools/browser-check/node_modules/playwright/test');
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const base = 'http://127.0.0.1:14200', control = 'http://127.0.0.1:19091';
const casos = [], errores = [], consola = [], peticiones = [], etapas = {}, interfaces = new Set(), tareas = [];
const pausa = ms => new Promise(r => setTimeout(r, ms));
const paso = (nombre, rf, datos = {}) => { casos.push({ nombre, RF: rf, correcto: true, ...datos }); console.log('PASS ' + nombre); };
const ctrl = async (ruta, data) => { const r = await fetch(control + ruta, { method:'PUT', body:JSON.stringify(data) }); expect(r.status).toBe(200); };
const video = path.join(root,'.tools/inc10-fixture.webm');
execFileSync(process.env.FFMPEG_BIN || path.join(root,'.tools/ffmpeg/ffmpeg.exe'), ['-hide_banner','-loglevel','error','-f','lavfi','-i','testsrc2=size=320x240:rate=15','-f','lavfi','-i','sine=frequency=440:sample_rate=48000','-t','2','-c:v','libvpx','-c:a','libopus','-y',video], {windowsHide:true});
const browser = await chromium.launch({executablePath:process.env.EP360_CHROME_BIN || path.join(root,'.tools/chrome-for-testing/chrome-win64/chrome.exe'),headless:true,
  args:['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
const context = await browser.newContext({viewport:{width:1365,height:1000}, permissions:['camera','microphone']});
const page = await context.newPage();
page.on('pageerror',e=>errores.push(e.message));
page.on('console',m=>{if(m.type()==='error')consola.push(m.text());});
page.on('response',r=>{if(r.url().includes('/api/v1/'))peticiones.push({origen:'pagina',metodo:r.request().method(),ruta:new URL(r.url()).pathname,estado:r.status()});});
let token, sesion, comparacion, cancelada;
const headers = () => ({Authorization:'Bearer '+token});
async function api(metodo,ruta,options={},status=200) {
  const r=await context.request.fetch(base+'/api/v1'+ruta,{method:metodo,headers:headers(),...options});
  peticiones.push({origen:'cliente_http',metodo,ruta:'/api/v1'+ruta.split('?')[0],estado:r.status()});
  expect(r.status(),metodo+' '+ruta).toBe(status); return r.json();
}
async function observar(id,estado='EVALUADO') {
  etapas[id] ||= [];
  let data;
  await expect.poll(async()=>{
    data=await api('GET','/respuestas/'+id);
    if(!etapas[id].includes(data.etapa_actual))etapas[id].push(data.etapa_actual);
    return data.estado_procesamiento;
  },{timeout:60000,intervals:[80,100,150]}).toBe(estado);
  return data;
}
async function grabar(repetir=false,fallo=false) {
  interfaces.add('MCK05');
  await expect(page.locator('.media-privacy')).toContainText('únicamente el audio extraído');
  await expect(page.getByRole('button',{name:'Iniciar grabación',exact:true})).toBeDisabled();
  await page.getByRole('button',{name:'Comprobar cámara y micrófono',exact:true}).click();
  await expect(page.getByRole('button',{name:'Iniciar grabación',exact:true})).toBeEnabled();
  const pistas=await page.locator('video[aria-label="Vista previa de cámara"]').evaluate(v=>v.srcObject.getTracks().map(t=>({tipo:t.kind,estado:t.readyState,activa:t.enabled})));
  expect(pistas).toHaveLength(2);expect(pistas.every(t=>t.estado==='live'&&t.activa)).toBe(true);
  async function toma(){
    interfaces.add('MCK06');await page.getByRole('button',{name:'Iniciar grabación',exact:true}).click();
    await expect(page.getByRole('timer')).toHaveText('00:02 / 01:00',{timeout:6000});
    await page.getByRole('button',{name:'Detener grabación',exact:true}).click();
    interfaces.add('MCK07');await expect(page.locator('video.review-video')).toBeVisible();
    await page.locator('video.review-video').evaluate(async v=>{v.muted=true;await v.play();});
    await expect.poll(()=>page.locator('video.review-video').evaluate(v=>v.currentTime)).toBeGreaterThan(0);
  }
  await toma();
  if(repetir){
    const antes=await api('GET','/sesiones/'+sesion);
    await page.getByRole('button',{name:'Volver a grabar',exact:true}).click();
    const despues=await api('GET','/sesiones/'+sesion);expect(despues.respuestas_confirmadas).toBe(antes.respuestas_confirmadas);
    // Repetir libera ambas pistas; la nueva toma vuelve a comprobar dispositivos.
    await page.getByRole('button',{name:'Comprobar cámara y micrófono',exact:true}).click();
    await expect(page.getByRole('button',{name:'Iniciar grabación',exact:true})).toBeEnabled();
    await toma();
  }
  const subida=page.waitForResponse(r=>r.request().method()==='PUT'&&/\/preguntas\/\d+\/respuesta$/.test(r.url()));
  await page.getByRole('button',{name:'Confirmar respuesta',exact:true}).click();
  const r=await subida;expect(r.status()).toBe(202);const data=await r.json();
  interfaces.add('MCK08');await expect(page.getByRole('heading',{name:'Procesamiento de respuesta',exact:true})).toBeVisible();
  await expect(page.locator('.processing-stages li')).toHaveCount(5);
  etapas[data.id_respuesta]=['EXTRAER_AUDIO'];
  const observado=observar(data.id_respuesta,fallo?'FALLIDO':'EVALUADO'); tareas.push(observado);
  const resultado=await observado;
  if(!fallo){
    for(const etapa of ['EXTRAER_AUDIO','TRANSCRIBIR','METRICAS','EVALUAR','FINALIZADO'])expect(etapas[data.id_respuesta]).toContain(etapa);
  }
  return resultado;
}
async function resultado(orden,score){
  await page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true}).click();interfaces.add('MCK09');
  await expect(page.getByRole('heading',{name:'Resultado de tu respuesta',exact:true})).toBeVisible();
  await expect(page.locator('.session-score')).toHaveText(score.toFixed(2)+' / 100');
  await expect(page.locator('.result-criterion').filter({has:page.locator('h3')})).toHaveCount(4);
  for(const c of ['relevancia','claridad','estructura','evidencias'])await expect(page.getByText('Justificación de '+c+' según la transcripción.',{exact:true})).toBeVisible();
  const star=page.getByRole('region',{name:'Diagnóstico cualitativo STAR'});
  if(orden===2){for(const n of ['Situación','Tarea','Acción','Resultado'])await expect(star).toContainText(n);}
  else await expect(star).toContainText('STAR no aplica a esta pregunta');
  await expect(page.getByRole('region',{name:'Métricas descriptivas MET-1.0'})).toContainText('o sea: 1');
  await expect(page.getByRole('region',{name:'Transcripción de tu respuesta'})).toContainText('Eh este código-fuente o sea resuelve problemas');
  const v=page.locator('video.result-video');await expect.poll(()=>v.evaluate(v=>v.readyState),{timeout:15000}).toBeGreaterThanOrEqual(2);
  await v.evaluate(async v=>{v.muted=true;await v.play();});await expect.poll(()=>v.evaluate(v=>v.currentTime)).toBeGreaterThan(0);
  const url=await v.getAttribute('src');expect(url).toContain('X-Amz-Expires=300');expect(url).toContain('X-Amz-Signature=');
  await pausa(1100);const renovacion=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/reproduccion'));
  await page.getByRole('button',{name:'Renovar enlace de video',exact:true}).click();const renovada=await renovacion;
  expect(renovada.status()).toBe(200);expect(renovada.headers()['cache-control']).toBe('no-store');
  expect((await renovada.json()).video_url_cloud).not.toBe(url);
}
const scores = n => n===87.5?[75,100,75,100]:[75,100,50,75];
async function subir(s,p,n){
  await ctrl('/__llm-control',{puntajes:scores(n)});
  const r=await api('PUT','/sesiones/'+s+'/preguntas/'+p+'/respuesta',{multipart:{video:{name:'fixture.webm',mimeType:'video/webm',buffer:fs.readFileSync(video)}}},202);
  await observar(r.id_respuesta);return r.id_respuesta;
}
try {
  await ctrl('/__control',{});await ctrl('/__llm-control',{puntajes:scores(87.5)});
  const email='inc10-'+Date.now()+'@example.test',password='Entrega-local-2026!';
  await page.goto(base+'/registro');interfaces.add('MCK02');
  await page.locator('#registro-nombre').fill('Demostración Integral');await page.locator('#registro-email').fill(email);
  await page.locator('#registro-password').fill(password);await page.locator('#registro-confirmacion').fill(password);
  const registro=page.waitForResponse(r=>r.url().endsWith('/auth/registro'));
  await page.getByRole('button',{name:'Crear cuenta',exact:true}).click();expect((await registro).status()).toBe(201);
  await expect(page).toHaveURL(/\/login$/);interfaces.add('MCK01');
  const login=page.waitForResponse(r=>r.url().endsWith('/auth/login'));
  await page.locator('#login-email').fill(email);await page.locator('#login-password').fill(password);
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();const auth=await(await login).json();token=auth.access_token;
  expect(auth.expires_in).toBe(1800);await expect(page).toHaveURL(/\/panel$/);interfaces.add('MCK03');
  expect(await page.evaluate(()=>({local:localStorage.length,session:sessionStorage.length}))).toEqual({local:0,session:0});
  paso('Registro MCK02 e inicio MCK01 reales; JWT de 1800s fuera de almacenamiento persistente',['RF-01','RF-02']);

  await page.getByRole('link',{name:'Perfil',exact:true}).click();interfaces.add('Perfil');
  await expect(page.locator('#perfil-email')).toHaveAttribute('readonly','');await expect(page.locator('#perfil-email')).toHaveValue(email);
  await page.locator('#perfil-nombre').fill('Demostración Integración 10');await page.locator('#perfil-resumen').fill('Estudiante de Sistemas: práctica reproducible.');
  await page.getByRole('button',{name:'Guardar cambios',exact:true}).click();await expect(page.getByText('Tu perfil se guardó correctamente.',{exact:true})).toBeVisible();
  await page.getByRole('link',{name:'Panel',exact:true}).click();await page.getByRole('link',{name:'Perfil',exact:true}).click();
  await expect(page.locator('#perfil-nombre')).toHaveValue('Demostración Integración 10');await expect(page.locator('#perfil-resumen')).toHaveValue('Estudiante de Sistemas: práctica reproducible.');
  expect((await api('GET','/perfil')).email).toBe(email);paso('Perfil: nombre y resumen persistidos; correo solo lectura',['RF-03']);

  await page.getByRole('link',{name:'Puestos',exact:true}).click();interfaces.add('MCK04');await expect(page.locator('#catalogo .card')).toHaveCount(3);
  const puestos=await api('GET','/puestos');expect(puestos).toHaveLength(3);
  for(const p of puestos){
    const banco=await api('GET','/puestos/'+p.id_puesto+'/preguntas');expect(banco.map(p=>p.orden)).toEqual([1,2,3]);expect(banco.map(p=>p.aplica_star)).toEqual([false,true,false]);
    await page.getByRole('link',{name:'Ver preguntas de '+p.nombre_puesto,exact:true}).click();await expect(page.locator('.question-list .question-card')).toHaveCount(3);
    await page.getByRole('link',{name:'Puestos',exact:true}).click();
  }
  await page.locator('app-iniciar-practica button').first().click();await expect(page).toHaveURL(/\/sesiones\/\d+$/);sesion=Number(page.url().split('/').pop());
  const conflicto=await api('POST','/sesiones',{data:{id_puesto:puestos[0].id_puesto}},409);expect(conflicto.codigo).toBe('SESION_ACTIVA_EXISTENTE');
  await page.getByRole('link',{name:'Panel',exact:true}).click();await expect(page.getByRole('link',{name:'Continuar práctica',exact:true})).toBeVisible();
  await page.getByRole('link',{name:'Continuar práctica',exact:true}).click();
  paso('MCK04: tres puestos y nueve preguntas; MCK03 conserva una única práctica activa',['RF-04','RF-05','RF-06']);

  const ids=[];
  for(let orden=1;orden<=3;orden++){
    await expect(page.locator('.capture-panel .star-note')).toHaveCount(orden===2?1:0);
    const r=await grabar(orden===1);ids.push(r.id_respuesta);
    expect(r.metricas.version_metricas).toBe('MET-1.0');expect(r.evaluacion.version_rubrica).toBe('COM-1.0');
    expect(r.evaluacion.aplica_star).toBe(orden===2);expect(r.tamano_bytes).toBeGreaterThan(0);expect(r.duracion_segundos).toBeGreaterThanOrEqual(1);
    await expect(page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true})).toBeVisible({timeout:10000});await resultado(orden,87.5);
    paso('P'+orden+': cámara/micrófono → captura → revisión → S3/ffprobe → cuatro etapas → COM/STAR/MET/transcripción/video SigV4',
      ['RF-07','RF-08','RF-09','RF-10','RF-11','RF-12','RF-13','RF-14','RF-15'],{id_respuesta:r.id_respuesta,etapas:etapas[r.id_respuesta],aplica_star:r.evaluacion.aplica_star});
    if(orden<3)await page.getByRole('link',{name:'Siguiente pregunta',exact:true}).click();
  }
  const completa=await api('GET','/sesiones/'+sesion);expect(completa.estado_sesion).toBe('COMPLETADA');expect(completa.puntaje_promedio).toBe(87.5);
  expect(completa.respuestas_evaluadas).toBe(3);expect(completa.pregunta_habilitada).toBeNull();expect(completa.fecha_fin).toMatch(/Z$/);
  await page.getByRole('link',{name:'Ver resumen',exact:true}).click();interfaces.add('Resumen de sesión');
  await expect(page.locator('app-resumen-sesion header .session-score')).toHaveText('87.50 / 100');
  await expect(page.locator('app-resumen-sesion .question-score')).toHaveCount(3);
  await page.screenshot({path:path.join(root,'.tools/inc10-resumen.png'),fullPage:true});
  paso('P3 cierra COMPLETADA atómicamente; resumen con tres resultados y promedio persistido',['RF-16'],{id_sesion:sesion,promedio:completa.puntaje_promedio,respuestas:ids});

  for(const [valores,media] of [[[75,75,75],75],[[75,75,87.5],79.17],[[75,87.5,87.5],83.33]]){
    const s=await api('POST','/sesiones',{data:{id_puesto:puestos[0].id_puesto}},201);
    for(let i=0;i<3;i++)await subir(s.id_sesion,s.preguntas[i].id_pregunta,valores[i]);
    expect((await api('GET','/sesiones/'+s.id_sesion)).puntaje_promedio).toBe(media);
  }
  paso('Tres sesiones adicionales verifican HALF_UP escala2: 75.00,79.17,83.33',['RF-06','RF-16','RF-19']);

  await page.getByRole('link',{name:'Nueva práctica',exact:true}).click();await page.locator('app-iniciar-practica button').first().click();
  await expect(page).toHaveURL(/\/sesiones\/\d+$/);sesion=Number(page.url().split('/').pop());
  await ctrl('/__llm-control',{puntajes:scores(75)});const previa=await grabar();await resultado(1,75);
  await page.getByRole('link',{name:'Siguiente pregunta',exact:true}).click();await ctrl('/__control',{fallos:3});
  const fallo=await grabar(false,true);expect(fallo.puede_reintentar).toBe(true);
  await expect(page.getByRole('button',{name:'Reintentar análisis',exact:true})).toBeVisible({timeout:10000});
  await ctrl('/__control',{});await page.getByRole('button',{name:'Reintentar análisis',exact:true}).click();
  await observar(fallo.id_respuesta);await expect(page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true})).toBeVisible({timeout:10000});
  await resultado(2,75);await api('POST','/respuestas/'+fallo.id_respuesta+'/reintento',{},409);
  await page.getByRole('button',{name:'Finalizar anticipadamente',exact:true}).click();
  const cancelarHttp=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/sesiones/'+sesion+'/cancelacion'));
  await page.getByRole('button',{name:'Sí, finalizar práctica',exact:true}).click();expect((await cancelarHttp).status()).toBe(200);
  cancelada=await api('GET','/sesiones/'+sesion);expect(cancelada.estado_sesion).toBe('CANCELADA');expect(cancelada.puntaje_promedio).toBeNull();expect(cancelada.respuestas_evaluadas).toBe(2);
  await api('POST','/sesiones/'+sesion+'/cancelacion');
  await page.getByRole('link',{name:'Ver resumen',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Resumen de sesión',exact:true})).toBeVisible();
  await expect(page.locator('app-resumen-sesion .question-score')).toHaveCount(2);await expect(page.locator('app-resumen-sesion header .session-score')).toHaveCount(0);
  paso('RF20: tres fallos STT, único reintento manual, recuperación y cancelación conservando P1/P2 sin promedio',['RF-20'],{id_sesion:sesion,id_recuperada:fallo.id_respuesta,id_previa:previa.id_respuesta,promedio:null});

  await page.getByRole('link',{name:'Historial',exact:true}).first().click();interfaces.add('MCK10');
  await expect(page.locator('.history-session')).toHaveCount(5);await expect(page.locator('.history-session').filter({hasText:'Cancelada'})).toContainText('Sin promedio');
  await page.getByLabel('Puesto',{exact:true}).selectOption(String(puestos[0].id_puesto));await page.getByLabel('Versión de rúbrica',{exact:true}).selectOption('COM-1.0');
  const fechaLima=new Intl.DateTimeFormat('sv-SE',{timeZone:'America/Lima'}).format(new Date());
  await page.getByLabel('Fecha desde',{exact:true}).fill(fechaLima);await page.getByLabel('Fecha hasta',{exact:true}).fill(fechaLima);
  await page.getByLabel('Estado',{exact:true}).selectOption('COMPLETADA');await page.getByLabel('Sesiones por página',{exact:true}).selectOption('2');
  await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  for(const [key,value] of [['promedio','81.25 / 100'],['mejor','87.50 / 100'],['primera','-4.17'],['anterior','+4.16']])await expect(page.locator('[data-testid='+key+']')).toHaveText(value);
  await expect(page.locator('svg circle')).toHaveCount(4);await expect(page.locator('table tbody tr')).toHaveCount(4);await expect(page.locator('.history-session')).toHaveCount(2);
  comparacion=(await api('GET','/sesiones?id_puesto='+puestos[0].id_puesto+'&version_rubrica=COM-1.0&size=2&fecha_desde='+fechaLima+'&fecha_hasta='+fechaLima)).comparacion;
  expect(comparacion.serie.map(s=>s.puntaje_promedio)).toEqual([87.5,75,79.17,83.33]);
  await page.getByRole('button',{name:'Página siguiente',exact:true}).click();await expect(page.getByText('Página 2 de 2',{exact:true})).toBeVisible();
  await expect(page.locator('[data-testid=promedio]')).toHaveText('81.25 / 100');
  await page.screenshot({path:path.join(root,'.tools/inc10-historial.png'),fullPage:true});await page.setViewportSize({width:390,height:844});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await page.screenshot({path:path.join(root,'.tools/inc10-historial-movil.png'),fullPage:true});await page.setViewportSize({width:1365,height:1000});
  await page.getByLabel('Estado',{exact:true}).selectOption('CANCELADA');await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  await expect(page.getByText('El filtro de rúbrica incluye únicamente sesiones completas',{exact:true})).toBeVisible();await expect(page.locator('.history-session')).toHaveCount(0);
  await page.getByLabel('Estado',{exact:true}).selectOption('COMPLETADA');await page.getByRole('button',{name:'Aplicar filtros',exact:true}).click();
  await page.locator('.history-session').first().getByRole('link',{name:'Ver resumen',exact:true}).click();
  await page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true}).first().click();await expect(page.locator('video.result-video')).toBeVisible();
  paso('MCK10: cinco filtros Lima→UTC, D10, paginación y PM17 exacto; vuelve a resumen y resultado propios',['RF-17','RF-18','RF-19'],{fecha_lima:fechaLima,comparacion});

  const otro=await browser.newContext();
  const r=await otro.request.get(base+'/api/v1/sesiones');expect(r.status()).toBe(401);
  const emailOtro='inc10-otro-'+Date.now()+'@example.test';await otro.request.post(base+'/api/v1/auth/registro',{data:{nombre_completo:'Otra Cuenta',email:emailOtro,password}});
  const l=await otro.request.post(base+'/api/v1/auth/login',{data:{email:emailOtro,password}});const tokenOtro=(await l.json()).access_token;
  for(const ruta of ['/sesiones/'+completa.id_sesion,'/respuestas/'+ids[0]])expect((await otro.request.get(base+'/api/v1'+ruta,{headers:{Authorization:'Bearer '+tokenOtro}})).status()).toBe(404);
  expect((await(await otro.request.get(base+'/api/v1/sesiones',{headers:{Authorization:'Bearer '+tokenOtro}})).json()).items).toEqual([]);await otro.close();
  paso('Propiedad JWT: otra cuenta no consulta sesiones/respuestas ni historial ajeno',['RF-17']);

  await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await expect(page).toHaveURL(/\/login$/);
  // Atrás conserva la instancia Angular y ejercita la guarda tras limpiar el estado en memoria.
  await page.goBack();await expect(page).toHaveURL(/\/login(?:\?|$)/);
  await page.goto(base+'/historial');await expect(page).toHaveURL(/\/login(?:\?|$)/);
  expect(await page.evaluate(()=>({local:localStorage.length,session:sessionStorage.length}))).toEqual({local:0,session:0});
  await expect(page.getByRole('button',{name:'Cerrar sesión',exact:true})).toHaveCount(0);
  paso('Cerrar sesión limpia estado en memoria y bloquea retorno a rutas protegidas',['RF-02']);

  const normalizar=r=>r.replace(/\/\d+(?=\/|$)/g,'/{id}');
  const endpoints=[...new Set(peticiones.filter(p=>p.estado<300).map(p=>p.metodo+' '+normalizar(p.ruta)))].sort();
  const contractuales=['POST /auth/registro','POST /auth/login','GET /perfil','PATCH /perfil','GET /puestos','GET /puestos/{id}/preguntas',
    'POST /sesiones','GET /sesiones','GET /sesiones/{id}','POST /sesiones/{id}/cancelacion','PUT /sesiones/{id}/preguntas/{id}/respuesta',
    'GET /respuestas/{id}','POST /respuestas/{id}/reintento','POST /respuestas/{id}/reproduccion'].map(e=>e.replace(' /',' /api/v1/')).sort();
  expect(endpoints).toEqual(contractuales);expect(interfaces.size).toBe(12);expect(errores).toEqual([]);expect(consola).toEqual([]);
  const stt=await(await fetch(control+'/__audit')).json(),llm=await(await fetch(control+'/__llm-audit')).json(),s3=await(await fetch('http://127.0.0.1:19090/__audit')).json();
  expect(stt.peticiones.length).toBe(17);expect(llm.peticiones.length).toBe(14);
  expect(stt.peticiones.every(p=>p.modelo&&p.wav&&p.sin_identificadores)).toBe(true);
  expect(llm.peticiones.every(p=>p.modelo==='gpt-5.4-mini-2026-03-17'&&p.store===false&&p.formato.strict&&p.sin_herramientas)).toBe(true);
  expect(s3.peticiones.every(p=>p.firmada)).toBe(true);
  const audio= s3.objetos.find(o=>o.tipo==='audio/wav');expect(audio).toBeTruthy();
  const audioInfo=JSON.parse(execFileSync(process.env.FFPROBE_BIN||path.join(root,'.tools/ffmpeg/ffprobe.exe'),['-v','error','-show_streams','-of','json',audio.archivo],{encoding:'utf8',windowsHide:true}));
  expect(audioInfo.streams[0].codec_name).toBe('pcm_s16le');expect(audioInfo.streams[0].sample_rate).toBe('16000');expect(audioInfo.streams[0].channels).toBe(1);
  const evidencia={incremento:10,fecha_utc:new Date().toISOString(),correcto:true,navegador:await browser.version(),total_casos:casos.length,casos,
    interfaces:[...interfaces],endpoints,RF:[...new Set(casos.flatMap(c=>c.RF))].sort(),errores_javascript:errores,errores_consola:consola,
    etapas_por_respuesta:etapas,comparacion_canonica:comparacion,sesion_completa:completa,sesion_cancelada:cancelada,
    llamadas_stt:stt.peticiones.length,llamadas_llm:llm.peticiones.length,auditoria_stt:stt,auditoria_llm:llm,auditoria_s3:s3.peticiones,
    wav:{codec:'pcm_s16le',sample_rate:16000,canales:1},peticiones_api:peticiones,
    entorno:'Angular AOT producción servido desde dist + JAR compilado + MySQL8.4.13 + ffmpeg/ffprobe y Chrome reales. S3 SDK y OpenAI HttpClient sobre fixtures HTTP locales. Dispositivos de cámara/micrófono emulados por Chrome.',
    limites:'No certifica IAM/bucket AWS, cuenta/modelos OpenAI de producción ni matriz de hardware físico.'};
  expect(evidencia.RF).toHaveLength(20);fs.writeFileSync(path.join(root,'docs/evidencias/pruebas-browser-incremento-10.json'),JSON.stringify(evidencia,null,2)+'\n');
  console.log('PASS TOTAL '+casos.length+'; 20 RF,14 endpoints,12 interfaces; 0 errores JS/consola');
}catch(e){console.error(e);console.error({errores,consola});await page.screenshot({path:path.join(root,'.tools/inc10-browser-fallo.png'),fullPage:true});process.exitCode=1;}
finally {await Promise.allSettled(tareas);await context.close();await browser.close();}
