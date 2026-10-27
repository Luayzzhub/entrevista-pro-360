const {chromium,expect}=require('../../.tools/browser-check/node_modules/playwright/test');
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),base='http://127.0.0.1:14200',control='http://127.0.0.1:19091';
const casos=[],errores=[],api=[];
const paso=(nombre,datos={})=>{casos.push({nombre,correcto:true,...datos});console.log('PASS '+nombre);};
const pausa=ms=>new Promise(r=>setTimeout(r,ms));
async function configurarLlm(data){expect((await fetch(control+'/__llm-control',{method:'PUT',body:JSON.stringify(data)})).status).toBe(200);}
async function nuevaCuenta(browser,tipo){
 const context=await browser.newContext({viewport:{width:1365,height:1000}}),page=await context.newPage();
 page.on('pageerror',e=>errores.push(e.message));
 page.on('response',r=>{if(r.url().includes('/api/v1/'))api.push({metodo:r.request().method(),ruta:new URL(r.url()).pathname,estado:r.status(),ms:Date.now()});});
 const email='inc7-'+tipo+'-'+Date.now()+'@example.test',password='Evaluacion-local-2026!';
 expect((await context.request.post(base+'/api/v1/auth/registro',{data:{nombre_completo:'Prueba Siete',email,password}})).status()).toBe(201);
 await page.goto(base+'/login');await page.locator('#login-email').fill(email);await page.locator('#login-password').fill(password);
 await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page).toHaveURL(/\/panel$/);
 const login=await context.request.post(base+'/api/v1/auth/login',{data:{email,password}});expect(login.status()).toBe(200);
 const token=(await login.json()).access_token;
 await page.getByRole('link',{name:/^Nueva práctica/}).click();await expect(page.locator('app-iniciar-practica button').first()).toBeEnabled();
 await page.locator('app-iniciar-practica button').first().click();await expect(page).toHaveURL(/\/sesiones\/\d+$/);
 return {context,page,token,sesion:Number(page.url().split('/').pop())};
}
async function grabar(page){
 await page.getByRole('button',{name:'Comprobar cámara y micrófono',exact:true}).click();
 await expect(page.getByRole('button',{name:'Iniciar grabación',exact:true})).toBeEnabled();
 await page.getByRole('button',{name:'Iniciar grabación',exact:true}).click();
 await expect(page.getByRole('timer')).toHaveText('00:02 / 01:00',{timeout:5000});
 await page.getByRole('button',{name:'Detener grabación',exact:true}).click();await expect(page.locator('video.review-video')).toBeVisible();
 const subida=page.waitForResponse(r=>r.request().method()==='PUT'&&/\/preguntas\/\d+\/respuesta$/.test(r.url()));
 await page.getByRole('button',{name:'Confirmar respuesta',exact:true}).click();const r=await subida;expect(r.status()).toBe(202);
 const data=await r.json();await expect(page.getByRole('heading',{name:'Procesamiento de respuesta',exact:true})).toBeVisible();return data;
}
async function abrirResultado(page){
 const link=page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true});
 await expect(link).toBeVisible({timeout:45000});await link.click();
 await expect(page.getByRole('heading',{name:'Resultado de tu respuesta',exact:true})).toBeVisible();
 await expect(page.locator('.session-score')).toHaveText('75.00 / 100');
 for(const c of ['Relevancia','Claridad','Estructura','Evidencias']){
  await expect(page.getByRole('heading',{name:c,exact:true})).toBeVisible();
  await expect(page.getByText('Justificación de '+c.toLowerCase()+' según la transcripción.',{exact:true})).toBeVisible();
 }
 await expect(page.getByRole('region',{name:'Transcripción de tu respuesta'})).toContainText('Eh este código-fuente o sea resuelve problemas');
 await expect(page.getByRole('region',{name:'Métricas descriptivas MET-1.0'})).toContainText('o sea: 1');
 expect(await page.locator('app-resultado-respuesta .safe-text b').count()).toBe(0);
 await expect(page.getByRole('button',{name:'Volver a grabar',exact:true})).toHaveCount(0);
}
(async()=>{
 const browser=await chromium.launch({executablePath:'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:true,
 args:['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
 try{
  await configurarLlm({});
  const a=await nuevaCuenta(browser,'secuencia'),{page,context,token,sesion}=a;
  const p1=await grabar(page);await abrirResultado(page);
  await expect(page.getByText('STAR no aplica a esta pregunta',{exact:true})).toBeVisible();
  await expect(page.locator('video.result-video')).toBeVisible();
  await expect.poll(()=>page.locator('video.result-video').evaluate(v=>v.readyState),{timeout:15000}).toBeGreaterThanOrEqual(2);
  await page.locator('video.result-video').evaluate(async v=>{v.muted=true;await v.play();});
  await expect.poll(()=>page.locator('video.result-video').evaluate(v=>v.currentTime),{timeout:5000}).toBeGreaterThan(0);
  paso('MCK08 → MCK09: P1, cuatro criterios, feedback plano, MET, transcripción y reproducción real',{id_respuesta:p1.id_respuesta});
  const primeraUrl=await page.locator('video.result-video').getAttribute('src');expect(primeraUrl).toContain('X-Amz-Expires=300');
  expect((await fetch(primeraUrl.replace('X-Amz-Expires=300','X-Amz-Expires=301'))).status).toBe(403);
  await pausa(1100);
  const renovar=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/respuestas/'+p1.id_respuesta+'/reproduccion'));
  await page.getByRole('button',{name:'Renovar enlace de video',exact:true}).click();const renovada=await renovar;
  expect(renovada.status()).toBe(200);expect(renovada.headers()['cache-control']).toBe('no-store');
  const enlace=await renovada.json();expect(enlace.video_url_cloud).not.toBe(primeraUrl);
  expect(new URL(enlace.video_url_cloud).searchParams.get('X-Amz-Expires')).toBe('300');
  paso('URL firmada real: 300 s, no-store, renovación manual y firma alterada rechazada');
  const expirado=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/respuestas/'+p1.id_respuesta+'/reproduccion'));
  await page.evaluate(()=>{const original=Date.now;Date.now=()=>original()+301000;document.querySelector('video.result-video').dispatchEvent(new Event('error'));Date.now=original;});
  expect((await expirado).status()).toBe(200);
  paso('Error de reproductor después de expira_en renueva el enlace automáticamente');
  const key=new URL(enlace.video_url_cloud).pathname;
  expect((await fetch('http://127.0.0.1:19090/__control',{method:'PUT',body:JSON.stringify({eliminar_clave:key})})).status).toBe(200);
  await expect(page.getByRole('button',{name:'Renovar enlace de video',exact:true})).toBeEnabled();
  await page.getByRole('button',{name:'Renovar enlace de video',exact:true}).click();
  await expect(page.getByText('Video no disponible',{exact:true})).toBeVisible();
  await expect(page.locator('video.result-video')).toHaveCount(0);
  await expect(page.locator('.session-score')).toHaveText('75.00 / 100');
  await expect(page.getByRole('heading',{name:'Transcripción',exact:true})).toBeVisible();
  await page.screenshot({path:path.join(root,'.tools/inc7-video-no-disponible.png'),fullPage:true});
  paso('VIDEO_NO_DISPONIBLE oculta video y conserva evaluación, transcripción y métricas');
  await page.getByRole('link',{name:'Siguiente pregunta',exact:true}).click();
  await expect(page).toHaveURL(new RegExp('/sesiones/'+sesion+'$'));
  await expect(page.locator('.available-question')).toContainText('02');
  await expect(page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true})).toBeVisible();
  paso('P1 EVALUADO habilita Pregunta 2 y el detalle ofrece enlace a MCK09');
  const p2=await grabar(page);await abrirResultado(page);
  await expect(page.getByText('STAR no aplica a esta pregunta',{exact:true})).toHaveCount(0);
  const star=page.getByRole('region',{name:'Diagnóstico cualitativo STAR'});
  for(const label of ['Situación','Tarea','Acción','Resultado'])await expect(star).toContainText(label);
  for(const estado of ['PRESENTE','PARCIAL','AUSENTE'])await expect(star).toContainText(estado);
  await page.screenshot({path:path.join(root,'.tools/inc7-resultado-star.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:path.join(root,'.tools/inc7-resultado-movil.png'),fullPage:true});
  await page.setViewportSize({width:1365,height:1000});
  paso('P2: cuatro elementos STAR cualitativos y MCK09 adaptable a móvil',{id_respuesta:p2.id_respuesta});
  await page.getByRole('link',{name:'Siguiente pregunta',exact:true}).click();await expect(page.locator('.available-question')).toContainText('03');
  const p3=await grabar(page);await abrirResultado(page);
  await expect(page.getByText('El cierre automático y el resumen global de las 3 respuestas se activarán en el Incremento 8.',{exact:false})).toBeVisible();
  await expect(page.getByRole('link',{name:'Siguiente pregunta',exact:true})).toHaveCount(0);
  const estado=await context.request.get(base+'/api/v1/sesiones/'+sesion,{headers:{Authorization:'Bearer '+token}});
  expect(estado.status()).toBe(200);const detalle=await estado.json();
  expect(detalle.estado_sesion).toBe('EN_PROCESO');expect(detalle.puntaje_promedio).toBeNull();expect(detalle.fecha_fin).toBeNull();expect(detalle.respuestas_evaluadas).toBe(3);
  paso('P3 EVALUADO conserva EN_PROCESO, promedio y fecha_fin nulos; cierre reservado al Incremento8',{id_respuesta:p3.id_respuesta});
  await context.close();
  await configurarLlm({fallos:3,estado:503});
  const b=await nuevaCuenta(browser,'manual'),segunda=await grabar(b.page);
  await expect(b.page.getByRole('button',{name:'Reintentar análisis',exact:true})).toBeVisible({timeout:45000});
  await b.page.screenshot({path:path.join(root,'.tools/inc7-reintento-evaluar.png'),fullPage:true});
  const retry=b.page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/reintento'));
  await b.page.getByRole('button',{name:'Reintentar análisis',exact:true}).click();expect((await retry).status()).toBe(202);
  await abrirResultado(b.page);
  paso('EVALUAR 3 × HTTP503 → FALLIDO recuperable → manual202 → MCK09 sin repetir etapas previas',{id_respuesta:segunda.id_respuesta});
  const cancelaciones=()=>api.filter(p=>p.metodo==='POST'&&p.ruta==='/api/v1/sesiones/'+b.sesion+'/cancelacion').length;
  const antes=cancelaciones();
  await b.page.getByRole('button',{name:'Nueva práctica',exact:true}).click();
  expect(cancelaciones()).toBe(antes);
  await b.page.getByRole('button',{name:'Confirmar cierre para nueva práctica',exact:true}).click();
  await expect(b.page.getByRole('group',{name:'Confirmar finalización anticipada'})).toBeVisible();
  expect(cancelaciones()).toBe(antes);
  await b.page.getByRole('button',{name:'Sí, finalizar práctica',exact:true}).click();await expect(b.page).toHaveURL(/\/puestos$/);
  expect(cancelaciones()).toBe(antes+1);
  paso('Nueva práctica exige confirmar cierre, cancela sesión vigente y abre /puestos');
  await b.context.close();
  const llm=await(await fetch(control+'/__llm-audit')).json(),stt=await(await fetch(control+'/__audit')).json(),s3=await(await fetch('http://127.0.0.1:19090/__audit')).json();
  expect(stt.peticiones.length).toBe(4);expect(llm.peticiones.length).toBe(7);
  for(const p of llm.peticiones){expect(p.modelo).toBe('gpt-5.4-mini-2026-03-17');expect(p.store).toBe(false);expect(p.formato.type).toBe('json_schema');expect(p.formato.strict).toBe(true);expect(p.sin_herramientas).toBe(true);expect([...p.campos].sort()).toEqual(['version_contrato','version_rubrica','puesto','pregunta','competencia_evaluada','aplica_star','transcripcion'].sort());}
  expect(s3.peticiones.filter(p=>p.metodo==='PUT'&&p.clave.endsWith('/audio.wav')).length).toBe(4);
  expect(errores).toEqual([]);
  const result={correcto:true,fecha_utc:new Date().toISOString(),navegador:await browser.version(),total_casos:casos.length,casos,peticiones_api:api,errores_javascript:errores,
    llamadas_stt:stt.peticiones.length,llamadas_llm:llm.peticiones.length,puts_wav:4,esquema_llm:llm.peticiones[0].formato,
    entorno:'JAR y worker reales, MySQL8.4.13, ffmpeg real, HTTP S3/STT/LLM locales, SigV4 verificada y Chrome con cámara/micrófono emulados'};
  fs.writeFileSync(path.join(root,'docs/evidencias/pruebas-browser-incremento-7.json'),JSON.stringify(result,null,2)+'\n');
  console.log(JSON.stringify({correcto:true,casos:casos.length,llamadas_stt:4,llamadas_llm:7}));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

