import http from './http';

export function listPositions() {
  return http.get('/positions');
}

export function createPosition(payload) {
  return http.post('/positions', payload);
}

export function updatePosition(payload) {
  return http.put('/positions', payload);
}

export function deletePosition(id) {
  return http.delete(`/positions/${id}`);
}
