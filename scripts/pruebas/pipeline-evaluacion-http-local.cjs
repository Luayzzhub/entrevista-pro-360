// Fixture de protocolo S3/STT local. No acredita IAM, firma AWS ni una cuenta OpenAI real.
const http=require('node:http'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'../../.tools/inc7-http-data');fs.mkdirSync(root,{recursive:true});
const objetos=new Map(),peticionesS3=[],peticionesStt=[];
let llmFallos=0,llmEstado=503,llmModo='correcto';
const peticionesLlm=[];
let fallosRestantes=0,texto='Eh este código-fuente o sea resuelve problemas',estadoFallo=503,demora=0;
function desfragmentar(raw){const partes=[];let pos=0;while(pos<raw.length){const fin=raw.indexOf('\r\n',pos),n=parseInt(raw.subarray(pos,fin).toString().split(';')[0],16);if(!Number.isFinite(n))throw Error('Framing');if(!n)break;pos=fin+2;partes.push(raw.subarray(pos,pos+n));pos+=n+2;}return Buffer.concat(partes);}
const json=(res,data,status=200)=>res.writeHead(status,{'content-type':'application/json'}).end(JSON.stringify(data));
http.createServer(async(req,res)=>{
 try{
    const url=new URL(req.url,'http://127.0.0.1:19090'),partes=[];
    for await(const chunk of req)partes.push(chunk);let body=Buffer.concat(partes);
    if(url.pathname==='/__audit'){json(res,{objetos:[...objetos].map(([clave,o])=>({clave,bytes:o.body.length,tipo:o.tipo,archivo:o.archivo})),peticiones:peticionesS3});return;}
    if(url.pathname==='/__control'&&req.method==='PUT'){const c=JSON.parse(body.toString());if(c.eliminar_clave){const o=objetos.get(c.eliminar_clave);if(o)fs.unlinkSync(o.archivo);objetos.delete(c.eliminar_clave);}json(res,{});return;}
    const prefirmada=verificarFirma(req,url);
    const firmada=String(req.headers.authorization||'').startsWith('AWS4-HMAC-SHA256')||prefirmada,clave=decodeURIComponent(url.pathname);
    peticionesS3.push({metodo:req.method,clave,tipo:req.headers['content-type'],firmada});
    const error=(status,code)=>res.writeHead(status,{'content-type':'application/xml'}).end(req.method==='HEAD'?undefined:'<Error><Code>'+code+'</Code></Error>');
    if(!firmada){error(403,'AccessDenied');return;}
    if(req.method==='PUT'){
        const wav=clave.endsWith('/audio.wav')&&req.headers['content-type']==='audio/wav';
        if(!wav&&req.headers['if-none-match']!=='*'){error(400,'InvalidRequest');return;}
        if(!wav&&objetos.has(clave)){error(412,'PreconditionFailed');return;}
        if(String(req.headers['content-encoding']||'').includes('aws-chunked'))body=desfragmentar(body);
        const archivo=path.join(root,crypto.createHash('sha256').update(clave).digest('hex')+(wav?'.wav':'.video'));
        fs.writeFileSync(archivo,body);objetos.set(clave,{body,tipo:req.headers['content-type'],sha:req.headers['x-amz-meta-sha256'],archivo});
        res.writeHead(200,{etag:'"local"'}).end();
    }else if(req.method==='HEAD'||req.method==='GET'){
        const o=objetos.get(clave);if(!o){error(404,'NoSuchKey');return;}
        const headers={'content-type':o.tipo,'content-length':o.body.length};if(o.sha)headers['x-amz-meta-sha256']=o.sha;
        headers['accept-ranges']='bytes';
        if(req.method==='GET'&&req.headers.range){const m=/^bytes=(\d+)-(\d*)$/.exec(req.headers.range);if(!m){res.writeHead(416).end();return;}const inicio=Number(m[1]),fin=m[2]?Math.min(Number(m[2]),o.body.length-1):o.body.length-1;if(inicio>fin){res.writeHead(416).end();return;}headers['content-range']='bytes '+inicio+'-'+fin+'/'+o.body.length;headers['content-length']=fin-inicio+1;res.writeHead(206,headers).end(o.body.subarray(inicio,fin+1));}
        else res.writeHead(200,headers).end(req.method==='HEAD'?undefined:o.body);
    }else if(req.method==='DELETE'){const o=objetos.get(clave);if(o)fs.unlinkSync(o.archivo);objetos.delete(clave);res.writeHead(204).end();}
    else error(405,'MethodNotAllowed');
 }catch(e){console.error(e.message);if(!res.headersSent)json(res,{error:'fixture'},500);else res.end();}
}).listen(19090,'127.0.0.1',()=>console.log('S3 HTTP local listo:19090'));
http.createServer(async(req,res)=>{
 try{
    const partes=[];for await(const chunk of req)partes.push(chunk);const body=Buffer.concat(partes);
    if(req.url==='/__llm-control'&&req.method==='PUT'){const c=JSON.parse(body.toString());llmFallos=c.fallos||0;llmEstado=c.estado||503;llmModo=c.modo||'correcto';json(res,{});return;}
    if(req.url==='/__llm-audit'){json(res,{peticiones:peticionesLlm});return;}
    if(req.url==='/v1/responses'){const p=JSON.parse(body.toString()),entrada=JSON.parse(p.input);peticionesLlm.push({modelo:p.model,store:p.store,formato:p.text.format,campos:Object.keys(entrada),aplica_star:entrada.aplica_star,sin_herramientas:!p.tools});if(llmFallos>0){llmFallos--;res.setHeader('Retry-After','0');json(res,{error:'fallo local'},llmEstado);return;}const salida=salidaEval(entrada.aplica_star);const contenido=llmModo==='refusal'?{type:'refusal',refusal:'Negativa local'}:{type:'output_text',text:llmModo==='json_invalido'?'{':JSON.stringify(salida)};json(res,{status:llmModo==='incompleto'?'incomplete':'completed',output:[{type:'message',status:'completed',content:[contenido]}]});return;}
    if(req.url==='/__control'&&req.method==='PUT'){
        const c=JSON.parse(body.toString());fallosRestantes=c.fallos||0;texto=c.texto??'Eh este código-fuente o sea resuelve problemas';estadoFallo=c.estado||503;demora=c.demora||0;json(res,{});return;
    }
    if(req.url==='/__audit'){json(res,{peticiones:peticionesStt});return;}
    peticionesStt.push({metodo:req.method,ruta:req.url,tipo:req.headers['content-type'],
        campos:[...body.toString('latin1').matchAll(/;\s*name="([^"]+)"/g)].map(m=>m[1]),
        modelo:body.includes(Buffer.from('gpt-4o-mini-transcribe-2025-12-15')),wav:body.includes(Buffer.from('RIFF')),
        sin_identificadores:!/(id_usuario|id_sesion|id_pregunta|nombre_completo|@example.test|usuarios\/)/.test(body.toString('latin1'))});
    if(req.method!=='POST'||req.url!=='/v1/audio/transcriptions'){json(res,{},404);return;}
    if(demora)await new Promise(resolve=>setTimeout(resolve,demora));
    if(fallosRestantes>0){fallosRestantes--;res.setHeader('Retry-After','0');json(res,{error:'transitorio local'},estadoFallo);}
    else json(res,{text:texto});
 }catch(e){if(!res.headersSent)json(res,{},500);else res.end();}
}).listen(19091,'127.0.0.1',()=>console.log('STT HTTP local listo:19091'));



function salidaEval(aplica){
 const criterios=Object.fromEntries(['relevancia','claridad','estructura','evidencias'].map((k,i)=>[k,{puntaje:[75,100,50,75][i],justificacion:'Justificación de '+k+' según la transcripción.'}]));
 const detalle=Object.fromEntries(['situacion','tarea','accion','resultado'].map((k,i)=>[k,{estado:['PRESENTE','PARCIAL','PRESENTE','AUSENTE'][i],observacion:'Observación de '+k+'.'}]));
 return {version_contrato:'EVAL-1.0',version_rubrica:'COM-1.0',criterios,aplica_star:aplica,detalle_star:aplica?detalle:null,fortaleza_principal:'Explicas tu aporte con claridad. <b>Texto literal</b>',areas_mejora:'Añade ejemplos verificables de tu aporte.',recomendaciones:'Describe una acción concreta y su resultado.'};
}
function verificarFirma(req,url){
 try{
  if(req.method!=='GET'||url.searchParams.get('X-Amz-Algorithm')!=='AWS4-HMAC-SHA256'||url.searchParams.get('X-Amz-Expires')!=='300'||url.searchParams.get('X-Amz-SignedHeaders')!=='host')return false;
  const fecha=url.searchParams.get('X-Amz-Date'),inicio=Date.parse(fecha.slice(0,4)+'-'+fecha.slice(4,6)+'-'+fecha.slice(6,8)+'T'+fecha.slice(9,11)+':'+fecha.slice(11,13)+':'+fecha.slice(13,15)+'Z');
  if(!Number.isFinite(inicio)||Date.now()<inicio||Date.now()>=inicio+300000)return false;
  const cred=url.searchParams.get('X-Amz-Credential').split('/');if(cred[0]!=='ep360-local-test'||cred[3]!=='s3')return false;
  const enc=s=>encodeURIComponent(s).replace(/[!'()*]/g,c=>'%'+c.charCodeAt(0).toString(16).toUpperCase());
  const canonicalQuery=[...url.searchParams].filter(([k])=>k!=='X-Amz-Signature').map(([k,v])=>enc(k)+'='+enc(v)).sort().join('&');
  const canonical='GET\n'+url.pathname+'\n'+canonicalQuery+'\nhost:'+req.headers.host+'\n\nhost\nUNSIGNED-PAYLOAD';
  const scope=cred.slice(1).join('/'),mensaje='AWS4-HMAC-SHA256\n'+fecha+'\n'+scope+'\n'+crypto.createHash('sha256').update(canonical).digest('hex');
  const hmac=(k,v)=>crypto.createHmac('sha256',k).update(v).digest();
  let k=Buffer.from('AWS4ep360-local-test-secret');for(const parte of cred.slice(1))k=hmac(k,parte);
  const firma=crypto.createHmac('sha256',k).update(mensaje).digest('hex');return firma===url.searchParams.get('X-Amz-Signature');
 }catch{return false;}
}

