import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

// Ejecuta las reglas puras TypeScript con el compilador ya instalado; no requiere navegador ni otra dependencia.
const source = readFileSync(new URL('../src/app/features/interviews/formatos-media.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } });
const { seleccionarFormato, validarTomaLocal } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);
const vp8 = 'video/webm;codecs=vp8,opus';
const vp9 = 'video/webm;codecs=vp9,opus';
const mp4 = 'video/mp4;codecs=avc1.42E01E,mp4a.40.2';

test('negociación prefiere VP8 y no consulta formatos posteriores si ya es compatible', () => {
  const consultados = [];
  const formato = seleccionarFormato(mime => { consultados.push(mime); return true; });
  assert.equal(formato.mimeType, vp8); assert.equal(formato.contenedor, 'webm'); assert.deepEqual(consultados, [vp8]);
});
test('negociación recurre a VP9 y después a MP4 en el orden contractual', () => {
  for (const permitido of [vp9, mp4]) {
    const consultados = [];
    const formato = seleccionarFormato(mime => { consultados.push(mime); return mime === permitido; });
    assert.equal(formato.mimeType, permitido);
    assert.equal(formato.contenedor, permitido === mp4 ? 'mp4' : 'webm');
    assert.deepEqual(consultados, permitido === vp9 ? [vp8, vp9] : [vp8, vp9, mp4]);
  }
});
test('ningún formato admitido devuelve null y nunca elige un tipo indeterminado', () => {
  const consultados = [];
  assert.equal(seleccionarFormato(mime => { consultados.push(mime); return false; }), null);
  assert.deepEqual(consultados, [vp8, vp9, mp4]);
});
test('límites inclusivos de duración: 1 y 60 s válidos, fuera de ellos inválidos', () => {
  for (const duracion of [1, 15.432, 60]) assert.deepEqual(validarTomaLocal(1024, duracion), []);
  for (const duracion of [0, 0.999, -1, 60.001, NaN, Infinity]) assert.notEqual(validarTomaLocal(1024, duracion).length, 0);
});
test('tamaño entero positivo estrictamente inferior a 50 000 000 bytes', () => {
  for (const bytes of [1, 49_999_999]) assert.deepEqual(validarTomaLocal(bytes, 2), []);
  for (const bytes of [0, -1, 0.5, 50_000_000, 50_000_001, NaN, Infinity]) assert.notEqual(validarTomaLocal(bytes, 2).length, 0);
});
test('una toma inválida informa problemas de tamaño y duración sin ocultar uno de ellos', () => {
  const errores = validarTomaLocal(50_000_000, 0.1);
  assert.equal(errores.length, 2); assert.match(errores[0], /menos de 50 MB/); assert.match(errores[1], /al menos 1 segundo/);
});
