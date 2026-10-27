// Sirve exclusivamente el build AOT de producción y reenvía /api al JAR local.
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../frontend/dist/entrevista-pro-360/browser');
const tipos = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon' };
http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) {
    const proxy = http.request({ hostname: '127.0.0.1', port: 18080, path: req.url, method: req.method, headers: req.headers }, upstream => {
      res.writeHead(upstream.statusCode, upstream.headers); upstream.pipe(res);
    });
    proxy.on('error', () => { res.writeHead(502).end(); }); req.pipe(proxy); return;
  }
  let relativa;
  try { relativa = decodeURIComponent(new URL(req.url, 'http://localhost').pathname).replace(/^\/+/, ''); }
  catch { res.writeHead(400).end(); return; }
  const archivo = path.resolve(root, relativa);
  if (archivo !== root && !archivo.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  const destino = fs.existsSync(archivo) && fs.statSync(archivo).isFile() ? archivo : path.join(root, 'index.html');
  res.writeHead(200, { 'content-type': tipos[path.extname(destino)] || 'application/octet-stream', 'cache-control': 'no-store' });
  fs.createReadStream(destino).pipe(res);
}).listen(14200, '127.0.0.1', () => console.log('Angular AOT producción listo:14200'));
