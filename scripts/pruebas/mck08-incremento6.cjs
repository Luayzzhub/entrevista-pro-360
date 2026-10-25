const {chromium,expect}=require('../../.tools/browser-check/node_modules/playwright/test');
const fs=require('node:fs'),path=require('node:path'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'../..'),base='http://127.0.0.1:14200',casos=[],errores=[],peticiones=[];
const screenshot=name=>path.join(root,'.tools',name);
async function control(data){expect((await fetch('http://127.0.0.1:19091/__control',{method:'PUT',body:JSON.stringify(data)})).status).toBe(200);}
(async()=>{
 const inicioStt=(await(await fetch('http://127.0.0.1:19091/__audit')).json()).peticiones.length;
 const browser=await chromium.launch({executablePath:'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:true,
    args:['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream','--autoplay-policy=no-user-gesture-required']});
 try{
    for(const tipo of ['exito','manual','definitivo']){
        await control(tipo==='manual'?{fallos:3}:tipo==='definitivo'?{texto:'   '}:{});
        const context=await browser.newContext({viewport:{width:1365,height:1000}}),page=await context.newPage();
        page.on('pageerror',e=>errores.push(e.message));
        page.on('response',r=>{if(r.url().includes('/api/v1/'))peticiones.push({metodo:r.request().method(),ruta:new URL(r.url()).pathname,estado:r.status(),ms:Date.now(),caso:tipo});});
        const email='inc6-'+tipo+'-'+Date.now()+'@example.test',password='Pipeline-local-2026!';
        expect((await context.request.post(base+'/api/v1/auth/registro',{data:{nombre_completo:'Prueba Seis',email,password}})).status()).toBe(201);
        await page.goto(base+'/login');await page.locator('#login-email').fill(email);await page.locator('#login-password').fill(password);
        await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page).toHaveURL(/\/panel$/);
        await page.getByRole('link',{name:/^Nueva práctica/}).click();await expect(page.locator('app-iniciar-practica button').first()).toBeEnabled();
        await page.locator('app-iniciar-practica button').first().click();await expect(page).toHaveURL(/\/sesiones\/\d+$/);
        const sesion=Number(page.url().split('/').pop());
        await page.getByRole('button',{name:'Comprobar cámara y micrófono',exact:true}).click();
        await expect(page.getByRole('button',{name:'Iniciar grabación',exact:true})).toBeEnabled();await page.getByRole('button',{name:'Iniciar grabación',exact:true}).click();
        await expect(page.getByRole('timer')).toHaveText('00:02 / 01:00',{timeout:5000});
        await page.getByRole('button',{name:'Detener grabación',exact:true}).click();await expect(page.locator('video.review-video')).toBeVisible();
        const upload=page.waitForResponse(r=>r.request().method()==='PUT'&&/\/preguntas\/\d+\/respuesta$/.test(r.url()));
        await page.getByRole('button',{name:'Confirmar respuesta',exact:true}).click();const res=await upload;expect(res.status()).toBe(202);
        const confirmacion=await res.json(),ruta='/api/v1/respuestas/'+confirmacion.id_respuesta;
        await expect(page.getByRole('heading',{name:'Procesamiento de respuesta',exact:true})).toBeVisible();
        if(tipo==='manual'){
            await expect(page.getByRole('button',{name:'Reintentar análisis',exact:true})).toBeVisible({timeout:30000});
            await expect(page.getByText('Se agotaron los intentos automáticos.',{exact:false})).toBeVisible();
            await page.screenshot({path:screenshot('inc6-manual-disponible.png'),fullPage:true});
            const retry=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith(ruta+'/reintento'));
            await page.getByRole('button',{name:'Reintentar análisis',exact:true}).click();expect((await retry).status()).toBe(202);
        }
        if(tipo==='definitivo'){
            await expect(page.getByText('La sesión pasó a CANCELADA conforme a D-02.',{exact:false})).toBeVisible({timeout:30000});
            await expect(page.getByRole('button',{name:'Reintentar análisis',exact:true})).toHaveCount(0);
            await expect(page.getByRole('link',{name:'Volver a la sesión',exact:true})).toBeVisible();
            await expect(page.getByRole('link',{name:'Volver al panel',exact:true})).toBeVisible();
        }else{
            await expect(page.getByText('Audio extraído, transcripción STT en español y métricas MET-1.0 registradas.',{exact:false})).toBeVisible({timeout:30000});
            await expect(page.locator('dt').filter({hasText:'Palabras por minuto'})).toBeVisible();
        }
        if(tipo==='manual'||tipo==='definitivo'){
            const login=await context.request.post(base+'/api/v1/auth/login',{data:{email,password}});
            expect(login.status()).toBe(200);const token=(await login.json()).access_token;
            const destino=tipo==='manual'?confirmacion.id_respuesta:casos[0].id_respuesta;
            const respuesta=await context.request.post(base+'/api/v1/respuestas/'+destino+'/reintento',{headers:{Authorization:'Bearer '+token}});
            expect(respuesta.status()).toBe(tipo==='manual'?409:404);
            expect((await respuesta.json()).codigo).toBe(tipo==='manual'?'REINTENTO_NO_DISPONIBLE':'RESPUESTA_NO_ENCONTRADA');
        }
        const consultas=()=>peticiones.filter(p=>p.caso===tipo&&p.ruta===ruta&&p.metodo==='GET').length;
        const antes=consultas();await page.waitForTimeout(3700);expect(consultas()).toBe(antes);
        await page.screenshot({path:screenshot('inc6-'+tipo+'.png'),fullPage:true});
        await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
        await page.screenshot({path:screenshot('inc6-'+tipo+'-movil.png'),fullPage:true});
        casos.push({caso:tipo,correcto:true,id_sesion:sesion,id_respuesta:confirmacion.id_respuesta,polling_detenido:true});
        console.log('PASS MCK08 '+tipo);await context.close();
    }
    const s3=await(await fetch('http://127.0.0.1:19090/__audit')).json(),stt=await(await fetch('http://127.0.0.1:19091/__audit')).json();
    stt.peticiones=stt.peticiones.slice(inicioStt);
    const wavs=s3.objetos.filter(o=>o.tipo==='audio/wav'&&o.clave.startsWith('/ep360-pruebas-local/')&&casos.some(c=>o.clave.includes('/sesiones/'+c.id_sesion+'/')));expect(wavs.length).toBe(3);
    const formatos=wavs.map(o=>JSON.parse(execFileSync(path.join(root,'.tools/ffmpeg/ffprobe.exe'),['-v','error','-show_entries','stream=codec_name,sample_rate,channels','-of','json',o.archivo],{encoding:'utf8'})).streams[0]);
    for(const f of formatos){expect(f.codec_name).toBe('pcm_s16le');expect(f.sample_rate).toBe('16000');expect(f.channels).toBe(1);}
    for(const r of stt.peticiones){expect(r.campos).toEqual(['file','model','language','response_format']);expect(r.modelo&&r.wav&&r.sin_identificadores).toBe(true);}
    expect(stt.peticiones.length).toBe(6);expect(errores).toEqual([]);
    const resultado={fecha:new Date().toISOString(),correcto:true,total_casos:casos.length,navegador:await browser.version(),casos,formatos_wav:formatos,stt:stt.peticiones,peticiones_api:peticiones,errores_javascript:errores,
        entorno:'JAR Spring + MySQL 8.4.13 + worker programado + ffmpeg/ffprobe reales + S3 y STT HTTP locales + Chrome con dispositivos emulados'};
    fs.writeFileSync(path.join(root,'docs/evidencias/pruebas-browser-incremento-6.json'),JSON.stringify(resultado,null,2)+'\n');
    console.log(JSON.stringify({correcto:true,casos:casos.length,llamadas_stt:stt.peticiones.length}));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

