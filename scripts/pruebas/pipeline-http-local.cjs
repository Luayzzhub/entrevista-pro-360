// Fixture de protocolo S3/STT local. No acredita IAM, firma AWS ni una cuenta OpenAI real.
const http=require('node:http'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=path.resolve(__dirname,'../../.tools/inc6-http-data');fs.mkdirSync(root,{recursive:true});
const objetos=new Map(),peticionesS3=[],peticionesStt=[];
let fallosRestantes=0,texto='Eh este código-fuente o sea resuelve problemas',estadoFallo=503,demora=0;
function desfragmentar(raw){const partes=[];let pos=0;while(pos<raw.length){const fin=raw.indexOf('\r\n',pos),n=parseInt(raw.subarray(pos,fin).toString().split(';')[0],16);if(!Number.isFinite(n))throw Error('Framing');if(!n)break;pos=fin+2;partes.push(raw.subarray(pos,pos+n));pos+=n+2;}return Buffer.concat(partes);}
const json=(res,data,status=200)=>res.writeHead(status,{'content-type':'application/json'}).end(JSON.stringify(data));
http.createServer(async(req,res)=>{
 try{
    const url=new URL(req.url,'http://127.0.0.1:19090'),partes=[];
    for await(const chunk of req)partes.push(chunk);let body=Buffer.concat(partes);
    if(url.pathname==='/__audit'){json(res,{objetos:[...objetos].map(([clave,o])=>({clave,bytes:o.body.length,tipo:o.tipo,archivo:o.archivo})),peticiones:peticionesS3});return;}
    const firmada=String(req.headers.authorization||'').startsWith('AWS4-HMAC-SHA256'),clave=decodeURIComponent(url.pathname);
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
        res.writeHead(200,headers).end(req.method==='HEAD'?undefined:o.body);
    }else if(req.method==='DELETE'){const o=objetos.get(clave);if(o)fs.unlinkSync(o.archivo);objetos.delete(clave);res.writeHead(204).end();}
    else error(405,'MethodNotAllowed');
 }catch(e){console.error(e.message);if(!res.headersSent)json(res,{error:'fixture'},500);else res.end();}
}).listen(19090,'127.0.0.1',()=>console.log('S3 HTTP local listo:19090'));
http.createServer(async(req,res)=>{
 try{
    const partes=[];for await(const chunk of req)partes.push(chunk);const body=Buffer.concat(partes);
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

