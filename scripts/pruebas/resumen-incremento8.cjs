const {chromium,expect}=require('../../.tools/browser-check/node_modules/playwright/test');
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),base='http://127.0.0.1:14200',control='http://127.0.0.1:19091';
const chromeBin=process.env.EP360_CHROME_BIN||path.join(root,'.tools/chrome-for-testing/chrome-win64/chrome.exe');
const casos=[],errores=[],consola=[],api=[];
const paso=(nombre,datos={})=>{casos.push({nombre,correcto:true,...datos});console.log('PASS '+nombre);};
const pausa=ms=>new Promise(r=>setTimeout(r,ms));
async function configurarLlm(data){expect((await fetch(control+'/__llm-control',{method:'PUT',body:JSON.stringify(data)})).status).toBe(200);}
async function nuevaCuenta(browser,tipo){
 const context=await browser.newContext({viewport:{width:1365,height:1000}}),page=await context.newPage();
 page.on('pageerror',e=>errores.push(e.message));
 page.on('console',m=>{if(m.type()==='error')consola.push(m.text());});
 page.on('response',r=>{if(r.url().includes('/api/v1/'))api.push({metodo:r.request().method(),ruta:new URL(r.url()).pathname,estado:r.status(),ms:Date.now()});});
 const email='inc8-'+tipo+'-'+Date.now()+'@example.test',password='Evaluacion-local-2026!';
 expect((await context.request.post(base+'/api/v1/auth/registro',{data:{nombre_completo:'Prueba Ocho',email,password}})).status()).toBe(201);
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
async function abrirResultado(page,puntaje='75.00 / 100'){
 const link=page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true});
 await expect(link).toBeVisible({timeout:45000});await link.click();
 await expect(page.getByRole('heading',{name:'Resultado de tu respuesta',exact:true})).toBeVisible();
 await expect(page.locator('.session-score')).toHaveText(puntaje);
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
 const inicioLlm=(await(await fetch(control+'/__llm-audit')).json()).peticiones.length;
 const inicioStt=(await(await fetch(control+'/__audit')).json()).peticiones.length;
 const browser=await chromium.launch({executablePath:chromeBin,headless:true,args:['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
 try{
  await configurarLlm({puntajes:[75,100,50,75]});
  const a=await nuevaCuenta(browser,'resumen'),{page,context,token,sesion}=a;
  const p1=await grabar(page);await abrirResultado(page);
  await expect(page.getByText('STAR no aplica a esta pregunta',{exact:true})).toBeVisible();
  await page.getByRole('link',{name:'Siguiente pregunta',exact:true}).click();
  await expect(page.locator('.available-question')).toContainText('02');
  await configurarLlm({puntajes:[75,100,75,75]});
  const p2=await grabar(page);await abrirResultado(page,'81.25 / 100');
  const star=page.getByRole('region',{name:'Diagnóstico cualitativo STAR'});
  for(const nombre of ['Situación','Tarea','Acción','Resultado'])await expect(star).toContainText(nombre);
  await page.getByRole('link',{name:'Siguiente pregunta',exact:true}).click();
  await expect(page.locator('.available-question')).toContainText('03');
  await configurarLlm({puntajes:[75,100,75,100]});
  const p3=await grabar(page);await abrirResultado(page,'87.50 / 100');
  await expect(page.getByRole('link',{name:'Ver resumen',exact:true})).toBeVisible();
  await expect(page.getByText('se activarán en el Incremento 8',{exact:false})).toHaveCount(0);
  const getSesion=await context.request.get(base+'/api/v1/sesiones/'+sesion,{headers:{Authorization:'Bearer '+token}});
  expect(getSesion.status()).toBe(200);const completa=await getSesion.json();
  expect(completa.estado_sesion).toBe('COMPLETADA');expect(completa.puntaje_promedio).toBe(81.25);expect(completa.respuestas_evaluadas).toBe(3);
  expect(completa.pregunta_habilitada).toBeNull();expect(completa.fecha_fin).not.toBeNull();
  paso('P1 → P2 con STAR → P3: cierre automático COMPLETADA con promedio81.25 y fecha UTC',{id_sesion:sesion,respuestas:[p1.id_respuesta,p2.id_respuesta,p3.id_respuesta]});
  await page.getByRole('link',{name:'Ver resumen',exact:true}).click();
  await expect(page).toHaveURL(new RegExp('/sesiones/'+sesion+'/resumen$'));
  await expect(page.getByRole('heading',{name:'Resumen de sesión',exact:true})).toBeVisible();
  await expect(page.getByRole('heading',{name:'Promedio final',exact:true})).toBeVisible();
  await expect(page.locator('app-resumen-sesion header .session-score')).toHaveText('81.25 / 100');
  await expect(page.locator('app-resumen-sesion .question-score')).toHaveText(['75.00 / 100','81.25 / 100','87.50 / 100']);
  await expect(page.locator('app-resumen-sesion header')).toContainText('UTC');
  await expect(page.locator('app-resumen-sesion header')).toContainText('Fin:');
  await page.screenshot({path:path.join(root,'.tools/inc8-resumen-completada.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:path.join(root,'.tools/inc8-resumen-completada-movil.png'),fullPage:true});await page.setViewportSize({width:1365,height:1000});
  paso('Resumen COMPLETADA desktop/móvil: promedio final81.25, tres puntajes y fechas UTC');
  for(let i=0;i<3;i++){
    await page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true}).nth(i).click();
    await expect(page.getByRole('heading',{name:'Resultado de tu respuesta',exact:true})).toBeVisible();
    await expect(page.locator('.session-score')).toHaveText(['75.00 / 100','81.25 / 100','87.50 / 100'][i]);
    await page.getByRole('link',{name:'Ver resumen',exact:true}).click();await expect(page.getByRole('heading',{name:'Resumen de sesión',exact:true})).toBeVisible();
  }
  paso('Los tres enlaces individuales abren MCK09 y vuelven al resumen');
  await page.getByRole('link',{name:'Ver detalle',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Sesión de práctica',exact:true})).toBeVisible();
  await expect(page.locator('app-sesion .question-score')).toHaveText(['Puntaje: 75.00 / 100','Puntaje: 81.25 / 100','Puntaje: 87.50 / 100']);
  await page.getByRole('link',{name:'Ver resumen de sesión',exact:true}).click();await expect(page.getByRole('heading',{name:'Resumen de sesión',exact:true})).toBeVisible();
  paso('Detalle COMPLETADA conserva puntajes individuales y enlaza Ver resumen de sesión');
  await page.getByRole('link',{name:'Volver al panel',exact:true}).click();await expect(page).toHaveURL(/\/panel$/);
  const ultima=page.locator('article').filter({hasText:'ÚLTIMA SESIÓN COMPLETADA'});
  await expect(ultima).toContainText('Promedio: 81.25 / 100');await expect(ultima).toContainText('Sesiones completadas: 1');
  await expect(ultima.getByRole('link',{name:'Ver resumen',exact:true})).toBeVisible();await expect(ultima.getByRole('link',{name:'Ver sesión',exact:true})).toBeVisible();
  await ultima.getByRole('link',{name:'Ver resumen',exact:true}).click();await expect(page.getByRole('heading',{name:'Resumen de sesión',exact:true})).toBeVisible();
  await page.getByRole('link',{name:'Volver al panel',exact:true}).click();
  await page.screenshot({path:path.join(root,'.tools/inc8-panel-completada.png'),fullPage:true});
  paso('MCK03: última sesión COMPLETADA, promedio81.25, contador1 y enlace de resumen funcional');
  await page.getByRole('link',{name:/^Nueva práctica/}).click();
  await expect(page.locator('app-iniciar-practica button').first()).toBeEnabled();
  const nueva=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/v1/sesiones');
  await page.locator('app-iniciar-practica button').first().click();expect((await nueva).status()).toBe(201);
  await expect(page).toHaveURL(/\/sesiones\/\d+$/);const cancelada=Number(page.url().split('/').pop());expect(cancelada).not.toBe(sesion);
  paso('Después de COMPLETADA se inicia inmediatamente nueva sesión con HTTP201',{id_sesion:cancelada});
  await configurarLlm({puntajes:[75,100,50,75]});const unica=await grabar(page);await abrirResultado(page);
  await page.getByRole('button',{name:'Finalizar anticipadamente',exact:true}).click();
  const cancelar=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname==='/api/v1/sesiones/'+cancelada+'/cancelacion');
  await page.getByRole('button',{name:'Sí, finalizar práctica',exact:true}).click();expect((await cancelar).status()).toBe(200);
  await page.getByRole('link',{name:'Ver resumen',exact:true}).click();
  await expect(page).toHaveURL(new RegExp('/sesiones/'+cancelada+'/resumen$'));
  await expect(page.getByText('Incompleta: 1 de 3 respuestas evaluadas',{exact:true})).toBeVisible();
  await expect(page.getByText('La sesión fue finalizada anticipadamente por el postulante.',{exact:true})).toBeVisible();
  await expect(page.getByText('Sin respuesta registrada',{exact:true})).toHaveCount(2);
  await expect(page.getByRole('heading',{name:'Promedio final',exact:true})).toHaveCount(0);
  await expect(page.locator('app-resumen-sesion header .session-score')).toHaveCount(0);
  await expect(page.locator('app-resumen-sesion .question-score')).toHaveText(['75.00 / 100']);
  await expect(page.getByText('0.00 / 100',{exact:true})).toHaveCount(0);
  await page.screenshot({path:path.join(root,'.tools/inc8-resumen-cancelada.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:path.join(root,'.tools/inc8-resumen-cancelada-movil.png'),fullPage:true});await page.setViewportSize({width:1365,height:1000});
  paso('Resumen CANCELADA conserva1/3, motivo y dos Sin respuesta registrada; no muestra promedio ni ceros de relleno');
  await page.getByRole('link',{name:'Ver resultado de tu respuesta',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Resultado de tu respuesta',exact:true})).toBeVisible();
  await expect(page.locator('.session-score')).toHaveText('75.00 / 100');
  await page.getByRole('link',{name:'Ver resumen',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Resumen de sesión',exact:true})).toBeVisible();
  await page.getByRole('link',{name:'Ver detalle',exact:true}).click();
  await page.getByRole('link',{name:'Ver resumen de sesión',exact:true}).click();
  await expect(page.getByRole('link',{name:'Nueva práctica',exact:true})).toHaveAttribute('href','/puestos');
  paso('CANCELADA: P1 sigue consultable en MCK09, detalle y resumen enlazados y Nueva práctica disponible',{id_respuesta:unica.id_respuesta});
  expect(errores).toEqual([]);expect(consola).toEqual([]);
  const llm=await(await fetch(control+'/__llm-audit')).json(),stt=await(await fetch(control+'/__audit')).json();
  llm.peticiones=llm.peticiones.slice(inicioLlm);stt.peticiones=stt.peticiones.slice(inicioStt);
  expect(llm.peticiones.length).toBe(4);expect(stt.peticiones.length).toBe(4);
  const result={correcto:true,fecha_utc:new Date().toISOString(),navegador:await browser.version(),total_casos:casos.length,casos,
    sesion_completada:completa,sesion_cancelada:cancelada,errores_javascript:errores,errores_consola:consola,peticiones_api:api,llamadas_llm:4,llamadas_stt:4,
    entorno:'JAR y Angular reales; MySQL8.4.13, ffmpeg y HTTP S3/STT/LLM locales; Chrome con dispositivos emulados'};
  fs.writeFileSync(path.join(root,'docs/evidencias/pruebas-browser-incremento-8.json'),JSON.stringify(result,null,2)+'\n');
  console.log(JSON.stringify({correcto:true,casos:casos.length,promedio:81.25,errores_javascript:errores.length,errores_consola:consola.length}));
  await context.close();
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

