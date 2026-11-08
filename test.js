const test = require('node:test');
const assert = require('node:assert/strict');
const { validarIncidencia } = require('./incidencias');

test('acepta una incidencia válida', () => {
  assert.equal(validarIncidencia({
    codigo: 'INC-001',
    descripcion: 'Sin conexión',
    prioridad: 'alta'
  }), true);
});

test('rechaza prioridad desconocida', () => {
  assert.equal(validarIncidencia({
    codigo: 'INC-002',
    descripcion: 'Equipo lento',
    prioridad: 'urgente'
  }), false);
});

test('rechaza descripción vacía', () => {
  assert.equal(validarIncidencia({
    codigo: 'INC-003',
    descripcion: '',
    prioridad: 'baja'
  }), false);
});
